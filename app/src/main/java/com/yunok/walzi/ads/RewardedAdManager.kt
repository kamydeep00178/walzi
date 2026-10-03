package com.yunok.walzi.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "RewardedAdManager"

/**
 * Rewarded video ads. Policy rules this follows:
 *  - Opt-in only: [show] is called only after the user tapped "Watch ad" in a dialog that
 *    says exactly what they get. Never shown automatically.
 *  - The reward is granted only from the SDK's onUserEarnedReward callback.
 *  - It counts toward the interstitial time cap, so an interstitial never follows right after.
 */
@Singleton
class RewardedAdManager @Inject constructor(
    private val interstitialAdManager: InterstitialAdManager
) {
    private var rewardedAd: RewardedAd? = null
    private var isLoading = false

    /** True when an ad is loaded and [show] would actually play one. */
    val isReady: Boolean get() = AdsConfig.ADS_ENABLED && rewardedAd != null

    fun load(context: Context) {
        if (!AdsConfig.ADS_ENABLED) return
        if (!AdManager.isInitialized.value) return
        if (isLoading || rewardedAd != null) return
        isLoading = true

        RewardedAd.load(
            context.applicationContext,
            AdManager.REWARDED_AD_UNIT_ID,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isLoading = false
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Rewarded failed to load: ${error.message}")
                    rewardedAd = null
                    isLoading = false
                }
            }
        )
    }

    /**
     * Plays the ad. [onResult] runs once when it closes: true if the user watched long enough
     * to earn the reward, false if they closed it early or no ad could be shown.
     */
    fun show(activity: Activity, onResult: (earned: Boolean) -> Unit) {
        val ad = rewardedAd
        if (!AdsConfig.ADS_ENABLED || ad == null || activity.isFinishing || activity.isDestroyed) {
            load(activity)
            onResult(false)
            return
        }

        val appContext = activity.applicationContext
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                interstitialAdManager.markFullScreenAdShown(appContext)
            }

            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                load(appContext) // pre-load the next one
                onResult(earned)
            }

            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                Log.w(TAG, "Rewarded failed to show: ${error.message}")
                rewardedAd = null
                load(appContext)
                onResult(false)
            }
        }
        rewardedAd = null // a RewardedAd can only be shown once
        ad.show(activity) { earned = true }
    }
}
