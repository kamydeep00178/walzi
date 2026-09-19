package com.yunok.walzi.presentation.search

import androidx.lifecycle.viewModelScope
import com.yunok.walzi.data.local.SearchHistoryDataStore
import com.yunok.walzi.domain.model.Category
import com.yunok.walzi.domain.model.Tag
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val QUERY_DEBOUNCE_MS = 200L
private const val OTHER_TAGS_COUNT = 12
private const val NEAR_TAGS_COUNT = 6

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val repository: WallpaperRepository,
    private val searchHistory: SearchHistoryDataStore
) : BaseViewModel<SearchIntent, SearchState, SearchEffect>(SearchState()) {

    private val queryFlow = MutableStateFlow("")
    private var allCategories: List<Category> = emptyList()
    private var allTags: List<Tag> = emptyList()
    private var resultsJob: Job? = null

    init {
        viewModelScope.launch {
            repository.observeCategories().collect { categories ->
                allCategories = categories
                recomputeSuggestions(currentState.query)
            }
        }
        viewModelScope.launch {
            repository.observeTags().collect { tags ->
                allTags = tags
                setState { copy(otherTags = tags.take(OTHER_TAGS_COUNT)) }
                recomputeSuggestions(currentState.query)
            }
        }
        viewModelScope.launch {
            searchHistory.recentSearches.collect { recent ->
                setState { copy(recentTagSearches = recent) }
            }
        }
        viewModelScope.launch {
            queryFlow
                .debounce(QUERY_DEBOUNCE_MS)
                .distinctUntilChanged()
                .collect { query -> recomputeSuggestions(query) }
        }
    }

    override suspend fun handleIntent(intent: SearchIntent) {
        when (intent) {
            is SearchIntent.QueryChanged -> {
                setState { copy(query = intent.query) }
                queryFlow.value = intent.query
            }
            SearchIntent.ClearQuery -> {
                resultsJob?.cancel()
                setState {
                    copy(
                        query = "",
                        matchingCategories = emptyList(),
                        matchingTags = emptyList(),
                        nearTags = emptyList(),
                        selectedTag = null,
                        wallpaperResults = emptyList(),
                        resultsError = null
                    )
                }
                queryFlow.value = ""
            }
            is SearchIntent.TagTapped -> selectTag(intent.tagName)
        }
    }

    private fun recomputeSuggestions(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) {
            setState { copy(matchingCategories = emptyList(), matchingTags = emptyList(), nearTags = emptyList()) }
            return
        }

        val categoryMatches = allCategories.filter { it.name.contains(trimmed, ignoreCase = true) }
        val tagMatches = allTags.filter { it.name.contains(trimmed, ignoreCase = true) }
        val near = if (tagMatches.isEmpty()) nearestTags(allTags, trimmed) else emptyList()

        setState {
            copy(matchingCategories = categoryMatches, matchingTags = tagMatches, nearTags = near)
        }
    }

    private fun nearestTags(all: List<Tag>, query: String): List<Tag> {
        if (all.isEmpty()) return emptyList()
        val sorted = all.sortedBy { it.name }
        val insertIndex = sorted.indexOfFirst { it.name >= query }.let { if (it == -1) sorted.size else it }
        val start = (insertIndex - NEAR_TAGS_COUNT / 2).coerceIn(0, sorted.size)
        val end = (start + NEAR_TAGS_COUNT).coerceAtMost(sorted.size)
        return sorted.subList(start, end)
    }

    private fun selectTag(tagName: String) {
        resultsJob?.cancel()
        resultsJob = viewModelScope.launch {
            setState { copy(selectedTag = tagName, isLoadingResults = true, resultsError = null) }
            try {
                val results = repository.searchWallpapersByTag(tagName)
                searchHistory.addSearch(tagName)
                setState { copy(isLoadingResults = false, wallpaperResults = results) }
            } catch (e: Exception) {
                android.util.Log.e("SearchViewModel", "Tag search failed for \"$tagName\"", e)
                setState { copy(isLoadingResults = false, resultsError = e.message ?: "Search failed") }
            }
        }
    }
}