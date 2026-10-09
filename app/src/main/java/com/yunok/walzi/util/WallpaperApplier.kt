package com.yunok.walzi.util

import com.yunok.walzi.domain.model.Duo
import com.yunok.walzi.domain.model.Wallpaper
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The one way Walzi applies a wallpaper - from the full preview, Duos, daily auto-rotate and the
 * Shuffle widget alike - so every apply lands in History and can be undone.
 */
@Singleton
class WallpaperApplier @Inject constructor(
    private val setter: WallpaperSetter,
    private val history: WallpaperHistoryStore
) {
    suspend fun apply(
        wallpaper: Wallpaper,
        target: WallpaperTarget,
        adjustments: WallpaperAdjustments = WallpaperAdjustments.NONE
    ): Result<Unit> = setter.setWallpaper(wallpaper.imageUrl, target, adjustments).onSuccess {
        history.add(wallpaper, target, adjustments)
    }

    /**
     * Applies a Duo. [target] BOTH = the real Duo (lock image on lock, home image on home);
     * HOME / LOCK = just that half on just that screen.
     */
    suspend fun applyDuo(duo: Duo, target: WallpaperTarget): Result<Unit> {
        val result = when (target) {
            WallpaperTarget.BOTH -> setter.setDuo(duo.homeImageUrl, duo.lockImageUrl)
            WallpaperTarget.HOME -> setter.setWallpaper(duo.homeImageUrl, WallpaperTarget.HOME)
            WallpaperTarget.LOCK -> setter.setWallpaper(duo.lockImageUrl, WallpaperTarget.LOCK)
        }
        return result.onSuccess {
            val isLock = target == WallpaperTarget.LOCK
            history.add(
                AppliedWallpaper(
                    wallpaperId = duo.id,
                    title = duo.title,
                    imageUrl = if (isLock) duo.lockImageUrl else duo.homeImageUrl,
                    thumbUrl = if (isLock) duo.lockCardUrl else duo.homeCardUrl,
                    categoryName = "Duo",
                    target = target,
                    adjustments = WallpaperAdjustments.NONE,
                    appliedAt = System.currentTimeMillis(),
                    lockImageUrl = if (target == WallpaperTarget.BOTH) duo.lockImageUrl else null
                )
            )
        }
    }

    /** "Apply again" from the History screen - same image(s), screen(s) and adjustments. */
    suspend fun reapply(entry: AppliedWallpaper): Result<Unit> =
        set(entry).onSuccess { history.addAgain(entry) }

    /** True when there is an earlier Walzi wallpaper to go back to. */
    val canUndo: Boolean get() = history.entries.value.size >= 2

    /**
     * Restores the wallpaper Walzi applied before the latest one (on that entry's own screens).
     * Fails if there is nothing earlier; History is left unchanged when re-applying fails.
     */
    suspend fun undo(): Result<AppliedWallpaper> {
        val entries = history.entries.value
        if (entries.size < 2) return Result.failure(IllegalStateException("Nothing to undo"))
        val previous = entries[1]
        return set(previous).map {
            history.removeLatest()
            previous
        }
    }

    private suspend fun set(entry: AppliedWallpaper): Result<Unit> = when (val lock = entry.lockImageUrl) {
        null -> setter.setWallpaper(entry.imageUrl, entry.target, entry.adjustments)
        else -> setter.setDuo(entry.imageUrl, lock)
    }
}
