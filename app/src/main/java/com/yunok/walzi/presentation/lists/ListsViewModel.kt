package com.yunok.walzi.presentation.lists

import androidx.lifecycle.viewModelScope
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.presentation.common.BaseViewModel
import com.yunok.walzi.util.AutoRotateScheduler
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ListsViewModel @Inject constructor(
    private val repository: WallpaperListRepository,
    private val autoRotateScheduler: AutoRotateScheduler
) : BaseViewModel<ListsIntent, ListsState, ListsEffect>(ListsState()) {

    init {
        viewModelScope.launch {
            repository.observeLists().collect { lists -> setState { copy(lists = lists) } }
        }
        viewModelScope.launch {
            repository.observeAutoRotateSettings().collect { settings ->
                setState { copy(autoRotateSettings = settings) }
            }
        }
    }

    override suspend fun handleIntent(intent: ListsIntent) {
        when (intent) {
            ListsIntent.OpenCreateListDialog -> setState { copy(showCreateListDialog = true, newListNameDraft = "") }
            ListsIntent.DismissCreateListDialog -> setState { copy(showCreateListDialog = false) }
            is ListsIntent.UpdateNewListNameDraft -> setState { copy(newListNameDraft = intent.value) }

            ListsIntent.ConfirmCreateList -> {
                val name = currentState.newListNameDraft.trim()
                if (name.isEmpty()) return
                repository.createList(name)
                setState { copy(showCreateListDialog = false, newListNameDraft = "") }
            }

            is ListsIntent.ToggleAutoRotate -> {
                repository.setAutoRotateEnabled(intent.enabled)
                if (intent.enabled) autoRotateScheduler.schedule() else autoRotateScheduler.cancel()
            }

            ListsIntent.OpenActiveListPicker -> setState { copy(showActiveListPicker = true) }
            ListsIntent.DismissActiveListPicker -> setState { copy(showActiveListPicker = false) }

            is ListsIntent.SetActiveList -> {
                repository.setActiveAutoRotateList(intent.listId)
                setState { copy(showActiveListPicker = false) }
            }

            is ListsIntent.SetAutoRotateTarget -> repository.setAutoRotateTarget(intent.target)
        }
    }
}