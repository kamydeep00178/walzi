package com.yunok.walzi.presentation.components

import androidx.compose.foundation.lazy.staggeredgrid.LazyStaggeredGridScope
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.runtime.remember
import com.google.android.gms.ads.nativead.NativeAd
import com.yunok.walzi.ads.NativeAdCard
import com.yunok.walzi.ads.NativeAdSlots
import com.yunok.walzi.domain.model.Wallpaper

/**
 * Wallpaper cards with [nativeAds] placed between them (see [NativeAdSlots]: after 8, then
 * every 12). Each ad is the big native card spanning the full row, with an "Ad · Sponsored"
 * header, so it reads as a separate row - never as a wallpaper.
 * With no ads this is exactly a plain list of cards.
 *
 * [style]: animated [AnimatedWallpaperCard]s with that screen's badge, each animating in once
 * ([dealtIds] remembers which already did). null = plain [WallpaperCard]s (e.g. search).
 */
fun LazyStaggeredGridScope.wallpaperCardsWithAds(
    wallpapers: List<Wallpaper>,
    nativeAds: List<NativeAd>,
    onWallpaperClick: (String) -> Unit,
    style: CardStyle? = null,
    dealtIds: MutableSet<String>? = null
) {
    val slots = NativeAdSlots(contentCount = wallpapers.size, adCount = nativeAds.size)
    items(
        count = slots.totalCount,
        key = { pos ->
            val ad = slots.adIndexAt(pos)
            if (ad >= 0) "native_ad_$ad" else wallpapers[slots.contentIndexAt(pos)].id
        },
        contentType = { pos -> if (slots.adIndexAt(pos) >= 0) "native_ad" else "wallpaper" },
        span = { pos ->
            if (slots.adIndexAt(pos) >= 0) StaggeredGridItemSpan.FullLine else StaggeredGridItemSpan.SingleLane
        }
    ) { pos ->
        val ad = slots.adIndexAt(pos)
        if (ad >= 0) {
            NativeAdCard(nativeAd = nativeAds[ad], big = true)
        } else {
            val index = slots.contentIndexAt(pos)
            val wallpaper = wallpapers[index]
            if (style != null) {
                // Entrance animation only the first time this wallpaper appears in this grid.
                val dealIn = remember(wallpaper.id) { dealtIds?.add(wallpaper.id) ?: false }
                AnimatedWallpaperCard(
                    wallpaper = wallpaper,
                    style = style,
                    rank = index + 1,
                    aspectRatio = aspectRatioFor(wallpaper.id),
                    dealIn = dealIn,
                    onClick = { onWallpaperClick(wallpaper.id) }
                )
            } else {
                WallpaperCard(
                    wallpaper = wallpaper,
                    aspectRatio = aspectRatioFor(wallpaper.id),
                    onClick = { onWallpaperClick(wallpaper.id) }
                )
            }
        }
    }
}
