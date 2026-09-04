package com.yunok.walzi.presentation.lists

import com.yunok.walzi.domain.model.Wallpaper
import com.yunok.walzi.presentation.common.MviEffect
import com.yunok.walzi.presentation.common.MviIntent
import com.yunok.walzi.presentation.common.MviState

data class ListDetailState(
    val listId: String = "",
    val listName: String = "",
    val isDefault: Boolean = false,
    val isActiveAutoRotateList: Boolean = false,
    val wallpapers: List<Wallpaper> = emptyList(),
    val isLoading: Boolean = true,
    val showRenameDialog: Boolean = false,
    val renameDraft: String = "",
    val showDeleteConfirm: Boolean = false
) : MviState

sealed interface ListDetailIntent : MviIntent {
    data class RemoveFromList(val wallpaperId: String) : ListDetailIntent
    data object SetAsActiveAutoRotateList : ListDetailIntent

    data object OpenRenameDialog : ListDetailIntent
    data object DismissRenameDialog : ListDetailIntent
    data class UpdateRenameDraft(val value: String) : ListDetailIntent
    data object ConfirmRename : ListDetailIntent

    data object OpenDeleteConfirm : ListDetailIntent
    data object DismissDeleteConfirm : ListDetailIntent
    data object ConfirmDelete : ListDetailIntent
}

sealed interface ListDetailEffect : MviEffect {
    /** Fired after the list is deleted - the screen should pop back since it no longer exists. */
    data object NavigateBack : ListDetailEffect
}