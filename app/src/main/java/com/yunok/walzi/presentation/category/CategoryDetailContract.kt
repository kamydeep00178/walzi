package com.yunok.walzi.presentation.category

import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.presentation.common.MviEffect
import com.yunok.walzi.presentation.common.MviIntent
import com.yunok.walzi.presentation.common.MviState

data class CategoryDetailState(
    val categoryId: String = "",
    val categoryName: String = "",
    val wallpapers: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val endReached: Boolean = false,
    /** First load failed and there is nothing cached to show - the UI offers a Retry. */
    val hasError: Boolean = false
) : MviState

sealed interface CategoryDetailIntent : MviIntent {
    data class ToggleFavorite(val wallpaperId: String) : CategoryDetailIntent
    data object LoadNextPage : CategoryDetailIntent
    data object Retry : CategoryDetailIntent
}

sealed interface CategoryDetailEffect : MviEffect
