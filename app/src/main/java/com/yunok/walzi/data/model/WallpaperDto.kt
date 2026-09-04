package com.yunok.walzi.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Mirrors the `wallpapers/{id}` document written by the Walzi admin portal.
 */
data class WallpaperDto(
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    @get:PropertyName("categoryId") @set:PropertyName("categoryId")
    var categoryId: String = "",

    @get:PropertyName("categoryName") @set:PropertyName("categoryName")
    var categoryName: String = "",

    @get:PropertyName("isPopular") @set:PropertyName("isPopular")
    var isPopular: Boolean = false,

    @get:PropertyName("priority") @set:PropertyName("priority")
    var priority: Long = 0,

    @get:PropertyName("resolution") @set:PropertyName("resolution")
    var resolution: String = "",

    @get:PropertyName("sizeLabel") @set:PropertyName("sizeLabel")
    var sizeLabel: String = "",

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = 0
) {
    constructor() : this("", "", "", "", false, 0, "", "", 0)
}
