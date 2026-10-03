package com.yunok.walzi.ads

import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.doOnAttach
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdView
import com.yunok.walzi.R

/** Height limits for the big card's media, so very tall or very wide creatives stay reasonable. */
private const val MEDIA_MIN_HEIGHT_DP = 120
private const val MEDIA_MAX_HEIGHT_DP = 280

/**
 * Native ad card for feeds and lists.
 *
 * - Small ([big] = false, native_ad_item.xml): one row - icon, "Ad" label, headline, body, CTA.
 *   Used where a large ad would dominate (Collections, Notifications).
 * - Big ([big] = true, native_ad_big.xml): "Ad · Sponsored" header, large image/video in a
 *   MediaView, then icon + headline + CTA. Used in the wallpaper feeds.
 *
 * Policy notes: both layouts always show an "Ad" label, the required AdChoices icon has its own
 * top-right slot, the large media is always a MediaView (never an ImageView), and the card is a
 * distinct full row - it never imitates a wallpaper card, so it can't be mistaken for content.
 *
 * Renders nothing while [nativeAd] is null. The ad's lifetime is owned by [AdViewModel], which
 * destroys it in onCleared(). Each NativeAd must be shown in one slot only.
 */
@Composable
fun NativeAdCard(nativeAd: NativeAd?, modifier: Modifier = Modifier, big: Boolean = false) {
    if (!AdsConfig.ADS_ENABLED || nativeAd == null) return

    AndroidView(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        factory = { context ->
            val layout = if (big) R.layout.native_ad_big else R.layout.native_ad_item
            val adView = LayoutInflater.from(context).inflate(layout, null, false) as NativeAdView
            adView.headlineView = adView.findViewById(R.id.ad_headline)
            adView.bodyView = adView.findViewById(R.id.ad_body)
            adView.iconView = adView.findViewById(R.id.ad_app_icon)
            adView.callToActionView = adView.findViewById(R.id.ad_call_to_action)
            // Only the big layout has a media slot.
            adView.findViewById<MediaView?>(R.id.ad_media)?.let { media ->
                media.setImageScaleType(ImageView.ScaleType.CENTER_CROP)
                adView.mediaView = media
            }
            // Explicit AdChoices slot (top-right) - the SDK fills it with the required icon.
            adView.adChoicesView = adView.findViewById(R.id.ad_choices)
            adView
        },
        update = { adView ->
            (adView.headlineView as? TextView)?.text = nativeAd.headline

            val body = adView.bodyView as? TextView
            body?.text = nativeAd.body
            body?.visibility = if (nativeAd.body.isNullOrEmpty()) View.GONE else View.VISIBLE

            val cta = adView.callToActionView as? TextView
            cta?.text = nativeAd.callToAction
            cta?.visibility = if (nativeAd.callToAction.isNullOrEmpty()) View.GONE else View.VISIBLE

            val iconView = adView.iconView as? ImageView
            val icon = nativeAd.icon?.drawable
            if (icon != null) {
                iconView?.setImageDrawable(icon)
                iconView?.visibility = View.VISIBLE
            } else {
                iconView?.visibility = View.GONE
            }

            adView.mediaView?.let { media ->
                val content = nativeAd.mediaContent
                media.mediaContent = content
                // Size the media to its own aspect ratio (full card width), within limits.
                val ratio = content?.aspectRatio ?: 0f
                if (ratio > 0f) {
                    val metrics = adView.resources.displayMetrics
                    val cardWidthPx = metrics.widthPixels - (32 * metrics.density) // grid padding
                    val heightPx = (cardWidthPx / ratio).coerceIn(
                        MEDIA_MIN_HEIGHT_DP * metrics.density,
                        MEDIA_MAX_HEIGHT_DP * metrics.density
                    )
                    media.layoutParams = media.layoutParams.apply { height = heightPx.toInt() }
                }
            }

            // Register only once the view is attached: registering a detached NativeAdView can
            // leave the required AdChoices icon (and impression tracking) unrendered.
            if (adView.isAttachedToWindow) adView.setNativeAd(nativeAd)
            else adView.doOnAttach { (it as NativeAdView).setNativeAd(nativeAd) }
        }
    )
}
