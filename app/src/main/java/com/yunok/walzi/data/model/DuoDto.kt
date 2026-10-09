package com.yunok.walzi.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Mirrors `duos/{id}` written by the admin portal: two wallpapers designed together.
 * Field names must match exactly (Firestore maps by property name).
 */
data class DuoDto(
    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("lockImageUrl") @set:PropertyName("lockImageUrl")
    var lockImageUrl: String = "",

    @get:PropertyName("lockThumbUrl") @set:PropertyName("lockThumbUrl")
    var lockThumbUrl: String = "",

    @get:PropertyName("homeImageUrl") @set:PropertyName("homeImageUrl")
    var homeImageUrl: String = "",

    @get:PropertyName("homeThumbUrl") @set:PropertyName("homeThumbUrl")
    var homeThumbUrl: String = "",

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = 0
)

/** Mirrors `app_config/duo`: the Duo banner in the Collections tab. */
data class DuoConfigDto(
    @get:PropertyName("enabled") @set:PropertyName("enabled")
    var enabled: Boolean = false,

    @get:PropertyName("title") @set:PropertyName("title")
    var title: String = "",

    @get:PropertyName("subtitle") @set:PropertyName("subtitle")
    var subtitle: String = "",

    @get:PropertyName("coverUrls") @set:PropertyName("coverUrls")
    var coverUrls: List<String>? = null
)
