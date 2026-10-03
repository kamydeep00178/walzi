package com.yunok.walzi.domain.model

data class Category(
    val id: String,
    val name: String,
    val imageUrl: String,
    val position: Int,
    val countryCodes: List<String> = emptyList(),
    /** Small preview image; empty when the admin portal hasn't generated one. */
    val thumbUrl: String = ""
) {
    /** What category tiles load: the light thumbnail if there is one, else the original. */
    val tileImageUrl: String get() = thumbUrl.ifEmpty { imageUrl }
}