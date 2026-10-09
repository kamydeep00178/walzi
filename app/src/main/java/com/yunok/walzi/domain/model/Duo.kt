package com.yunok.walzi.domain.model

/** Two wallpapers designed together: [lockImageUrl] for the lock screen, [homeImageUrl] for home. */
data class Duo(
    val id: String,
    val title: String,
    val lockImageUrl: String,
    val lockThumbUrl: String,
    val homeImageUrl: String,
    val homeThumbUrl: String,
    val createdAt: Long
) {
    /** What cards load: the light thumbnail if there is one, else the original. */
    val lockCardUrl: String get() = lockThumbUrl.ifEmpty { lockImageUrl }
    val homeCardUrl: String get() = homeThumbUrl.ifEmpty { homeImageUrl }
}

/** The Duo banner in the Collections tab. Shown only when [isVisible]. */
data class DuoConfig(
    val enabled: Boolean,
    val title: String,
    val subtitle: String,
    val coverUrls: List<String>
) {
    val isVisible: Boolean get() = enabled && coverUrls.isNotEmpty()
}
