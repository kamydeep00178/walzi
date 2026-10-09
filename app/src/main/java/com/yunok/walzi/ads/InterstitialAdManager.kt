package com.yunok.walzi.ads

import android.app.Activity
import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InterstitialAdManager"

/**
 * Loads and shows interstitial (full-screen) ads at natural break points only - after a
 * wallpaper is set / downloaded, or when opening a category. Never on app open/exit, never
 * while swiping wallpapers, never from background work.
 *
 * Two caps, both must allow it:
 *  - Cadence: one ad per [SHOW_EVERY_N_TRIGGERS] triggers, skipping the very first one, so a
 *    new user's first action is never interrupted (shows on the 2nd, 6th, 10th, ...).
 *  - Time: at least [MIN_INTERVAL_MS] since the last full-screen ad of any kind (interstitial
 *    or rewarded - see [markFullScreenAdShown]).
 * Both are persisted, so they survive app restarts. Nothing shows while the user is ad-free.
 *
 * A loaded ad older than [AD_MAX_AGE_MS] is discarded and reloaded instead of being shown stale
 * (AdMob full-screen ads expire after about an hour).
 *
 * Usage: call [show] every time a triggering action happens and continue the flow in
 * onDismissed - it runs after the ad closes, or immediately when this trigger is skipped.
 */
@Singleton
class InterstitialAdManager @Inject constructor() {

    private var interstitialAd: InterstitialAd? = null
    private var loadedAtElapsed = 0L
    private var isLoading = false
    private var isShowing = false

    /** Pre-load the ad. Requests made before the SDK finished initialising are dropped. */
    fun load(context: Context) {
        if (!AdsConfig.ADS_ENABLED) return
        if (!AdManager.isInitialized.value) return
        if (isLoading || freshAd() != null) return
        isLoading = true

        InterstitialAd.load(
            context.applicationContext,
            AdManager.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    loadedAtElapsed = SystemClock.elapsedRealtime()
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Interstitial failed to load: ${error.message}")
                    interstitialAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Call every time the triggering action happens. Decides by itself whether this call shows
     * an ad. [onDismissed] always runs exactly once - after the ad, or straight away if skipped.
     */
    fun show(activity: Activity, onDismissed: () -> Unit = {}) {
        // Off, or ad-free reward active: proceed immediately and don't advance the counter.
        if (!AdsConfig.ADS_ENABLED || AdFreeManager.isAdFreeNow()) { onDismissed(); return }
        // A second tap while an ad is on screen: the first call's onDismissed continues the flow.
        if (isShowing) return
        if (activity.isFinishing || activity.isDestroyed) { onDismissed(); return }

        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val triggerCount = prefs.getInt(KEY_TRIGGER_COUNT, 0) + 1
        prefs.edit().putInt(KEY_TRIGGER_COUNT, triggerCount).apply()

        val cadenceAllows = triggerCount % SHOW_EVERY_N_TRIGGERS == 2
        val sinceLast = System.currentTimeMillis() - prefs.getLong(KEY_LAST_FULLSCREEN_AT, 0L)
        if (!cadenceAllows || sinceLast < MIN_INTERVAL_MS) {
            onDismissed()
            return
        }

        val ad = freshAd()
        if (ad == null) {
            load(activity)
            onDismissed()
            return
        }

        val appContext = activity.applicationContext
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                markFullScreenAdShown(appContext)
            }

            override fun onAdDismissedFullScreenContent() {
                isShowing = false
                interstitialAd = null
                load(appContext) // pre-load the next one
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                isShowing = false
                interstitialAd = null
                load(appContext)
                onDismissed()
            }
        }
        isShowing = true
        ad.show(activity)
    }

    /** The loaded ad, or null - an expired one is dropped so it gets reloaded. */
    private fun freshAd(): InterstitialAd? {
        val ad = interstitialAd ?: return null
        if (SystemClock.elapsedRealtime() - loadedAtElapsed > AD_MAX_AGE_MS) {
            Log.d(TAG, "Dropping expired interstitial")
            interstitialAd = null
            return null
        }
        return ad
    }

    /** Records that a full-screen ad (interstitial or rewarded) was just shown - starts the time cap. */
    fun markFullScreenAdShown(context: Context) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putLong(KEY_LAST_FULLSCREEN_AT, System.currentTimeMillis()).apply()
    }

    companion object {
        private const val PREFS_NAME = "interstitial_ad_prefs"
        private const val KEY_TRIGGER_COUNT = "trigger_count"
        private const val KEY_LAST_FULLSCREEN_AT = "last_fullscreen_at"

        /** One ad per this many triggers (on the 2nd, 6th, 10th, ...). */
        private const val SHOW_EVERY_N_TRIGGERS = 4

        /** Minimum gap between any two full-screen ads. */
        private val MIN_INTERVAL_MS = TimeUnit.SECONDS.toMillis(90)

        /** AdMob full-screen ads expire after ~1 hour; drop ours a little before that. */
        private val AD_MAX_AGE_MS = TimeUnit.MINUTES.toMillis(55)
    }
}
