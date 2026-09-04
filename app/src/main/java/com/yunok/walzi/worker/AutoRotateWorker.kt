package com.yunok.walzi.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.util.WallpaperSetter
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * Runs roughly once every 24 hours (scheduled by AutoRotateScheduler). Advances the active
 * list's rotation pointer by one and sets that wallpaper via the same WallpaperSetter the
 * manual "Set Wallpaper" action uses. Android does not guarantee exact daily timing under
 * Doze/battery optimization - this is expected and matches how every wallpaper-rotation app
 * behaves without requesting exact-alarm permissions.
 */
@HiltWorker
class AutoRotateWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val wallpaperListRepository: WallpaperListRepository,
    private val wallpaperSetter: WallpaperSetter
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val settings = wallpaperListRepository.observeAutoRotateSettings().first()
        if (!settings.enabled) return Result.success()

        val wallpaper = wallpaperListRepository.advanceAndGetNextRotationWallpaper()
            ?: return Result.success() // active list is empty - nothing to do, not a failure

        val outcome = wallpaperSetter.setWallpaper(wallpaper.imageUrl, settings.target)
        return if (outcome.isSuccess) Result.success() else Result.retry()
    }
}