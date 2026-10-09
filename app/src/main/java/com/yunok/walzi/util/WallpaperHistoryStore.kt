package com.yunok.walzi.util

import android.content.Context
import com.yunok.walzi.domain.model.Wallpaper
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import javax.inject.Inject
import javax.inject.Singleton

/** One wallpaper Walzi applied - enough to show it in History and to apply it again exactly. */
data class AppliedWallpaper(
    val wallpaperId: String,
    val title: String,
    val imageUrl: String,
    val thumbUrl: String,
    val categoryName: String,
    val target: WallpaperTarget,
    val adjustments: WallpaperAdjustments,
    val appliedAt: Long,
    /** Set for a Duo applied to both screens: [imageUrl] went to home, this to the lock screen. */
    val lockImageUrl: String? = null
) {
    val isDuo: Boolean get() = lockImageUrl != null
}

/**
 * The last [MAX_ENTRIES] wallpapers Walzi applied (manually, by daily auto-rotate or by the
 * widget), newest first. Small, so plain JSON in SharedPreferences is enough.
 *
 * Android doesn't let apps read the phone's previous system wallpaper, so this history is also
 * what Undo restores from.
 */
@Singleton
class WallpaperHistoryStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _entries = MutableStateFlow(load())
    val entries: StateFlow<List<AppliedWallpaper>> = _entries.asStateFlow()

    @Synchronized
    fun add(wallpaper: Wallpaper, target: WallpaperTarget, adjustments: WallpaperAdjustments) {
        val entry = AppliedWallpaper(
            wallpaperId = wallpaper.id,
            title = wallpaper.title,
            imageUrl = wallpaper.imageUrl,
            thumbUrl = wallpaper.gridImageUrl,
            categoryName = wallpaper.categoryName,
            target = target,
            adjustments = adjustments,
            appliedAt = System.currentTimeMillis()
        )
        add(entry)
    }

    @Synchronized
    fun add(entry: AppliedWallpaper) {
        save((listOf(entry) + _entries.value).take(MAX_ENTRIES))
    }

    /** Re-adds an existing entry at the top with a fresh timestamp ("Apply again"). */
    @Synchronized
    fun addAgain(entry: AppliedWallpaper) {
        save((listOf(entry.copy(appliedAt = System.currentTimeMillis())) + _entries.value).take(MAX_ENTRIES))
    }

    /** Removes the newest entry (after an Undo) and returns it, or null if empty. */
    @Synchronized
    fun removeLatest(): AppliedWallpaper? {
        val current = _entries.value
        if (current.isEmpty()) return null
        save(current.drop(1))
        return current.first()
    }

    @Synchronized
    fun clear() = save(emptyList())

    private fun save(list: List<AppliedWallpaper>) {
        _entries.value = list
        val json = JSONArray()
        list.forEach { e ->
            json.put(
                JSONObject()
                    .put("id", e.wallpaperId)
                    .put("title", e.title)
                    .put("imageUrl", e.imageUrl)
                    .put("thumbUrl", e.thumbUrl)
                    .put("category", e.categoryName)
                    .put("target", e.target.name)
                    .put("dim", e.adjustments.dim.toDouble())
                    .put("blur", e.adjustments.blur.toDouble())
                    .put("brightness", e.adjustments.brightness.toDouble())
                    .put("position", e.adjustments.position.toDouble())
                    .put("at", e.appliedAt)
                    .apply { e.lockImageUrl?.let { put("lockImageUrl", it) } }
            )
        }
        prefs.edit().putString(KEY_ENTRIES, json.toString()).apply()
    }

    private fun load(): List<AppliedWallpaper> = try {
        val json = JSONArray(prefs.getString(KEY_ENTRIES, "[]"))
        (0 until json.length()).mapNotNull { i ->
            val o = json.getJSONObject(i)
            val target = runCatching { WallpaperTarget.valueOf(o.getString("target")) }.getOrNull()
                ?: return@mapNotNull null
            AppliedWallpaper(
                wallpaperId = o.getString("id"),
                title = o.optString("title"),
                imageUrl = o.getString("imageUrl"),
                thumbUrl = o.optString("thumbUrl"),
                categoryName = o.optString("category"),
                target = target,
                adjustments = WallpaperAdjustments(
                    dim = o.optDouble("dim", 0.0).toFloat(),
                    blur = o.optDouble("blur", 0.0).toFloat(),
                    brightness = o.optDouble("brightness", 0.0).toFloat(),
                    position = o.optDouble("position", 0.0).toFloat()
                ),
                appliedAt = o.optLong("at"),
                lockImageUrl = o.optString("lockImageUrl").ifEmpty { null }
            )
        }
    } catch (_: Exception) {
        emptyList() // corrupt / old format: start fresh rather than crash
    }

    private companion object {
        const val PREFS_NAME = "wallpaper_history"
        const val KEY_ENTRIES = "entries"
        const val MAX_ENTRIES = 30
    }
}
