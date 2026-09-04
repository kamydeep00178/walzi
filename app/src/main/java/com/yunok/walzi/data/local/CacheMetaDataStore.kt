package com.yunok.walzi.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.cacheMetaDataStore by preferencesDataStore(name = "walzi_cache_meta")

/** Tracks "when did we last successfully fetch X from the network" timestamps - one key for
 *  categories, and one dynamically-keyed entry per wallpaper cache bucket - so each has its
 *  own independent TTL check. */
@Singleton
class CacheMetaDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val CATEGORIES_LAST_FETCHED_AT = longPreferencesKey("categories_last_fetched_at")
        fun wallpaperBucketLastFetchedAt(bucket: String) = longPreferencesKey("wallpapers_last_fetched_$bucket")
    }

    suspend fun getCategoriesLastFetchedAt(): Long? =
        context.cacheMetaDataStore.data.first()[Keys.CATEGORIES_LAST_FETCHED_AT]

    suspend fun setCategoriesLastFetchedAt(timestamp: Long) {
        context.cacheMetaDataStore.edit { prefs -> prefs[Keys.CATEGORIES_LAST_FETCHED_AT] = timestamp }
    }

    suspend fun getWallpaperBucketLastFetchedAt(bucket: String): Long? =
        context.cacheMetaDataStore.data.first()[Keys.wallpaperBucketLastFetchedAt(bucket)]

    suspend fun setWallpaperBucketLastFetchedAt(bucket: String, timestamp: Long) {
        context.cacheMetaDataStore.edit { prefs -> prefs[Keys.wallpaperBucketLastFetchedAt(bucket)] = timestamp }
    }
}