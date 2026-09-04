package com.yunok.walzi.presentation.lists

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.presentation.common.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListDetailViewModel @Inject constructor(
    private val listRepository: WallpaperListRepository,
    private val wallpaperRepository: WallpaperRepository,
    savedStateHandle: SavedStateHandle
) : BaseViewModel<ListDetailIntent, ListDetailState, ListDetailEffect>(ListDetailState()) {

    private val listId: String = savedStateHandle.get<String>("listId").orEmpty()

    init {
        setState { copy(listId = listId) }
        viewModelScope.launch {
            listRepository.observeLists().collect { lists ->
                val list = lists.firstOrNull { it.id == listId } ?: return@collect
                setState { copy(listName = list.name, isDefault = list.isDefault, isLoading = true) }
                val wallpapers = wallpaperRepository.getWallpapersByIds(list.wallpaperIds)
                setState { copy(wallpapers = wallpapers, isLoading = false) }
            }
        }
        viewModelScope.launch {
            listRepository.observeAutoRotateSettings().collect { settings ->
                setState { copy(isActiveAutoRotateList = settings.activeListId == listId) }
            }
        }
    }

    override suspend fun handleIntent(intent: ListDetailIntent) {
        when (intent) {
            is ListDetailIntent.RemoveFromList -> listRepository.removeWallpaperFromList(listId, intent.wallpaperId)
            ListDetailIntent.SetAsActiveAutoRotateList -> listRepository.setActiveAutoRotateList(listId)

            ListDetailIntent.OpenRenameDialog -> setState { copy(showRenameDialog = true, renameDraft = listName) }
            ListDetailIntent.DismissRenameDialog -> setState { copy(showRenameDialog = false) }
            is ListDetailIntent.UpdateRenameDraft -> setState { copy(renameDraft = intent.value) }
            ListDetailIntent.ConfirmRename -> {
                val name = currentState.renameDraft.trim()
                if (name.isEmpty()) return
                listRepository.renameList(listId, name)
                setState { copy(showRenameDialog = false) }
            }

            ListDetailIntent.OpenDeleteConfirm -> setState { copy(showDeleteConfirm = true) }
            ListDetailIntent.DismissDeleteConfirm -> setState { copy(showDeleteConfirm = false) }
            ListDetailIntent.ConfirmDelete -> {
                listRepository.deleteList(listId)
                setState { copy(showDeleteConfirm = false) }
                setEffect(ListDetailEffect.NavigateBack)
            }
        }
    }
}