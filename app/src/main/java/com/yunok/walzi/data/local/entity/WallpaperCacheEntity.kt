package com.yunok.walzi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import com.yunok.walzi.domain.model.Wallpaper

@Entity(tableName = "wallpaper_cache", primaryKeys = ["id", "bucket"])
data class WallpaperCacheEntity(
    val id: String,
    val bucket: String,
    /** Index within the fetched page, i.e. the exact order Firestore returned. The cache must be
     *  read back in this order so "load more" cursors derived from the last cached item are right. */
    @ColumnInfo(defaultValue = "0") val position: Int = 0,
    val title: String,
    val imageUrl: String,
    val categoryId: String,
    val categoryName: String,
    val isPopular: Boolean,
    val priority: Long,
    val resolution: String,
    val sizeLabel: String,
    val createdAt: Long,
    val tags: List<String> = emptyList()
)

fun WallpaperCacheEntity.toDomain(isFavorite: Boolean) = Wallpaper(
    id = id,
    title = title,
    imageUrl = imageUrl,
    categoryId = categoryId,
    categoryName = categoryName,
    isPopular = isPopular,
    priority = priority.toInt(),
    resolution = resolution,
    sizeLabel = sizeLabel,
    createdAt = createdAt,
    isFavorite = isFavorite,
    tags = tags
)