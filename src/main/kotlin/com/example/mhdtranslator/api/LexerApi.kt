package com.example.mhdtranslator.api

import com.example.mhdtranslator.model.ArticleSpan
import com.example.mhdtranslator.model.LexerEntry
import com.example.mhdtranslator.model.SearchResult
import com.example.mhdtranslator.model.TypesetStyle
import com.example.mhdtranslator.model.Suggestion
import org.json.JSONArray
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL
import java.net.URLEncoder

private const val BASE_OPEN = "https://api.woerterbuchnetz.de/open-api/dictionaries"
private const val BASE      = "$BASE_OPEN/Lexer"
private const val BASE_SITE = "https://api.woerterbuchnetz.de/dictionaries/Lexer"

/**
 * Lexer's linkGroup from wbProperties — dictionaries cross-linked to Lexer.
 * These are queried in parallel for definition/fulltext mixed results,
 * matching what the Wörterbuchnetz website shows in its middle column.
 */
private val MHD_DICT_GROUP = listOf(
    "Lexer", "BMZ", "LexerN", "FindeB", "MWB",
    "AWB", "DWB", "DWB2", "FWB", "DRW", "MHDBDB",
)

class LexerApi {

    // ─── Public entry point ───────────────────────────────────────────────────

    suspend fun fetchSuggestions(prefix: String): List<Suggestion> = withContext(Dispatchers.IO) {
        if (prefix.isBlank()) return@withContext emptyList()
        try {
            val encoded = URLEncoder.encode(prefix.trim(), "UTF-8")
            // Matches the typeahead endpoint used by the website:
            // /dictionaries/Lexer/lemmata/lemma/{prefix}*/100/json?term={prefix}&_type=query&q={prefix}
            val url = "$BASE_SITE/lemmata/lemma/$encoded*/100/json?term=$encoded&_type=query&q=$encoded"
            val array = fetchJsonArray(url)
            buildList {
                for (i in 0 until array.length()) {
                    val obj = array.getJSONObject(i)
                    add(Suggestion(
                        label = decodeEntities(obj.optString("label")),
                        gram  = obj.optString("gram"),
                    ))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    suspend fun search(query: String): SearchResult = withContext(Dispatchers.IO) {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")

        coroutineScope {
            val lemmataDeferred    = async { fetchLemmata(encoded) }
            val definitionDeferred = async { fetchDefinitions(encoded) }
            val fulltextDeferred   = async { fetchArticles(encoded) }
            val prefixDeferred     = async { fetchLemmataPrefix(encoded) }

            SearchResult(
                query             = query.trim(),
                lemmataResults    = lemmataDeferred.await(),
                definitionResults = definitionDeferred.await(),
                fulltextResults   = fulltextDeferred.await(),
                articleResults    = prefixDeferred.await(),
            )
        }
    }

    // ─── Lemmata (MHD headword search) ────────────────────────────────────────

    private fun fetchLemmata(encodedQuery: String): List<LexerEntry> {
        return try {
            val json = fetchJson("$BASE/lemmata/$encodedQuery")
            val results = json.optJSONArray("result_set") ?: return emptyList()
            buildList {
                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    add(
                        LexerEntry(
                            lemma      = decodeEntities(obj.optString("lemma")),
                            gram       = obj.optString("gram").takeIf { it.isNotBlank() },
                            wbnetzId   = obj.optString("wbnetzid"),
                            kwicText   = null,
                            wbnetzLink = obj.optString("wbnetzlink"),
                        )
                    )
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─── Definitions (German→MHD search, mixed across link-group dicts) ─────────

    private suspend fun fetchDefinitions(encodedQuery: String): List<LexerEntry> =
        coroutineScope {
            MHD_DICT_GROUP.map { sigle ->
                async { fetchDefinitionsFor(sigle, encodedQuery) }
            }.awaitAll().flatten()
        }

    private suspend fun fetchDefinitionsFor(sigle: String, encodedQuery: String): List<LexerEntry> {
        return try {
            val json    = fetchJson("$BASE_OPEN/$sigle/definition/$encodedQuery")
            val results = json.optJSONArray("result_set") ?: return emptyList()

            data class RawEntry(val entry: LexerEntry, val kwicUrl: String?)
            val rawEntries = buildList {
                for (i in 0 until results.length()) {
                    val obj = results.getJSONObject(i)
                    add(RawEntry(
                        entry = LexerEntry(
                            lemma      = decodeEntities(obj.optString("lemma")),
                            gram       = obj.optString("gram").takeIf { it.isNotBlank() },
                            wbnetzId   = obj.optString("wbnetzid"),
                            kwicText   = null,
                            wbnetzLink = obj.optString("wbnetzlink"),
                            sigle      = sigle,
                        ),
                        kwicUrl = obj.optString("wbnetzkwiclink").takeIf { it.isNotBlank() }
                    ))
                }
            }

            // Fetch KWiC for up to 5 results per dictionary
            coroutineScope {
                rawEntries.take(5).map { raw ->
                    async {
                        val kwic = raw.kwicUrl?.let { fetchKwicText(it) }
                        raw.entry.copy(kwicText = kwic)
                    }
                }.awaitAll()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─── Article text (Wörterbuchartikel — article content for prefix lemmata) ──

    private suspend fun fetchArticles(encodedQuery: String): List<LexerEntry> =
        coroutineScope {
            val prefixLemmata = fetchLemmataPrefix(encodedQuery)

            // Deduplicate by wbnetzId, preserving order
            val seen   = mutableSetOf<String>()
            val unique = prefixLemmata.filter { seen.add(it.wbnetzId) }

            // Fetch full article content for each unique entry in parallel
            unique.take(15).map { entry ->
                async {
                    val spans = fetchArticleSpans(entry.wbnetzId)
                    val plainText = spans?.joinToString("") { it.text }
                        ?.replace(Regex(" {2,}"), " ")?.trim()
                        ?.takeIf { it.isNotEmpty() }
                    entry.copy(kwicText = plainText, articleSpans = spans)
                }
            }.awaitAll()
        }

    /**
     * Fetches the full structured article for a lemid and returns a list of styled spans.
     * Uses the `typeset` field for formatting (italics, caps) and a simple entity decoder
     * instead of Html.fromHtml (which mangles XML tag fragments in the data).
     *
     * Broken XML tag fragments (`<hi rend="...">`) split across entries are detected
     * and skipped — we rely on `typeset` for styling instead.
     */
    private fun fetchArticleSpans(wbnetzId: String): List<ArticleSpan>? {
        return try {
            val array = fetchJsonArray("$BASE_SITE/articles/$wbnetzId/lemid")
            if (array.length() == 0) return null

            val skipSilent = setOf(
                "idlinkend", "linkend",
                "startdefinition", "enddefinition",
                "qvzidlinkend", "bmzref",
            )

            val spans = mutableListOf<ArticleSpan>()
            // Track XML tag fragment state: 0 = normal, 1 = inside <hi ... > opening,
            // 2 = inside tag content until ">
            var xmlSkip = 0
            // Track active link URL (between *linkstart and *linkend)
            var activeLinkUrl: String? = null

            for (i in 0 until array.length()) {
                val obj     = array.getJSONObject(i)
                val type    = obj.optString("elementtype")
                val typeset = obj.optString("typeset")

                // Link region tracking
                if (type == "qvzidlinkstart") {
                    val sigle   = obj.optString("word")    // e.g. "MWVQVZ"
                    val normword = obj.optString("normword") // e.g. "QJ0003"
                    activeLinkUrl = "https://www.woerterbuchnetz.de/$sigle?lemid=$normword"
                    continue
                }
                if (type == "idlinkstart" || type == "bmzidlinkstart") {
                    val sigle   = obj.optString("word")    // e.g. "FindeB", "BMZ"
                    val normword = obj.optString("normword") // e.g. "S02229"
                    activeLinkUrl = "https://www.woerterbuchnetz.de/$sigle?lemid=$normword"
                    continue
                }
                if (type == "qvzidlinkend" || type == "idlinkend" || type == "linkend") {
                    activeLinkUrl = null
                    continue
                }
                if (type in skipSilent) continue
                if (type == "sensemark") {
                    spans.add(ArticleSpan("\n"))
                    continue
                }

                val raw = obj.optString("word")
                if (raw.isEmpty()) continue
                val decoded = decodeEntities(raw)

                // Detect and handle broken XML tag fragments: <hi rend= >content">
                when (xmlSkip) {
                    0 -> {
                        if (decoded.trimStart().startsWith("<hi")) {
                            xmlSkip = 1
                            continue
                        }
                    }
                    1 -> {
                        // Inside opening tag attributes — look for '>'
                        val gt = decoded.indexOf('>')
                        if (gt >= 0) {
                            xmlSkip = 2
                            val after = decoded.substring(gt + 1)
                            if (after.contains("\">")) {
                                val content = after.substringBefore("\">")
                                if (content.isNotBlank()) {
                                    spans.add(ArticleSpan(content, typesetToStyle(typeset), activeLinkUrl))
                                }
                                xmlSkip = 0
                            } else if (after.isNotEmpty()) {
                                spans.add(ArticleSpan(after, typesetToStyle(typeset), activeLinkUrl))
                            }
                        }
                        continue
                    }
                    2 -> {
                        if (decoded.contains("\">")) {
                            val content = decoded.substringBefore("\">")
                            if (content.isNotEmpty()) {
                                spans.add(ArticleSpan(content, typesetToStyle(typeset), activeLinkUrl))
                            }
                            xmlSkip = 0
                        } else {
                            spans.add(ArticleSpan(decoded, typesetToStyle(typeset), activeLinkUrl))
                        }
                        continue
                    }
                }

                spans.add(ArticleSpan(decoded, typesetToStyle(typeset), activeLinkUrl))
            }

            spans.takeIf { it.isNotEmpty() }
        } catch (e: Exception) {
            null
        }
    }

    private fun typesetToStyle(typeset: String): TypesetStyle = when (typeset) {
        "italics" -> TypesetStyle.ITALIC
        "caps"    -> TypesetStyle.CAPS
        else      -> TypesetStyle.NORMAL
    }

    /** Decodes HTML/XML entities without Html.fromHtml (which strips whitespace and mangles tags). */
    private fun decodeEntities(text: String): String {
        if (!text.contains('&')) return text
        return text
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&apos;", "'")
            .replace(Regex("&#x([0-9a-fA-F]+);")) {
                String(Character.toChars(it.groupValues[1].toInt(16)))
            }
            .replace(Regex("&#(\\d+);")) {
                String(Character.toChars(it.groupValues[1].toInt()))
            }
    }

    // ─── Prefix index (headwords starting with query, mirrors website left panel) ─

    private fun fetchLemmataPrefix(encodedQuery: String): List<LexerEntry> {
        return try {
            val url = "$BASE_SITE/lemmata/lemma/$encodedQuery*/50/json" +
                    "?term=$encodedQuery&_type=query&q=$encodedQuery"
            val array = fetchJsonArray(url)
            buildList {
                for (i in 0 until array.length()) {
                    val obj   = array.getJSONObject(i)
                    val value = obj.optString("value")
                    add(LexerEntry(
                        lemma      = decodeEntities(obj.optString("label")),
                        gram       = obj.optString("gram").takeIf { it.isNotBlank() },
                        wbnetzId   = value,
                        kwicText   = null,
                        wbnetzLink = "https://www.woerterbuchnetz.de/Lexer?lemid=$value",
                    ))
                }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ─── KWiC snippet ────────────────────────────────────────────────────────

    private fun fetchKwicText(kwicUrl: String): String? {
        return try {
            val json = fetchJson(kwicUrl)
            json.optString("kwic_text").takeIf { it.isNotBlank() }?.let { decodeEntities(it) }
        } catch (e: Exception) {
            null
        }
    }

    // ─── HTTP helpers ─────────────────────────────────────────────────────────

    private fun fetchJson(urlString: String): JSONObject {
        return JSONObject(fetchText(urlString))
    }

    private fun fetchJsonArray(urlString: String): JSONArray {
        return JSONArray(fetchText(urlString))
    }

    private fun fetchText(urlString: String): String {
        val connection = URL(urlString).openConnection()
        connection.setRequestProperty("Accept", "application/json")
        connection.setRequestProperty("User-Agent", "MHD-Translator-App/1.0")
        connection.connectTimeout = 10_000
        connection.readTimeout    = 10_000
        return connection.getInputStream().bufferedReader().readText()
    }

}
