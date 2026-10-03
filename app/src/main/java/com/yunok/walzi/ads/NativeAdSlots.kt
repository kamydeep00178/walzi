package com.yunok.walzi.ads

/** First native ad goes after this many content items... */
const val NATIVE_AD_FIRST_AFTER = 8

/** ...then one more after every this many content items. */
const val NATIVE_AD_EVERY = 12

/** Ads requested per feed screen. Each loaded ad fills exactly one slot (never shown twice). */
const val NATIVE_ADS_PER_FEED = 3

/**
 * Places native ads among [contentCount] list items: after [firstAfter] items, then every
 * [every] items, up to [adCount] ads, and only where real content follows (never a trailing ad).
 * Positions are in the combined list (content + ads).
 */
class NativeAdSlots(
    contentCount: Int,
    adCount: Int,
    firstAfter: Int = NATIVE_AD_FIRST_AFTER,
    every: Int = NATIVE_AD_EVERY
) {
    private val adPositions: IntArray = buildList {
        for (k in 0 until adCount) {
            val contentBefore = firstAfter + k * every
            if (contentBefore >= contentCount) break
            add(contentBefore + k)
        }
    }.toIntArray()

    val totalCount: Int = contentCount + adPositions.size

    /** Which ad (index into the ad list) sits at [position], or -1 for a content item. */
    fun adIndexAt(position: Int): Int = adPositions.indexOf(position)

    /** Content index at [position]; only valid when [adIndexAt] is -1. */
    fun contentIndexAt(position: Int): Int = position - adPositions.count { it < position }
}
