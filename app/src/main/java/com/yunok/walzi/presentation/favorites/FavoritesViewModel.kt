package com.yunok.walzi.presentation.favorites

import androidx.lifecycle.viewModelScope
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class FavoritesViewModel @Inject constructor(
    private val repository: WallpaperRepository
) : BaseViewModel<FavoritesIntent, FavoritesState, FavoritesEffect>(FavoritesState()) {

    init {
        // Only ever fetches the specific favorited wallpaper docs (whereIn, chunked by 10) -
        // never the whole collection, regardless of how many wallpapers exist overall.
        viewModelScope.launch {
            repository.observeFavoriteIds().collect { ids ->
                setState { copy(isLoading = true) }
                val wallpapers = repository.getWallpapersByIds(ids.toList())
                setState { copy(wallpapers = wallpapers, isLoading = false) }
            }
        }
    }

    override suspend fun handleIntent(intent: FavoritesIntent) {
        when (intent) {
            is FavoritesIntent.ToggleFavorite -> repository.toggleFavorite(intent.wallpaperId)
        }
    }
}
