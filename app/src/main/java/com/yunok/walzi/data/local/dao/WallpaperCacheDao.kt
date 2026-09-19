package com.yunok.walzi.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.yunok.walzi.data.local.entity.WallpaperCacheEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WallpaperCacheDao {

    @Query("SELECT * FROM wallpaper_cache WHERE bucket = :bucket ORDER BY priority DESC, createdAt DESC")
    fun observeByBucket(bucket: String): Flow<List<WallpaperCacheEntity>>

    @Query("SELECT COUNT(*) FROM wallpaper_cache WHERE bucket = :bucket")
    suspend fun count(bucket: String): Int

    @Query("DELETE FROM wallpaper_cache WHERE bucket = :bucket")
    suspend fun clearBucket(bucket: String)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(wallpapers: List<WallpaperCacheEntity>)

    @Transaction
    suspend fun replaceBucket(bucket: String, wallpapers: List<WallpaperCacheEntity>) {
        clearBucket(bucket)
        insertAll(wallpapers)
    }
}