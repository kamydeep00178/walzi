package com.yunok.walzi.domain.model

/**
 * A user-created (or the built-in default) collection of wallpapers, kept purely on-device.
 * [wallpaperIds] is stored in add-order, which matters for sequential auto-rotation - the
 * worker walks this list in order and wraps back to the start once it reaches the end.
 */
data class WallpaperList(
    val id: String,
    val name: String,
    val wallpaperIds: List<String>,
    val isDefault: Boolean
)

/** The id every install starts with - always exists, never deletable, only renamable. */
const val DEFAULT_LIST_ID = "default"