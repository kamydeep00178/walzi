package com.yunok.walzi.presentation.favorites

import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.presentation.common.MviEffect
import com.yunok.walzi.presentation.common.MviIntent
import com.yunok.walzi.presentation.common.MviState

data class FavoritesState(
    val wallpapers: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = true
) : MviState

sealed interface FavoritesIntent : MviIntent {
    data class ToggleFavorite(val wallpaperId: String) : FavoritesIntent
}

sealed interface FavoritesEffect : MviEffect
