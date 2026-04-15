package com.example.mhdtranslator.ui

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.mhdtranslator.api.LexerApi
import com.example.mhdtranslator.model.SearchResult
import com.example.mhdtranslator.model.Suggestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import androidx.core.content.edit

sealed class SearchState {
    data object Idle : SearchState()
    data object Loading : SearchState()
    data class Success(val result: SearchResult) : SearchState()
    data class Error(val message: String) : SearchState()
}

class MHDTranslatorViewModel(application: Application) : AndroidViewModel(application) {
    private val api = LexerApi()
    private val prefs = application.getSharedPreferences("ui_prefs", Context.MODE_PRIVATE)

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private val _suggestions = MutableStateFlow<List<Suggestion>>(emptyList())
    val suggestions: StateFlow<List<Suggestion>> = _suggestions.asStateFlow()

    // ─── Theme preference ─────────────────────────────────────────────────────

    private val _themeMode = MutableStateFlow(
        ThemeMode.valueOf(prefs.getString("theme_mode", ThemeMode.SYSTEM.name) ?: ThemeMode.SYSTEM.name)
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit { putString("theme_mode", mode.name) }
    }

    // ─── Persistent section-expansion states ─────────────────────────────────

    private val _lemmataExpanded     = MutableStateFlow(prefs.getBoolean("lemmata_expanded", false))
    private val _definitionsExpanded = MutableStateFlow(prefs.getBoolean("definitions_expanded", false))
    private val _fulltextExpanded    = MutableStateFlow(prefs.getBoolean("fulltext_expanded", true))
    private val _articleExpanded     = MutableStateFlow(prefs.getBoolean("article_expanded", false))

    val lemmataExpanded: StateFlow<Boolean>     = _lemmataExpanded.asStateFlow()
    val definitionsExpanded: StateFlow<Boolean> = _definitionsExpanded.asStateFlow()
    val fulltextExpanded: StateFlow<Boolean>    = _fulltextExpanded.asStateFlow()
    val articleExpanded: StateFlow<Boolean>     = _articleExpanded.asStateFlow()

    fun setLemmataExpanded(expanded: Boolean) {
        _lemmataExpanded.value = expanded
        prefs.edit { putBoolean("lemmata_expanded", expanded) }
    }

    fun setDefinitionsExpanded(expanded: Boolean) {
        _definitionsExpanded.value = expanded
        prefs.edit { putBoolean("definitions_expanded", expanded) }
    }

    fun setFulltextExpanded(expanded: Boolean) {
        _fulltextExpanded.value = expanded
        prefs.edit { putBoolean("fulltext_expanded", expanded) }
    }

    fun setArticleExpanded(expanded: Boolean) {
        _articleExpanded.value = expanded
        prefs.edit { putBoolean("article_expanded", expanded) }
    }

    // ─── Navigation history ───────────────────────────────────────────────────

    // ─── Result cache ─────────────────────────────────────────────────────────

    private val resultCache = mutableMapOf<String, SearchResult>()

    // ─── Navigation history ───────────────────────────────────────────────────

    private val backStack = mutableListOf<String>()

    private val _canGoBack = MutableStateFlow(false)
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    /** Navigate to a word, pushing the current query onto the back stack. */
    fun navigateTo(word: String) {
        val current = _query.value
        if (current.isNotBlank()) {
            backStack.add(current)
            _canGoBack.value = true
        }
        _query.value = word
        performSearch(word)
    }

    /** Pop the back stack and return to the previous word. Returns false if already at root. */
    fun navigateBack(): Boolean {
        if (backStack.isEmpty()) return false
        val prev = backStack.removeAt(backStack.lastIndex)
        _canGoBack.value = backStack.isNotEmpty()
        _query.value = prev
        performSearch(prev)
        return true
    }

    // ─── Autocomplete ─────────────────────────────────────────────────────────

    private var suggestJob: Job? = null

    fun onQueryChange(text: String) {
        _query.value = text
        suggestJob?.cancel()
        if (text.isEmpty()) {
            _suggestions.value = emptyList()
            return
        }
        suggestJob = viewModelScope.launch {
            delay(150)
            _suggestions.value = api.fetchSuggestions(text)
        }
    }

    fun clearSuggestions() {
        suggestJob?.cancel()
        _suggestions.value = emptyList()
    }

    fun selectSuggestion(text: String) {
        clearSuggestions()
        _query.value = text
        search(text)
    }

    // ─── Search ───────────────────────────────────────────────────────────────

    /** User-initiated search — resets the back stack. */
    fun search(word: String = _query.value) {
        backStack.clear()
        _canGoBack.value = false
        performSearch(word)
    }

    private fun performSearch(word: String) {
        suggestJob?.cancel()
        _suggestions.value = emptyList()
        if (word.isBlank()) {
            _state.value = SearchState.Idle
            return
        }
        val key = word.trim()
        val cached = resultCache[key]
        if (cached != null) {
            _state.value = SearchState.Success(cached)
            return
        }
        viewModelScope.launch {
            _state.value = SearchState.Loading
            _state.value = try {
                val result = api.search(word)
                if (result.lemmataResults.isEmpty() && result.definitionResults.isEmpty() && result.fulltextResults.isEmpty()) {
                    SearchState.Error("Keine Ergebnisse f\u00FCr \u201E$word\u201C")
                } else {
                    resultCache[key] = result
                    SearchState.Success(result)
                }
            } catch (e: Exception) {
                SearchState.Error("Fehler: ${e.message}")
            }
        }
    }
}
