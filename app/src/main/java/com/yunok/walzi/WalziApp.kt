package com.yunok.walzi

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.firebase.messaging.FirebaseMessaging
import com.yunok.walzi.ads.AdFreeManager
import com.yunok.walzi.domain.repository.WallpaperListRepository
import com.yunok.walzi.util.AutoRotateScheduler
import com.yunok.walzi.util.CrashReporter
import com.yunok.walzi.util.ProgressInterceptor
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltAndroidApp
class WalziApp : Application(), ImageLoaderFactory, Configuration.Provider {

    @Inject lateinit var hiltWorkerFactory: HiltWorkerFactory
    @Inject lateinit var wallpaperListRepository: WallpaperListRepository
    @Inject lateinit var autoRotateScheduler: AutoRotateScheduler

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(hiltWorkerFactory)
            .build()

    /** Process-lifetime scope for fire-and-forget startup work. SupervisorJob: one failure can't cancel the rest. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        // Crash reports only from release builds, so debugging crashes don't pollute the dashboard.
        FirebaseCrashlytics.getInstance().setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
        // Same for Analytics: production data only (the manifest flag already covers first launch).
        FirebaseAnalytics.getInstance(this).setAnalyticsCollectionEnabled(!BuildConfig.DEBUG)

        // Ads are initialised from MainActivity, after the consent flow - see AdsConfig.ADS_ENABLED.
        // Restores a still-running "Remove ads for 24 hours" reward (cheap: one prefs read).
        AdFreeManager.init(this)
        createNotificationChannel()
        subscribeToDefaultTopic()
        reArmAutoRotateIfEnabled()
    }

    /**
     * Safety net for cold start: if the user previously enabled daily auto-rotate, make sure
     * the periodic WorkManager job is actually enqueued (enqueueUniquePeriodicWork with KEEP
     * policy below is a no-op if it's already scheduled, so this is cheap and idempotent).
     */
    private fun reArmAutoRotateIfEnabled() {
        appScope.launch {
            try {
                if (wallpaperListRepository.observeAutoRotateSettings().first().enabled) {
                    autoRotateScheduler.schedule()
                }
            } catch (e: Exception) {
                CrashReporter.record(e)
            }
        }
    }

    /**
     * Coil auto-detects that Application implements ImageLoaderFactory and uses this as the
     * app-wide singleton - every AsyncImage call (grid cards, category tiles, full-screen
     * preview) automatically benefits from this config with no per-call changes needed.
     *
     * Tuned specifically for a wallpaper app:
     *  - Memory cache: 25% of available app memory, so scrolling the masonry grid re-shows
     *    already-seen wallpapers instantly without re-decoding. It only stays useful because
     *    nothing puts full-resolution bitmaps into it: the detail preview decodes at screen
     *    size, and set/download bypass it (see util/ImageRequests.kt).
     *  - Disk cache: capped at 250MB on-device, so wallpapers survive process death / app
     *    restarts without needing to re-download from R2 every time.
     *  - respectCacheHeaders(false): R2 doesn't set long-lived Cache-Control headers by
     *    default, so we ignore the server's cache hints entirely and let Coil's own disk
     *    cache policy (content never changes once uploaded) be the single source of truth.
     *  - crossfade: subtle fade-in so cache hits vs. fresh network loads both feel smooth.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("wallpaper_image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250MB
                    .build()
            }
            // Reports real download progress for URLs a screen tracks (full preview progress bar).
            .okHttpClient { OkHttpClient.Builder().addNetworkInterceptor(ProgressInterceptor()).build() }
            .respectCacheHeaders(false)
            .crossfade(200)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                getString(R.string.default_notification_channel_id),
                getString(R.string.default_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * The admin portal can target this topic to reach every install.
     * Feel free to add more granular topics (e.g. per-category) the same way.
     */
    private fun subscribeToDefaultTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
    }
}