package com.yunok.walzi.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Anchored adaptive banner for the bottom of a screen, below the content (never overlapping
 * it). Renders nothing when ads are off, before the SDK is ready (consent), or while the user
 * is ad-free.
 *
 * Policy notes: use only at the bottom of list/grid screens - never on the full-screen preview,
 * splash, dialogs or bottom sheets, and at most one per screen.
 *
 * The slot reserves the banner's exact height up front, so content doesn't jump when the ad
 * arrives. The AdView is paused/resumed with the lifecycle and destroyed when leaving
 * composition - a leaked AdView keeps refreshing and retains the Activity.
 */
@Composable
fun BannerAdComposable(modifier: Modifier = Modifier) {
    if (!AdsConfig.ADS_ENABLED) return

    val sdkReady by AdManager.isInitialized.collectAsStateWithLifecycle()
    val adFree by AdFreeManager.isAdFree.collectAsStateWithLifecycle()
    if (!sdkReady || adFree) return

    val context = LocalContext.current
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val adSize = remember(screenWidthDp) {
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, screenWidthDp)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    var adView by remember { mutableStateOf<AdView?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(adSize.height.dp),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(adSize)
                    adUnitId = AdManager.BANNER_AD_UNIT_ID
                    loadAd(AdRequest.Builder().build())
                    adView = this
                }
            }
        )
    }

    DisposableEffect(adView, lifecycleOwner) {
        val view = adView
        if (view == null) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> view.resume()
                    Lifecycle.Event.ON_PAUSE -> view.pause()
                    else -> Unit
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                view.destroy()
            }
        }
    }
}
