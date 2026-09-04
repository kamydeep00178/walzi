package com.yunok.walzi.data.model

import com.google.firebase.firestore.PropertyName

/**
 * Mirrors the `categories/{id}` document written by the Walzi admin portal.
 * Field names must match exactly (Firestore maps by property name).
 */
data class CategoryDto(
    @get:PropertyName("name") @set:PropertyName("name")
    var name: String = "",

    @get:PropertyName("imageUrl") @set:PropertyName("imageUrl")
    var imageUrl: String = "",

    @get:PropertyName("position") @set:PropertyName("position")
    var position: Long = 0,

    @get:PropertyName("countryCodes") @set:PropertyName("countryCodes")
    var countryCodes: List<String>? = null,

    @get:PropertyName("createdAt") @set:PropertyName("createdAt")
    var createdAt: Long = 0
) {
    // Firestore requires a no-arg constructor for reflection-based deserialization.
    constructor() : this("", "", 0, null, 0)
}