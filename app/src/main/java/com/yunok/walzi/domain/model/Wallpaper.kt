package com.yunok.walzi.domain.model

data class Wallpaper(
    val id: String,
    val title: String,
    val imageUrl: String,
    val categoryId: String,
    val categoryName: String,
    val isPopular: Boolean,
    val priority: Int,
    val resolution: String,
    val sizeLabel: String,
    val createdAt: Long,
    val isFavorite: Boolean = false
)
