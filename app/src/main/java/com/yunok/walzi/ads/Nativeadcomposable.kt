package com.yunok.walzi.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.yunok.walzi.R

/**
 * Native ad card inserted every 7 rows in HomeTab's LazyColumn.
 *
 * - Shows nothing while loading (no blank space flash)
 * - Fades in once the ad is ready
 * - Properly destroys the NativeAd on composition exit to prevent memory leaks
 * - Hidden entirely for premium users (caller's responsibility)
 */
@Composable
fun NativeAdCard(nativeAdViewModel : AdViewModel?, modifier: Modifier = Modifier, isPremium: Boolean = true) {
    val context = LocalContext.current
    val nativeAd = nativeAdViewModel?.nativeAd
   /* // Holds the loaded NativeAd — null = not ready yet
    var nativeAd by remember { mutableStateOf<NativeAd?>(null) }

    // Cleanup: destroy NativeAd when this composable leaves composition
    DisposableEffect(Unit) {
        onDispose {
            nativeAd?.destroy()
            nativeAd = null
        }
    }

    // Load the ad once, outside AndroidView factory so it survives recomposition
    LaunchedEffect(Unit) {
        AdLoader.Builder(context, AdManager.NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                // Destroy previous ad if somehow reloaded
                nativeAd?.destroy()
                nativeAd = ad
            }
            .withAdListener(object : AdListener() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    // Ad failed — keep nativeAd = null so nothing is shown
                }
            })
            .withNativeAdOptions(NativeAdOptions.Builder().build())
            .build()
            .loadAd(AdRequest.Builder().build())
    }*/
    if (!isPremium) {
        Box(modifier = modifier.fillMaxSize()) {
            if (nativeAd == null) {
                // ✅ SHOW PLACEHOLDER WHILE LOADING
                NativeAdPlaceholder()
            }
            // Only show the card once the ad is actually ready — no blank flicker
            AnimatedVisibility(
                visible = nativeAd != null,
                enter = fadeIn()
            ) {
                // Shimmer placeholder height while binding (near-instant)
                AndroidView(
                    modifier = modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    factory = { ctx ->
                        LayoutInflater.from(ctx)
                            .inflate(R.layout.native_ad_item, null, false) as NativeAdView
                    },
                    update = { adView ->
                        val ad : NativeAd = nativeAd ?: return@AndroidView

                        // Wire up NativeAdView child references
                        adView.headlineView = adView.findViewById(R.id.ad_headline)
                        adView.bodyView = adView.findViewById(R.id.ad_body)
                        adView.iconView = adView.findViewById(R.id.ad_app_icon)
                        adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

                        (adView.headlineView as TextView).text = ad.headline
                        (adView.bodyView as TextView).text = ad.body
                        (adView.callToActionView as TextView).text = ad.callToAction

                        val iconView = adView.iconView as ImageView
                        if (ad.icon?.drawable != null) {
                            iconView.setImageDrawable(ad.icon!!.drawable)
                            iconView.visibility = View.VISIBLE
                        } else {
                            iconView.visibility = View.GONE
                        }

                        adView.setNativeAd(ad)
                    }
                )
            }
        }
    } // Only show the card once the ad is actually ready — no blank flicker
    AnimatedVisibility(
        visible = nativeAd != null,
        enter   = fadeIn()
    ) {
        // Shimmer placeholder height while binding (near-instant)
        AndroidView(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            factory = { ctx ->
                LayoutInflater.from(ctx)
                    .inflate(R.layout.native_ad_item, null, false) as NativeAdView
            },
            update = { adView ->
                val ad = nativeAd ?: return@AndroidView

                // Wire up NativeAdView child references
                adView.headlineView     = adView.findViewById(R.id.ad_headline)
                adView.bodyView         = adView.findViewById(R.id.ad_body)
                adView.iconView         = adView.findViewById(R.id.ad_app_icon)
                adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)

                (adView.headlineView as TextView).text = ad.headline
                (adView.bodyView as TextView).text = ad.body
                (adView.callToActionView as TextView).text = ad.callToAction

                val iconView = adView.iconView as ImageView
                if (ad.icon?.drawable != null) {
                    iconView.setImageDrawable(ad.icon!!.drawable)
                    iconView.visibility = View.VISIBLE
                } else {
                    iconView.visibility = View.GONE
                }

                adView.setNativeAd(ad)
            }
        )
    }
}

@Composable
fun NativeAdPlaceholder() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {

        Row(verticalAlignment = Alignment.CenterVertically) {

            // App Icon placeholder
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(Color.Gray.copy(alpha = 0.2f), shape = CircleShape)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {

                // Title line
                Box(
                    modifier = Modifier
                        .height(12.dp)
                        .fillMaxWidth(0.7f)
                        .background(Color.Gray.copy(alpha = 0.2f))
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Subtitle line
                Box(
                    modifier = Modifier
                        .height(10.dp)
                        .fillMaxWidth(0.5f)
                        .background(Color.Gray.copy(alpha = 0.15f))
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // CTA button placeholder
            Box(
                modifier = Modifier
                    .height(32.dp)
                    .width(70.dp)
                    .background(Color.Gray.copy(alpha = 0.25f), shape = RoundedCornerShape(16.dp))
            )
        }
    }
}