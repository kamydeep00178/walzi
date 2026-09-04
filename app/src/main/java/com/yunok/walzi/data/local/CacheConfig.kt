package com.yunok.walzi.data.local

/**
 * Tune how long Room-cached data is trusted before a background refresh from Firestore is
 * triggered, per cache type.
 *
 *   > 0  -> refresh once this many days have passed since the last successful fetch.
 *   0    -> never age out automatically. Only fetches once, on first run (empty cache) or if
 *           the local cache is ever cleared (app data cleared / reinstalled).
 *
 * Note: this only governs the cached *first page* of each bucket (recent/popular/category).
 * Scrolling further ("load more") always hits Firestore live, same as before - caching the
 * first screen is what makes navigating back to a tab feel instant; deep pagination isn't
 * cached, since caching arbitrarily deep pages has rapidly diminishing value.
 */
object CacheConfig {
    const val CATEGORY_REFRESH_INTERVAL_DAYS: Long = 1
    const val WALLPAPER_REFRESH_INTERVAL_DAYS: Long = 1
}