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

@Singleton
class CacheMetaDataStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val categoriesLastFetchedAtKey = longPreferencesKey("categories_last_fetched_at")
    private val tagsLastFetchedAtKey = longPreferencesKey("tags_last_fetched_at")

    // ---------- Categories ----------

    suspend fun getCategoriesLastFetchedAt(): Long? =
        context.cacheMetaDataStore.data.first()[categoriesLastFetchedAtKey]

    suspend fun setCategoriesLastFetchedAt(millis: Long) {
        context.cacheMetaDataStore.edit { it[categoriesLastFetchedAtKey] = millis }
    }

    // ---------- Tags ----------

    suspend fun getTagsLastFetchedAt(): Long? =
        context.cacheMetaDataStore.data.first()[tagsLastFetchedAtKey]

    suspend fun setTagsLastFetchedAt(millis: Long) {
        context.cacheMetaDataStore.edit { it[tagsLastFetchedAtKey] = millis }
    }

    // ---------- Wallpaper buckets ----------

    private fun wallpaperBucketKey(bucket: String) = longPreferencesKey("wallpaper_bucket_last_fetched_$bucket")

    suspend fun getWallpaperBucketLastFetchedAt(bucket: String): Long? =
        context.cacheMetaDataStore.data.first()[wallpaperBucketKey(bucket)]

    suspend fun setWallpaperBucketLastFetchedAt(bucket: String, millis: Long) {
        context.cacheMetaDataStore.edit { it[wallpaperBucketKey(bucket)] = millis }
    }
}