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
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    /** Small preview image; empty when the backend hasn't generated one. */
    val thumbUrl: String = ""
) {
    /** What grids / carousels / previews load: the light thumbnail if there is one, else the original.
     *  Full preview, Set Wallpaper and Download always use the original [imageUrl]. */
    val gridImageUrl: String get() = thumbUrl.ifEmpty { imageUrl }
}