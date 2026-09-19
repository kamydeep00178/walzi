package com.yunok.walzi.data.model

import com.google.firebase.firestore.PropertyName

data class TagDto(
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("wallpaperCount") @set:PropertyName("wallpaperCount")
    var wallpaperCount: Long = 0,

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = 0
) {
    constructor() : this("", 0, 0)
}