package com.yunok.walzi.domain.model

/**
 * Explicit pagination cursor (the sort-key values of the last item on a page), replacing an
 * opaque Firestore DocumentSnapshot. Being explicit like this means a cursor can be derived
 * from a Room-cached item just as easily as a freshly-fetched one - which is what lets cached
 * "first screen" data and live "load more" pagination continue seamlessly from each other.
 *
 * [id] is the document-id tie-breaker: many wallpapers share the same priority (and sometimes
 * the same createdAt), and Firestore's startAfter() skips *every* document equal to the cursor
 * values, so without it items sitting on a page boundary would silently disappear.
 */
data class WallpaperCursor(val priority: Long, val createdAt: Long, val id: String)

fun Wallpaper.toCursor() = WallpaperCursor(priority = priority.toLong(), createdAt = createdAt, id = id)
