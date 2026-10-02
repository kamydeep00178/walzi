package com.yunok.walzi.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

/**
 * Banner ad. Renders nothing when [AdsConfig.ADS_ENABLED] is false, and waits for the SDK to
 * finish initialising (consent) before requesting.
 *
 * The AdView is paused/resumed with the lifecycle and destroyed when leaving composition -
 * a leaked AdView keeps refreshing and retains the Activity.
 */
@Composable
fun BannerAdComposable(modifier: Modifier = Modifier) {
    if (!AdsConfig.ADS_ENABLED) return

    val sdkReady by AdManager.isInitialized.collectAsStateWithLifecycle()
    if (!sdkReady) return

    val lifecycleOwner = LocalLifecycleOwner.current
    var adView by remember { mutableStateOf<AdView?>(null) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
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
