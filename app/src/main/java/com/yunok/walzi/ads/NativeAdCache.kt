package com.yunok.walzi.ads

import android.content.Context
import android.os.SystemClock
import android.util.Log
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Collections
import java.util.WeakHashMap

/**
 * App-wide native ad cache.
 *
 * Previously every screen visit fired its own batch of native requests (Home 5, each Category
 * visit 3, Notifications 1), and ads that loaded but were never scrolled into view were
 * destroyed unseen when the screen closed. That inflates requests per user and pulls the
 * match/show rate down. This cache instead:
 *
 *  - keeps up to [CAPACITY] ads ready, so a slot usually fills instantly;
 *  - runs one request at a time, and only while a slot is waiting or the cache is below
 *    capacity;
 *  - takes back ads a screen received but never displayed ([recycle]), so the next screen
 *    reuses them instead of requesting new ones;
 *  - backs off for [FAIL_BACKOFF_MS] after a no-fill instead of re-requesting on every screen;
 *  - drops ads older than [MAX_AGE_MS] (AdMob native ads expire after ~1 hour).
 *
 * Every [NativeAd] handed out is owned by exactly one slot and shown in one place only. Ads that
 * were displayed ([markShown]) are destroyed by their owner, never reused.
 *
 * All state is touched on the main thread only.
 */
object NativeAdCache {

    private const val TAG = "NativeAdCache"

    private const val CAPACITY = 2
    private const val MAX_AGE_MS = 55 * 60_000L
    private const val FAIL_BACKOFF_MS = 60_000L
    private const val WAIT_TIMEOUT_MS = 12_000L

    private class Entry(val ad: NativeAd, val loadedAt: Long)

    private val ready = ArrayDeque<Entry>()
    private val waiters = ArrayDeque<CompletableDeferred<NativeAd?>>()
    private val loadedAt = WeakHashMap<NativeAd, Long>()
    private val shown: MutableSet<NativeAd> = Collections.newSetFromMap(WeakHashMap())
    private var loading = false
    private var lastFailAt = 0L
    private var appContext: Context? = null

    /**
     * Returns an ad for one slot, or null if none could be filled (the slot then stays hidden).
     * Suspends until the Mobile Ads SDK is initialised (i.e. consent allows ad requests).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun obtain(context: Context): NativeAd? = withContext(Dispatchers.Main.immediate) {
        if (!AdsConfig.ADS_ENABLED) return@withContext null
        appContext = context.applicationContext
        AdManager.isInitialized.first { it }

        takeFresh()?.let { ad ->
            loadNext() // top the cache back up for the next slot
            return@withContext ad
        }
        if (inBackoff()) return@withContext null

        val waiter = CompletableDeferred<NativeAd?>()
        waiters.addLast(waiter)
        loadNext()

        var result: NativeAd? = null
        try {
            result = withTimeoutOrNull(WAIT_TIMEOUT_MS) { waiter.await() }
            result
        } finally {
            waiters.remove(waiter)
            // Timed out or the screen closed after an ad was already handed to this waiter:
            // keep that ad for the next slot instead of losing it.
            if (result == null && waiter.isCompleted) {
                waiter.getCompleted()?.let { offer(it) }
            }
        }
    }

    /** Called when [ad] is bound to a visible NativeAdView - it can never be reused after this. */
    fun markShown(ad: NativeAd) {
        shown += ad
    }

    /**
     * Gives back an ad a screen no longer needs. Never-displayed, still-fresh ads go back into
     * the cache for the next slot; everything else is destroyed.
     */
    fun recycle(ad: NativeAd) {
        val born = loadedAt[ad]
        val fresh = born != null && SystemClock.elapsedRealtime() - born <= MAX_AGE_MS
        if (ad in shown || !fresh) {
            shown -= ad
            loadedAt -= ad
            ad.destroy()
            return
        }
        offer(ad)
    }

    private fun takeFresh(): NativeAd? {
        val now = SystemClock.elapsedRealtime()
        while (ready.isNotEmpty()) {
            val entry = ready.removeFirst()
            if (now - entry.loadedAt <= MAX_AGE_MS) return entry.ad
            Log.d(TAG, "Dropping expired native ad")
            loadedAt -= entry.ad
            entry.ad.destroy()
        }
        return null
    }

    private fun inBackoff(): Boolean =
        lastFailAt != 0L && SystemClock.elapsedRealtime() - lastFailAt < FAIL_BACKOFF_MS

    /** Hands [ad] to the first slot still waiting, else keeps it ready (or destroys it if full). */
    private fun offer(ad: NativeAd) {
        while (waiters.isNotEmpty()) {
            if (waiters.removeFirst().complete(ad)) return
        }
        if (ready.size < CAPACITY) {
            ready.addLast(Entry(ad, loadedAt[ad] ?: SystemClock.elapsedRealtime()))
        } else {
            loadedAt -= ad
            ad.destroy()
        }
    }

    private fun loadNext() {
        val context = appContext ?: return
        if (loading) return
        if (waiters.isEmpty() && ready.size >= CAPACITY) return
        if (inBackoff()) return
        loading = true

        AdLoader.Builder(context, AdManager.NATIVE_AD_UNIT_ID)
            .forNativeAd { ad ->
                loading = false
                lastFailAt = 0L
                loadedAt[ad] = SystemClock.elapsedRealtime()
                offer(ad)
                loadNext()
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
                    loading = false
                    lastFailAt = SystemClock.elapsedRealtime()
                    // No fill right now: let every waiting slot hide.
                    while (waiters.isNotEmpty()) waiters.removeFirst().complete(null)
                }
            })
            .build()
            .loadAd(AdRequest.Builder().build())
    }
}
