package com.yunok.walzi.data.local.entity

import androidx.room.Entity
import com.yunok.walzi.domain.model.Wallpaper

/**
 * Room-cached mirror of a `wallpapers/{id}` Firestore document, tagged with which cache
 * "bucket" it belongs to ("recent", "popular", or "category:<id>"). The same wallpaper can
 * legitimately appear in more than one bucket at once (e.g. both Recent and Popular), so the
 * primary key is (id, bucket) rather than id alone.
 */
@Entity(tableName = "wallpaper_cache", primaryKeys = ["id", "bucket"])
data class WallpaperCacheEntity(
    val id: String,
    val bucket: String,
    val title: String,
    val imageUrl: String,
    val categoryId: String,
    val categoryName: String,
    val isPopular: Boolean,
    val priority: Long,
    val resolution: String,
    val sizeLabel: String,
    val createdAt: Long
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
    isFavorite = isFavorite
)