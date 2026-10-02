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
import com.google.android.gms.ads.nativead.NativeAd
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Bridge between Compose screens and the ads layer. Obtain with hiltViewModel() wherever a
 * screen needs a native ad or wants to trigger an interstitial.
 *
 * With [AdsConfig.ADS_ENABLED] off this does nothing: no loads, [nativeAd] stays null, and
 * [showInterstitial] is a no-op.
 */
@HiltViewModel
class AdViewModel @Inject constructor(
    @ApplicationContext private val appContext: Context,
    private val interstitialAdManager: InterstitialAdManager
) : ViewModel() {

    /** The loaded native ad, or null until one is ready (or if ads are off / failed to load). */
    var nativeAd by mutableStateOf<NativeAd?>(null)
        private set

    init {
        if (AdsConfig.ADS_ENABLED) {
            viewModelScope.launch {
                // Consent may still be pending on a first launch - wait for the SDK instead of
                // firing requests that are guaranteed to fail.
                AdManager.isInitialized.first { it }
                loadNativeAd()
                interstitialAdManager.load(appContext)
            }
        }
    }

    private fun loadNativeAd() {
        if (nativeAd != null) return
        AdLoader.Builder(appContext, AdManager.NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                // The ViewModel may have been cleared while the request was in flight.
                if (!viewModelScope.isActive) {
                    ad.destroy()
                } else {
                    nativeAd?.destroy()
                    nativeAd = ad
                }
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    Log.w(TAG, "Native ad failed to load: ${error.message}")
                }
            })
            .build()
            .loadAd(AdRequest.Builder().build())
    }

    /** Frequency-capped by [InterstitialAdManager]; harmless to call after every qualifying action. */
    fun showInterstitial(activity: Activity) {
        interstitialAdManager.show(activity)
    }

    override fun onCleared() {
        // NativeAd holds native resources and must be destroyed explicitly.
        nativeAd?.destroy()
        nativeAd = null
    }

    private companion object {
        const val TAG = "AdViewModel"
    }
}
