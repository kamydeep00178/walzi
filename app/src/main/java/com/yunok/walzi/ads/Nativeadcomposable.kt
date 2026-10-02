package com.yunok.walzi.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.yunok.walzi.R
import com.yunok.walzi.presentation.theme.Surface

/**
 * Native ad card for the feed grid.
 *
 * Renders nothing while [nativeAd] is null (still loading / failed / ads disabled) - no
 * placeholder, so a failed ad never leaves an empty box in the grid. The ad's lifetime is owned
 * by [AdViewModel], which destroys it in onCleared().
 */
@Composable
fun NativeAdCard(nativeAd: NativeAd?, modifier: Modifier = Modifier) {
    if (!AdsConfig.ADS_ENABLED || nativeAd == null) return

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Surface),
        factory = { context ->
            val adView = LayoutInflater.from(context)
                .inflate(R.layout.native_ad_item, null, false) as NativeAdView
            adView.headlineView = adView.findViewById(R.id.ad_headline)
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.iconView = adView.findViewById(R.id.ad_app_icon)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            adView
        },
        update = { adView ->
            (adView.headlineView as? TextView)?.text = nativeAd.headline
            (adView.bodyView as? TextView)?.text = nativeAd.body
            (adView.callToActionView as? TextView)?.text = nativeAd.callToAction

            val iconView = adView.iconView as? ImageView
            val icon = nativeAd.icon?.drawable
            if (icon != null) {
                iconView?.setImageDrawable(icon)
                iconView?.visibility = View.VISIBLE
            } else {
                iconView?.visibility = View.GONE
            }

            adView.setNativeAd(nativeAd)
        }
    )
}
