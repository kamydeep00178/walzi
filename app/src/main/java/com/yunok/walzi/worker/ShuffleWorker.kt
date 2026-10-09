package com.yunok.walzi.worker

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.yunok.walzi.domain.model.WallpaperSource
import com.yunok.walzi.domain.model.cacheBucket
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.domain.repository.WallpaperRepository
import com.yunok.walzi.util.WallpaperApplier
import com.yunok.walzi.widget.ShuffleWidgetProvider
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import kotlinx.coroutines.flow.first

/**
 * One tap on the Shuffle widget: sets the next wallpaper from the auto-rotate list (same order
 * as daily rotation), or - if that list is empty - a random wallpaper from the cached Recent
 * feed. Uses the auto-rotate screen choice (Home / Lock / Both). Goes through
 * [WallpaperApplier], so it appears in History and can be undone.
 */
@HiltWorker
class ShuffleWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val listRepository: WallpaperListRepository,
    private val wallpaperRepository: WallpaperRepository,
    private val wallpaperApplier: WallpaperApplier
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result = try {
        val settings = listRepository.observeAutoRotateSettings().first()
        val wallpaper = listRepository.advanceAndGetNextRotationWallpaper()
            ?: wallpaperRepository.observeCachedWallpapers(WallpaperSource.Recent.cacheBucket()).first().randomOrNull()
        if (wallpaper != null) wallpaperApplier.apply(wallpaper, settings.target)
        // Never retried: a tap should change the wallpaper now or not at all, not minutes later.
        Result.success()
    } finally {
        ShuffleWidgetProvider.updateAll(applicationContext, busy = false)
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "walzi_widget_shuffle"

        /** REPLACE: rapid repeated taps run once, for the latest tap. */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<ShuffleWorker>().build()
            )
        }
    }
}
