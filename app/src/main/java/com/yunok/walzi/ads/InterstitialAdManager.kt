package com.yunok.walzi.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "InterstitialAdManager"

/**
 * Manages loading and showing interstitial (full-screen) ads, with frequency
 * capping: shows on the 1st trigger, skips the next 3, shows again on the 5th,
 * skips 3, shows on the 9th, etc. — i.e. one ad shown per 4 triggers.
 *
 * The trigger counter is persisted in SharedPreferences (not just an in-memory
 * field), so the cadence survives app restarts — otherwise a user who force-
 * closes right after seeing an ad would see another one immediately on reopen.
 *
 * Typical usage — call `show()` every time the "triggering" action happens
 * (e.g. every time a video finishes, every notification opened, etc.) —
 * the manager itself decides whether THIS particular call actually shows
 * an ad or silently skips it:
 *
 *   val interstitialMgr = remember { InterstitialAdManager() }
 *   LaunchedEffect(Unit) { interstitialMgr.load(context) }
 *
 *   // On some action:
 *   interstitialMgr.show(activity, isPremium)
 */
@Singleton
class InterstitialAdManager @Inject constructor() {

    private var interstitialAd: InterstitialAd? = null
    private var isLoading = false

    /** Pre-load the ad. Call this early (e.g. in LaunchedEffect on screen open). */
    fun load(context: Context) {
        if (isLoading || interstitialAd != null) return
        isLoading = true

        InterstitialAd.load(
            context,
            AdManager.INTERSTITIAL_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    Log.d(TAG, "Interstitial loaded ✅")
                    interstitialAd = ad
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
     * Call this every time the triggering action happens. Internally decides
     * whether this particular call should actually show an ad (every 4th
     * trigger) or just skip straight to onDismissed().
     *
     * @param onDismissed called after the ad is closed, OR immediately if this
     * trigger was skipped, OR immediately if the user is premium — callers
     * should treat onDismissed as "proceed with whatever comes next" in all
     * three cases, without needing to know which one happened.
     */
    fun show(activity: Activity, isPremium: Boolean, onDismissed: () -> Unit = {}) {
        if (isPremium) { onDismissed(); return }

        val triggerCount = incrementAndGetTriggerCount(activity)
        val shouldShowThisTime = triggerCount % SHOW_EVERY_N_TRIGGERS == 1

        if (!shouldShowThisTime) {
            Log.d(TAG, "Skipping ad — trigger #$triggerCount (showing every $SHOW_EVERY_N_TRIGGERS)")
            onDismissed()
            return
        }

        val ad = interstitialAd
        if (ad == null) {
            Log.d(TAG, "This trigger should show an ad, but none is loaded yet — showing content immediately")
            load(activity)
            onDismissed()
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "Interstitial dismissed")
                interstitialAd = null
                load(activity)   // pre-load next one
                onDismissed()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Interstitial failed to show: ${error.message}")
                interstitialAd = null
                onDismissed()
            }
            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "Interstitial showing (trigger #$triggerCount)")
            }
        }
        ad.show(activity)
    }

    /** True if an ad is loaded and ready to show. */
    val isReady: Boolean get() = interstitialAd != null

    private fun incrementAndGetTriggerCount(context: Context): Int {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val next = prefs.getInt(KEY_TRIGGER_COUNT, 0) + 1
        prefs.edit().putInt(KEY_TRIGGER_COUNT, next).apply()
        return next
    }

    companion object {
        private const val PREFS_NAME = "interstitial_ad_prefs"
        private const val KEY_TRIGGER_COUNT = "trigger_count"

        /** Show 1 ad per this many triggers (currently: 1st, 5th, 9th, ...). */
        private const val SHOW_EVERY_N_TRIGGERS = 4
    }
}