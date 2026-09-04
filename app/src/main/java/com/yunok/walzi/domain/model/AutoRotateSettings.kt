package com.yunok.walzi.domain.model

import com.yunok.walzi.util.WallpaperTarget

/**
 * Persisted daily-rotation preference. [activeListId] is the list AutoRotateWorker pulls the
 * next wallpaper from; [lastRotatedWallpaperId] tracks position within that list so rotation
 * resumes from the right spot after a restart, app kill, or the active list being switched.
 */
data class AutoRotateSettings(
    val enabled: Boolean,
    val activeListId: String,
    val target: WallpaperTarget,
    val lastRotatedWallpaperId: String?
)