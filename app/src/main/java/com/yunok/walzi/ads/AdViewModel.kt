package com.yunok.walzi.ads

import android.app.Activity
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.ads.nativead.NativeAd
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

    /**
     * Fills up to [count] native ad slots for this screen (once), one at a time from the shared
     * [NativeAdCache] - usually instant, and unseen ads from earlier screens are reused instead
     * of requesting new ones. Stops at the first no-fill. Each ad is shown in one slot only.
     */
    fun requestNativeAds(count: Int) {
        if (!AdsConfig.ADS_ENABLED || nativeRequested) return
        nativeRequested = true
        viewModelScope.launch {
            repeat(count) {
                val ad = NativeAdCache.obtain(appContext) ?: return@launch
                // The ViewModel may have been cleared while waiting: give the ad back.
                if (!isActive) { NativeAdCache.recycle(ad); return@launch }
                loadedNativeAds = loadedNativeAds + ad
            }
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
        // Displayed ads are destroyed; never-displayed fresh ones go back to the cache for the
        // next screen (NativeAd holds native resources, so nothing is just dropped).
        loadedNativeAds.forEach { NativeAdCache.recycle(it) }
        loadedNativeAds = emptyList()
    }
}
