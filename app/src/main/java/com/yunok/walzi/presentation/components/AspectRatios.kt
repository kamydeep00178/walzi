package com.yunok.walzi.presentation.components

/** Varied card heights that give the staggered grids their masonry look. */
val MasonryAspectRatios = listOf(0.55f, 0.62f, 0.5f, 0.68f, 0.58f, 0.72f)

/** Gentler ratios used by the smaller "gallery" style grids (Favourites, List detail). */
val GalleryAspectRatios = listOf(0.75f, 0.85f, 1f, 0.62f, 0.95f, 0.7f)

/** Deterministic per-id pick, so a wallpaper keeps the same card shape everywhere and across scrolls. */
fun aspectRatioFor(id: String, ratios: List<Float> = MasonryAspectRatios): Float =
    ratios[(id.hashCode() and 0x7fffffff) % ratios.size]
