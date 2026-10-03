package com.yunok.walzi.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.yunok.walzi.BuildConfig
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Central place for AdMob configuration and SDK initialisation. Everything here is a no-op
 * unless [AdsConfig.ADS_ENABLED] is true.
 *
 * Ad units: debug builds use Google's official test units (always safe, never invalid traffic);
 * release builds use the real units below. The AdMob *app* id lives in build.gradle.kts
 * (`ADMOB_APP_ID_RELEASE` in gradle.properties) - it must belong to the same AdMob app as
 * these unit ids.
 */
object AdManager {

    private const val TAG = "AdManager"

    // ── Test units (Google's official ids - used for every debug build) ──────
    private const val TEST_BANNER_ID       = "ca-app-pub-3940256099942544/9214589741" // adaptive banner
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_NATIVE_ID       = "ca-app-pub-3940256099942544/2247696110"
    private const val TEST_REWARDED_ID     = "ca-app-pub-3940256099942544/5224354917"

    // ── Real units (used by release builds) ──────────────────────────────────
    // TODO(before enabling ads): create the app + these 4 units in the AdMob console
    // (Banner, Interstitial, Native advanced, Rewarded) and paste the real ids here.
    private const val RELEASE_BANNER_ID       = "ca-app-pub-4136650480208705/8460550657"
    private const val RELEASE_INTERSTITIAL_ID = "ca-app-pub-4136650480208705/4117805955"
    private const val RELEASE_NATIVE_ID       = "ca-app-pub-4136650480208705/8728143966"
    private const val RELEASE_REWARDED_ID     = "ca-app-pub-4136650480208705/6293552317"

    val BANNER_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_ID else RELEASE_BANNER_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else RELEASE_INTERSTITIAL_ID

    val NATIVE_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_NATIVE_ID else RELEASE_NATIVE_ID

    val REWARDED_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_REWARDED_ID else RELEASE_REWARDED_ID

    private val started = AtomicBoolean(false)
    private val _isInitialized = MutableStateFlow(false)

    /** True once the Mobile Ads SDK has finished initialising. Ad loads wait for this. */
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** Safe to call any number of times (e.g. from both the cached-consent and fresh-consent paths). */
    fun initialize(context: Context) {
        if (!AdsConfig.ADS_ENABLED) return
        if (!started.compareAndSet(false, true)) return

        // Must be set before the first ad request: caps ad content at AdsConfig.MAX_AD_CONTENT_RATING
        // and marks our own phones as test devices.
        MobileAds.setRequestConfiguration(
            RequestConfiguration.Builder()
                .setMaxAdContentRating(AdsConfig.MAX_AD_CONTENT_RATING)
                .setTestDeviceIds(AdsConfig.TEST_DEVICE_IDS)
                .build()
        )

        val appContext = context.applicationContext
        // Google recommends initialising off the main thread - it can take a noticeable time.
        CoroutineScope(Dispatchers.IO).launch {
            MobileAds.initialize(appContext) { initStatus ->
                if (BuildConfig.DEBUG) {
                    initStatus.adapterStatusMap.forEach { (adapter, status) ->
                        Log.d(TAG, "Adapter: $adapter — ${status.initializationState}")
                    }
                }
                _isInitialized.value = true
            }
        }
    }
}
