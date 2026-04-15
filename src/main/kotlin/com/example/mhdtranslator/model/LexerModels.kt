package com.example.mhdtranslator.model

enum class TypesetStyle { NORMAL, ITALIC, CAPS }

data class ArticleSpan(
    val text: String,
    val style: TypesetStyle = TypesetStyle.NORMAL,
    val linkUrl: String? = null,
)

/**
 * A single result entry from the Lexer API.
 * Used for both lemmata (MHD→German) and definition (German→MHD) searches.
 */
data class LexerEntry(
    val lemma: String,         // MHD headword (HTML-decoded, e.g. "hûs")
    val gram: String?,         // Grammatical category (e.g. "stn.", "swm.", "adj.")
    val wbnetzId: String,      // Internal ID (e.g. "H04072")
    val kwicText: String?,     // Definition excerpt, only available for definition-search results
    val wbnetzLink: String,    // Full article URL on woerterbuchnetz.de
    val sigle: String = "",    // Source dictionary (e.g. "BMZ"); empty = don't show badge
    val articleSpans: List<ArticleSpan>? = null,  // Rich article content with formatting
)

/**
 * A single autocomplete suggestion returned by the typeahead endpoint.
 */
data class Suggestion(
    val label: String,  // MHD lemma to display and search with
    val gram: String,   // Grammatical category (may be empty)
)

/**
 * Combined result of a Lexer search, containing results from both search strategies.
 */
data class SearchResult(
    val query: String,
    val lemmataResults: List<LexerEntry>,    // Direct MHD headword matches
    val definitionResults: List<LexerEntry>, // MHD entries whose German definition contains the query
    val fulltextResults: List<LexerEntry>,   // All article text occurrences of the query (with KWiC context)
    val articleResults: List<LexerEntry>,     // All headwords starting with the query (prefix index)
)
