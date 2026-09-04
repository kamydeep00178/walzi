package com.yunok.walzi.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.yunok.walzi.data.local.dao.CategoryDao
import com.yunok.walzi.data.local.dao.WallpaperCacheDao
import com.yunok.walzi.data.local.entity.CategoryEntity
import com.yunok.walzi.data.local.entity.WallpaperCacheEntity

@Database(
    entities = [CategoryEntity::class, WallpaperCacheEntity::class],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class WalziDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun wallpaperCacheDao(): WallpaperCacheDao
}