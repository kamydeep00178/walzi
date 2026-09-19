package com.yunok.walzi.presentation.search

import com.yunok.walzi.domain.model.Category
import com.yunok.walzi.domain.model.Tag
import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.presentation.common.MviEffect
import com.yunok.walzi.presentation.common.MviIntent
import com.yunok.walzi.presentation.common.MviState

data class SearchState(
    val query: String = "",
    val matchingCategories: List<Category> = emptyList(),
    val matchingTags: List<Tag> = emptyList(),
    val nearTags: List<Tag> = emptyList(),
    val otherTags: List<Tag> = emptyList(),
    val selectedTag: String? = null,
    val isLoadingResults: Boolean = false,
    val wallpaperResults: List<Wallpaper> = emptyList(),
    val resultsError: String? = null,
    val recentTagSearches: List<String> = emptyList()
) : MviState

sealed interface SearchIntent : MviIntent {
    data class QueryChanged(val query: String) : SearchIntent
    data object ClearQuery : SearchIntent
    data class TagTapped(val tagName: String) : SearchIntent
}

sealed interface SearchEffect : MviEffect