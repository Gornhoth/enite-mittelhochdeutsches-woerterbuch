package com.example.mhdtranslator.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.ComponentActivity
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import kotlinx.coroutines.delay
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.mhdtranslator.model.ArticleSpan
import com.example.mhdtranslator.model.LexerEntry
import com.example.mhdtranslator.model.Suggestion
import androidx.core.net.toUri

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val vm: MHDTranslatorViewModel = viewModel()
            val themeMode by vm.themeMode.collectAsState()
            AppTheme(themeMode = themeMode) {
                MHDTranslatorScreen(vm = vm)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MHDTranslatorScreen(vm: MHDTranslatorViewModel = viewModel()) {
    val query       by vm.query.collectAsState()
    val state       by vm.state.collectAsState()
    val suggestions by vm.suggestions.collectAsState()
    val canGoBack   by vm.canGoBack.collectAsState()

    val lemmataExpanded     by vm.lemmataExpanded.collectAsState()
    val definitionsExpanded by vm.definitionsExpanded.collectAsState()
    val fulltextExpanded    by vm.fulltextExpanded.collectAsState()
    val articleExpanded     by vm.articleExpanded.collectAsState()
    val themeMode           by vm.themeMode.collectAsState()

    val context = LocalContext.current
    var themeMenuOpen  by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var showExitHint   by remember { mutableStateOf(false) }
    var lastBackPress  by remember { mutableLongStateOf(0L) }
    val activity = LocalActivity.current as? ComponentActivity

    // Auto-dismiss the exit hint after 2 s
    LaunchedEffect(showExitHint) {
        if (showExitHint) {
            delay(2_000)
            showExitHint = false
        }
    }

    BackHandler {
        val now = System.currentTimeMillis()
        if (now - lastBackPress < 600L) {
            // Fast double-back: exit from anywhere
            activity?.finish()
        } else {
            lastBackPress = now
            if (canGoBack) {
                vm.navigateBack()
            } else {
                // At root: hint the user that a fast second back will exit
                showExitHint = true
            }
        }
    }

    if (showAboutDialog) {
        AboutDialog(onDismiss = { showAboutDialog = false })
    }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                expandedHeight = 64.dp,
                title = {
                    Column {
                        Text("Enite - Mittelhochdeutsches Wörterbuch", style = MaterialTheme.typography.titleMedium)
                        Text("Lexer · Wörterbuchnetz", style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { themeMenuOpen = true }) {
                            Icon(Icons.Default.Settings, contentDescription = "Einstellungen")
                        }
                        DropdownMenu(
                            expanded = themeMenuOpen,
                            onDismissRequest = { themeMenuOpen = false },
                        ) {
                            listOf(
                                ThemeMode.SYSTEM to "System",
                                ThemeMode.LIGHT  to "Hell",
                                ThemeMode.DARK   to "Dunkel",
                            ).forEach { (mode, label) ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            label,
                                            fontWeight = if (themeMode == mode) FontWeight.Bold else FontWeight.Normal,
                                            color = if (themeMode == mode) MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.onSurface,
                                        )
                                    },
                                    onClick = { vm.setThemeMode(mode); themeMenuOpen = false },
                                )
                            }
                            HorizontalDivider()
                            DropdownMenuItem(
                                text = { Text("Über die App") },
                                onClick = { themeMenuOpen = false; showAboutDialog = true },
                            )
                            DropdownMenuItem(
                                text = { Text("☕ Spenden") },
                                onClick = {
                                    themeMenuOpen = false
                                    context.startActivity(Intent(Intent.ACTION_VIEW, "https://ko-fi.com/johannesschatteiner".toUri()))
                                },
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(Modifier.height(12.dp))
            SearchBar(
                query = query,
                suggestions = suggestions,
                onQueryChange = vm::onQueryChange,
                onSearch = vm::search,
                onSuggestionSelected = vm::selectSuggestion,
            )
            Spacer(Modifier.height(8.dp))

            when (val s = state) {
                is SearchState.Idle    -> HintText("Deutsches oder mittelhochdeutsches Wort eingeben")
                is SearchState.Loading -> LoadingSpinner()
                is SearchState.Error   -> ErrorText(s.message)
                is SearchState.Success -> ResultList(
                    query               = s.result.query,
                    lemmata             = s.result.lemmataResults,
                    definitions         = s.result.definitionResults,
                    fulltext            = s.result.fulltextResults,
                    articles            = s.result.articleResults,
                    lemmataExpanded     = lemmataExpanded,
                    definitionsExpanded = definitionsExpanded,
                    fulltextExpanded    = fulltextExpanded,
                    articleExpanded     = articleExpanded,
                    onToggleLemmata     = { vm.setLemmataExpanded(!lemmataExpanded) },
                    onToggleDefinitions = { vm.setDefinitionsExpanded(!definitionsExpanded) },
                    onToggleFulltext    = { vm.setFulltextExpanded(!fulltextExpanded) },
                    onToggleArticle     = { vm.setArticleExpanded(!articleExpanded) },
                    onArticleEntryClick = vm::navigateTo,
                )
            }
        }
    }

    // Exit hint — shown above the app bar on double-back prompt
    if (showExitHint) {
        Surface(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 72.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth(),
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.inverseSurface,
            shadowElevation = 4.dp,
        ) {
            Text(
                text = "Erneut zur\u00FCck dr\u00FCcken zum Beenden",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                color = MaterialTheme.colorScheme.inverseOnSurface,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
    } // end Box
}

// ─── Search bar ───────────────────────────────────────────────────────────────

@Composable
fun SearchBar(
    query: String,
    suggestions: List<Suggestion>,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    onSuggestionSelected: (String) -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    var fieldValue by remember { mutableStateOf(TextFieldValue(query)) }

    // When query changes externally (e.g. suggestion selected), move caret to end
    LaunchedEffect(query) {
        if (fieldValue.text != query) {
            fieldValue = TextFieldValue(text = query, selection = TextRange(query.length))
        }
    }

    Column(Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = fieldValue,
            onValueChange = { new ->
                fieldValue = new
                onQueryChange(new.text)
            },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("Suchen…") },
            leadingIcon  = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (fieldValue.text.isNotEmpty()) {
                    IconButton(onClick = { onQueryChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Löschen")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.None,
                imeAction = ImeAction.Search
            ),
            keyboardActions = KeyboardActions(onSearch = {
                onSearch(fieldValue.text)
                keyboard?.hide()
            })
        )
        if (suggestions.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 240.dp),
                shadowElevation = 4.dp,
                border = BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    suggestions.forEachIndexed { index, suggestion ->
                        if (index > 0) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = MaterialTheme.colorScheme.outlineVariant
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSuggestionSelected(suggestion.label)
                                    keyboard?.hide()
                                }
                                .padding(horizontal = 16.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(suggestion.label, style = MaterialTheme.typography.bodyMedium)
                            if (suggestion.gram.isNotBlank()) {
                                Text(
                                    text = "  ${suggestion.gram}",
                                    style = MaterialTheme.typography.bodySmall,
                                    fontStyle = FontStyle.Italic,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ─── Result list ─────────────────────────────────────────────────────────────

@Composable
fun ResultList(
    query: String,
    lemmata: List<LexerEntry>,
    definitions: List<LexerEntry>,
    fulltext: List<LexerEntry>,
    articles: List<LexerEntry>,
    lemmataExpanded: Boolean,
    definitionsExpanded: Boolean,
    fulltextExpanded: Boolean,
    articleExpanded: Boolean,
    onToggleLemmata: () -> Unit,
    onToggleDefinitions: () -> Unit,
    onToggleFulltext: () -> Unit,
    onToggleArticle: () -> Unit,
    onArticleEntryClick: (String) -> Unit = {},
) {
    LazyColumn(
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp)
    ) {
        if (lemmata.isNotEmpty()) {
            item {
                SectionHeader(
                    label    = "Stichwort",
                    subtitle = "MHD Lemmata f\u00FCr \u201E$query\u201C",
                    expanded = lemmataExpanded,
                    onToggle = onToggleLemmata,
                )
            }
            if (lemmataExpanded) {
                itemsIndexed(lemmata, key = { i, it -> "L$i${it.wbnetzId}" }) { _, entry ->
                    EntryRow(entry, showKwic = false)
                    HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        if (articles.isNotEmpty()) {
            item {
                SectionHeader(
                    label    = "Eintr\u00E4ge",
                    subtitle = "Lemmata beginnend mit \u201E$query\u201C",
                    expanded = articleExpanded,
                    onToggle = onToggleArticle,
                )
            }
            if (articleExpanded) {
                item(key = "articles_scroll") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 240.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        articles.forEachIndexed { index, entry ->
                            ArticleListRow(entry, onEntryClick = onArticleEntryClick)
                            if (index < articles.lastIndex) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                )
                            }
                        }
                    }
                }
            }
        }

        if (definitions.isNotEmpty()) {
            item {
                SectionHeader(
                    label    = "Bedeutungsfeld",
                    subtitle = "MHD W\u00F6rter mit \u201E$query\u201C in der Definition",
                    expanded = definitionsExpanded,
                    onToggle = onToggleDefinitions,
                )
            }
            if (definitionsExpanded) {
                itemsIndexed(definitions, key = { i, it -> "D$i${it.wbnetzId}" }) { _, entry ->
                    EntryRow(entry, showKwic = true)
                    HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }

        if (fulltext.isNotEmpty()) {
            item {
                SectionHeader(
                    label    = "W\u00F6rterbuchartikel",
                    subtitle = "Lexer-Artikel zu \u201E$query\u201C*",
                    expanded = fulltextExpanded,
                    onToggle = onToggleFulltext,
                )
            }
            if (fulltextExpanded) {
                itemsIndexed(fulltext, key = { i, it -> "F$i${it.wbnetzId}" }) { _, entry ->
                    ArticleEntryRow(entry)
                    HorizontalDivider(Modifier, thickness = 0.5.dp, color = MaterialTheme.colorScheme.outlineVariant)
                }
            }
        }
    }
}

@Composable
fun SectionHeader(label: String, subtitle: String, expanded: Boolean, onToggle: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .padding(top = 16.dp, bottom = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp),
            )
        }
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(6.dp))
        HorizontalDivider(Modifier, thickness = 1.dp, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
fun EntryRow(entry: LexerEntry, showKwic: Boolean) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                val intent = Intent(Intent.ACTION_VIEW, entry.wbnetzLink.toUri())
                context.startActivity(intent)
            }
            .padding(horizontal = 4.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = entry.lemma,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                if (entry.gram != null) {
                    Text(
                        text = "  ${entry.gram}",
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (entry.sigle.isNotBlank()) {
                    Text(
                        text = "  ${entry.sigle}",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            if (showKwic && entry.kwicText != null) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = entry.kwicText,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3
                )
            }
        }
    }
}

@Composable
fun ArticleEntryRow(entry: LexerEntry) {
    val context  = LocalContext.current
    var expanded by remember(entry.wbnetzId) { mutableStateOf(false) }
    // Assume expandable for any entry with article text; corrected downward by onTextLayout
    // if the text turns out to be short (< 3 lines). This avoids relying on hasVisualOverflow
    // or hidden measurement passes, both of which are unreliable with TextOverflow.Ellipsis.
    var isLong   by remember(entry.wbnetzId) { mutableStateOf(entry.kwicText != null) }

    val linkColor = MaterialTheme.colorScheme.primary
    val bodyStyle = MaterialTheme.typography.bodyMedium.copy(
        color = MaterialTheme.colorScheme.onSurface,
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 10.dp)
    ) {
        val displayText = entry.kwicText ?: entry.lemma
        val spans = entry.articleSpans
        val annotated = remember(displayText, spans, linkColor) {
            if (spans != null) {
                buildArticleAnnotatedString(spans, linkColor)
            } else {
                buildArticleAnnotatedStringPlain(displayText)
            }
        }

        Text(
            text     = annotated,
            style    = bodyStyle,
            maxLines = if (expanded) Int.MAX_VALUE else 3,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { result ->
                // lineCount is capped at maxLines (3) when text is truncated, so < 3 reliably
                // means the text fits completely — no expand button needed.
                if (!expanded && result.lineCount < 3) isLong = false
            },
        )

        if (entry.kwicText != null) {
            Spacer(Modifier.height(2.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .clickable {
                            context.startActivity(Intent(Intent.ACTION_VIEW, entry.wbnetzLink.toUri()))
                        },
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text      = entry.lemma,
                        style     = MaterialTheme.typography.labelSmall,
                        fontStyle = FontStyle.Italic,
                        color     = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (entry.gram != null) {
                        Text(
                            text      = "  ${entry.gram}",
                            style     = MaterialTheme.typography.labelSmall,
                            fontStyle = FontStyle.Italic,
                            color     = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (isLong || expanded) {
                    Text(
                        text     = if (expanded) "\u25b2 weniger" else "\u25bc mehr",
                        style    = MaterialTheme.typography.labelSmall,
                        color    = linkColor,
                        modifier = Modifier
                            .clickable { expanded = !expanded }
                            .padding(start = 8.dp),
                    )
                }
            }
        } else if (isLong || expanded) {
            Text(
                text     = if (expanded) "\u25b2 weniger" else "\u25bc mehr",
                style    = MaterialTheme.typography.labelSmall,
                color    = linkColor,
                modifier = Modifier
                    .align(Alignment.End)
                    .clickable { expanded = !expanded }
                    .padding(top = 2.dp),
            )
        }
    }
}

/**
 * Builds an [AnnotatedString] from structured [ArticleSpan]s with proper formatting
 * (italic for definitions, small-caps for authors) and clickable citation links.
 */
private fun buildArticleAnnotatedString(
    spans: List<ArticleSpan>,
    linkColor: Color,
): AnnotatedString = buildAnnotatedString {
    // Group consecutive spans with the same linkUrl so each link region gets one annotation
    var i = 0
    while (i < spans.size) {
        val span = spans[i]
        val style = when (span.style) {
            com.example.mhdtranslator.model.TypesetStyle.ITALIC ->
                SpanStyle(fontStyle = FontStyle.Italic)
            com.example.mhdtranslator.model.TypesetStyle.CAPS ->
                SpanStyle(fontFeatureSettings = "smcp")
            else -> null
        }

        if (span.linkUrl != null) {
            // Collect all consecutive spans sharing this link URL
            val linkUrl = span.linkUrl
            val linkStart = length
            while (i < spans.size && spans[i].linkUrl == linkUrl) {
                val s = spans[i]
                val ls = when (s.style) {
                    com.example.mhdtranslator.model.TypesetStyle.ITALIC ->
                        SpanStyle(fontStyle = FontStyle.Italic)
                    com.example.mhdtranslator.model.TypesetStyle.CAPS ->
                        SpanStyle(fontFeatureSettings = "smcp")
                    else -> null
                }
                if (ls != null) withStyle(ls) { append(s.text) } else append(s.text)
                i++
            }
            val linkEnd = length
            if (linkEnd > linkStart) {
                addLink(
                    url = LinkAnnotation.Url(
                        url = linkUrl,
                        styles = TextLinkStyles(
                            SpanStyle(
                                color = linkColor,
                                textDecoration = TextDecoration.Underline,
                            )
                        ),
                    ),
                    start = linkStart,
                    end = linkEnd,
                )
            }
        } else {
            if (style != null) withStyle(style) { append(span.text) } else append(span.text)
            i++
        }
    }
}

/** Plain-text fallback for entries without structured spans (e.g. definition results). */
private fun buildArticleAnnotatedStringPlain(
    text: String,
): AnnotatedString = buildAnnotatedString {
    append(text)
}

@Composable
fun AboutDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val linkColor = MaterialTheme.colorScheme.primary
    val githubUrl  = "https://github.com/Gornhoth/enite-mittelhochdeutsches-woerterbuch"
    val privacyUrl = "$githubUrl/blob/main/PRIVACY.md"
    val donateUrl  = "https://ko-fi.com/johannesschatteiner"

    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Schließen")
            }
        },
        title = { Text("Über Enite") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Entwickelt von Johannes Schatteiner",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Text(
                    buildAnnotatedString {
                        append("Quellcode: ")
                        val start = length
                        append("GitHub")
                        addLink(
                            url = LinkAnnotation.Url(
                                url = githubUrl,
                                styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                            ),
                            start = start,
                            end = length,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                )
                androidx.compose.material3.OutlinedButton(
                    onClick = {
                        context.startActivity(Intent(Intent.ACTION_VIEW, donateUrl.toUri()))
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("☕  App unterstützen")
                }
                HorizontalDivider()
                Text(
                    "Die Wörterbuchdaten werden über die öffentliche API des Wörterbuchnetzes abgerufen " +
                    "(Kompetenzzentrum für elektronische Erschließungs- und Publikationsverfahren in den " +
                    "Geisteswissenschaften, Universität Trier). Diese App ist ein unabhängiges Open-Source-Projekt " +
                    "und steht in keiner Verbindung zur Universität Trier oder zum Kompetenzzentrum.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    buildAnnotatedString {
                        append("Lizenz: MIT  ·  ")
                        val start = length
                        append("Datenschutzerklärung")
                        addLink(
                            url = LinkAnnotation.Url(
                                url = privacyUrl,
                                styles = TextLinkStyles(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline)),
                            ),
                            start = start,
                            end = length,
                        )
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
    )
}

@Composable
fun ArticleListRow(entry: LexerEntry, onEntryClick: (String) -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEntryClick(entry.lemma) }
            .padding(horizontal = 4.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text  = entry.lemma,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (entry.gram != null) {
            Text(
                text      = ", ${entry.gram}",
                style     = MaterialTheme.typography.bodySmall,
                fontStyle = FontStyle.Italic,
                color     = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// ─── Helper composables ───────────────────────────────────────────────────────

@Composable
fun LoadingSpinner() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .wrapContentSize(Alignment.Center)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(40.dp))
    }
}

@Composable
fun ErrorText(message: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .wrapContentSize(Alignment.TopCenter)
    ) {
        Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun HintText(hint: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp)
            .wrapContentSize(Alignment.TopCenter)
    ) {
        Text(
            text = hint,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
