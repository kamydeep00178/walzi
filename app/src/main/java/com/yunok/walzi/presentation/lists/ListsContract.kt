package com.yunok.walzi.presentation.lists

import com.yunok.walzi.domain.model.AutoRotateSettings
import com.yunok.walzi.domain.model.WallpaperList
import com.yunok.walzi.presentation.common.MviEffect
import com.yunok.walzi.presentation.common.MviIntent
import com.yunok.walzi.presentation.common.MviState
import com.yunok.walzi.util.WallpaperTarget

data class ListsState(
    val lists: List<WallpaperList> = emptyList(),
    val autoRotateSettings: AutoRotateSettings = AutoRotateSettings(
        enabled = false,
        activeListId = "",
        target = WallpaperTarget.BOTH,
        lastRotatedWallpaperId = null
    ),
    val showCreateListDialog: Boolean = false,
    val newListNameDraft: String = "",
    val showActiveListPicker: Boolean = false
) : MviState

sealed interface ListsIntent : MviIntent {
    data object OpenCreateListDialog : ListsIntent
    data object DismissCreateListDialog : ListsIntent
    data class UpdateNewListNameDraft(val value: String) : ListsIntent
    data object ConfirmCreateList : ListsIntent

    data class ToggleAutoRotate(val enabled: Boolean) : ListsIntent
    data object OpenActiveListPicker : ListsIntent
    data object DismissActiveListPicker : ListsIntent
    data class SetActiveList(val listId: String) : ListsIntent
    data class SetAutoRotateTarget(val target: WallpaperTarget) : ListsIntent
}

sealed interface ListsEffect : MviEffect