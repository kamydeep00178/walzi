package com.yunok.walzi.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.MobileAds
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
    private const val TEST_BANNER_ID       = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_INTERSTITIAL_ID = "ca-app-pub-3940256099942544/1033173712"
    private const val TEST_NATIVE_ID       = "ca-app-pub-3940256099942544/2247696110"

    // ── Real units (used by release builds) ──────────────────────────────────
    // TODO(before enabling ads): these are NOT verified against a real AdMob app. Create the app
    // and units in the AdMob console and paste the real ids here, or ads won't serve.
    private const val RELEASE_BANNER_ID       = "ca-app-pub-4136650480208705/9399995791"
    private const val RELEASE_INTERSTITIAL_ID = "ca-app-pub-4136650480208705/6857592938"
    private const val RELEASE_NATIVE_ID       = "ca-app-pub-4136650480208705/4661840286"

    val BANNER_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_BANNER_ID else RELEASE_BANNER_ID

    val INTERSTITIAL_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_INTERSTITIAL_ID else RELEASE_INTERSTITIAL_ID

    val NATIVE_AD_UNIT_ID: String
        get() = if (BuildConfig.DEBUG) TEST_NATIVE_ID else RELEASE_NATIVE_ID

    private val started = AtomicBoolean(false)
    private val _isInitialized = MutableStateFlow(false)

    /** True once the Mobile Ads SDK has finished initialising. Ad loads wait for this. */
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    /** Safe to call any number of times (e.g. from both the cached-consent and fresh-consent paths). */
    fun initialize(context: Context) {
        if (!AdsConfig.ADS_ENABLED) return
        if (!started.compareAndSet(false, true)) return

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
