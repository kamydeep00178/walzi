package com.yunok.walzi.util

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import com.yunok.walzi.domain.model.Wallpaper
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * All custom Firebase Analytics events in one place, so names and parameters stay consistent.
 * Consent is handled by Google Consent Mode (see ConsentManager.applyAnalyticsConsent): these
 * calls are always safe - Analytics itself decides what it may store or send.
 *
 * Events (names <= 40 chars, string values truncated to Firebase's 100-char limit):
 *  - category_click     tile tapped in Collections         category_id, category_name, source
 *  - category_view      category screen opened (once)      category_id, category_name
 *  - wallpaper_open     wallpaper tapped -> full preview   wallpaper_*, category_*, source
 *  - wallpaper_view     each wallpaper shown in preview    wallpaper_*, category_*, source
 *  - wallpaper_download download finished                  wallpaper_*, category_*, success, via_reward
 *  - set_wallpaper      set-wallpaper finished             wallpaper_*, category_*, target, adjusted, success
 *  - wallpaper_undo     Undo tapped after setting          success
 *  - surprise_spin      "Surprise me" spun                 -
 *  - surprise_open      Surprise pick opened               wallpaper_*, category_*
 *  - duo_banner_click   Duo banner tapped in Collections   -
 *  - duo_view           Duo card shown                     duo_id, duo_name
 *  - duo_set            Duo applied                        duo_id, duo_name, target (duo/home/lock), success
 *
 * Mark the parameters you want to report on as custom dimensions in the Firebase console
 * (Analytics -> Custom definitions), otherwise they only appear in DebugView / BigQuery.
 */
@Singleton
class AnalyticsTracker @Inject constructor(
    @ApplicationContext context: Context
) {
    private val analytics = FirebaseAnalytics.getInstance(context)

    fun categoryClick(categoryId: String, categoryName: String, source: String) = log(
        "category_click",
        "category_id" to categoryId,
        "category_name" to categoryName,
        "source" to source
    )

    fun categoryView(categoryId: String, categoryName: String) = log(
        "category_view",
        "category_id" to categoryId,
        "category_name" to categoryName
    )

    /** The wallpaper the user tapped to open the full preview (i.e. the click). */
    fun wallpaperOpen(wallpaper: Wallpaper, source: String) =
        log("wallpaper_open", *wallpaperParams(wallpaper), "source" to source)

    /** Every wallpaper actually shown in the full preview, including ones reached by swiping. */
    fun wallpaperView(wallpaper: Wallpaper, source: String) =
        log("wallpaper_view", *wallpaperParams(wallpaper), "source" to source)

    fun download(wallpaper: Wallpaper, success: Boolean, viaReward: Boolean) = log(
        "wallpaper_download",
        *wallpaperParams(wallpaper),
        "success" to success.asParam(),
        "via_reward" to viaReward.asParam()
    )

    fun setWallpaper(wallpaper: Wallpaper, target: WallpaperTarget, success: Boolean, adjusted: Boolean = false) = log(
        "set_wallpaper",
        *wallpaperParams(wallpaper),
        "adjusted" to adjusted.asParam(),
        "target" to when (target) {
            WallpaperTarget.HOME -> "home"
            WallpaperTarget.LOCK -> "lock"
            WallpaperTarget.BOTH -> "both"
        },
        "success" to success.asParam()
    )

    /** "Surprise me" shuffle started (incl. Shuffle again). */
    fun surpriseSpin() = log("surprise_spin")

    /** The "Surprise me" pick was opened in the full preview. */
    fun surpriseOpen(wallpaper: Wallpaper) = log("surprise_open", *wallpaperParams(wallpaper))

    /** Duo banner tapped in the Collections tab. */
    fun duoBannerClick() = log("duo_banner_click")

    /** A Duo card shown on the Duo screen (once per Duo it settles on). */
    fun duoView(duoId: String, title: String) = log("duo_view", "duo_id" to duoId, "duo_name" to title)

    fun duoSet(duoId: String, title: String, target: WallpaperTarget, success: Boolean) = log(
        "duo_set",
        "duo_id" to duoId,
        "duo_name" to title,
        "target" to when (target) {
            WallpaperTarget.BOTH -> "duo"
            WallpaperTarget.HOME -> "home"
            WallpaperTarget.LOCK -> "lock"
        },
        "success" to success.asParam()
    )

    /** Undo after setting - restored the previous Walzi wallpaper. */
    fun undo(success: Boolean) = log("wallpaper_undo", "success" to success.asParam())

    private fun wallpaperParams(wallpaper: Wallpaper): Array<Pair<String, Any>> = arrayOf(
        "wallpaper_id" to wallpaper.id,
        "wallpaper_name" to wallpaper.title,
        "category_id" to wallpaper.categoryId,
        "category_name" to wallpaper.categoryName
    )

    /** Booleans as 1/0 numbers - Firebase has no boolean parameter type. */
    private fun Boolean.asParam(): Long = if (this) 1L else 0L

    private fun log(event: String, vararg params: Pair<String, Any>) {
        val bundle = Bundle()
        params.forEach { (key, value) ->
            when (value) {
                is Long -> bundle.putLong(key, value)
                else -> bundle.putString(key, value.toString().take(MAX_STRING_VALUE))
            }
        }
        analytics.logEvent(event, bundle)
    }

    private companion object {
        const val MAX_STRING_VALUE = 100
    }
}
