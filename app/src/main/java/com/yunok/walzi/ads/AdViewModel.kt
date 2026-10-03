package com.yunok.walzi.ads

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bridge between Compose screens and the ads layer. Obtain with hiltViewModel() in any screen
 * that shows ads; each screen gets its own native ads, destroyed when the screen goes away.
 *
 * With [AdsConfig.ADS_ENABLED] off this does nothing: no loads, [nativeAds] stays empty,
 * interstitials are skipped and the rewarded ad is never ready.
 */
@HiltViewModel
class AdViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val interstitialAdManager: InterstitialAdManager,
    private val rewardedAdManager: RewardedAdManager
) : ViewModel() {

    private var loadedNativeAds by mutableStateOf<List<NativeAd>>(emptyList())
    private var adFree by mutableStateOf(AdFreeManager.isAdFreeNow())
    private var nativeRequested = false

    /** Native ads ready to show - empty until loaded, when ads are off, or while ad-free. */
    val nativeAds: List<NativeAd> get() = if (adFree) emptyList() else loadedNativeAds

    /** True when a rewarded ad is loaded, so a "Watch ad" offer can actually deliver. */
    val isRewardedReady: Boolean get() = rewardedAdManager.isReady

    init {
        if (AdsConfig.ADS_ENABLED) {
            viewModelScope.launch { AdFreeManager.isAdFree.collect { adFree = it } }
            viewModelScope.launch {
                // Consent may still be pending on a first launch - wait for the SDK instead of
                // firing requests that are guaranteed to fail.
                AdManager.isInitialized.first { it }
                interstitialAdManager.load(appContext)
                rewardedAdManager.load(appContext)
            }
        }
    }

    /** Loads up to [count] native ads for this screen (once). Each ad is shown in one slot only. */
    fun requestNativeAds(count: Int) {
        if (!AdsConfig.ADS_ENABLED || nativeRequested) return
        nativeRequested = true
        viewModelScope.launch {
            AdManager.isInitialized.first { it }
            AdLoader.Builder(appContext, AdManager.NATIVE_AD_UNIT_ID)
                .forNativeAd { ad ->
                    // The ViewModel may have been cleared while the request was in flight.
                    if (!viewModelScope.isActive) ad.destroy() else loadedNativeAds = loadedNativeAds + ad
                }
                // Landscape media suits the full-width big card; videos start muted (never surprise
                // the user with sound).
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setMediaAspectRatio(NativeAdOptions.NATIVE_MEDIA_ASPECT_RATIO_LANDSCAPE)
                        .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                        .build()
                )
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        Log.w(TAG, "Native ad failed to load: ${error.message}")
                    }
                })
                .build()
                .loadAds(AdRequest.Builder().build(), count)
        }
    }

    /** Frequency-capped by [InterstitialAdManager]; [onDismissed] always runs exactly once. */
    fun showInterstitial(activity: Activity, onDismissed: () -> Unit = {}) {
        interstitialAdManager.show(activity, onDismissed)
    }

    /** Call only after the user opted in. [onResult] is true if the reward was earned. */
    fun showRewarded(activity: Activity, onResult: (earned: Boolean) -> Unit) {
        rewardedAdManager.show(activity, onResult)
    }

    override fun onCleared() {
        // NativeAd holds native resources and must be destroyed explicitly.
        loadedNativeAds.forEach { it.destroy() }
        loadedNativeAds = emptyList()
    }

    private companion object {
        const val TAG = "AdViewModel"
    }
}
