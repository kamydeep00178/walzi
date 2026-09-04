package com.yunok.walzi.domain.model

data class Category(
    val id: String,
    val name: String,
    val imageUrl: String,
    val position: Int,
    /** ISO country codes this category is tagged for (e.g. ["IN", "NP"]), or ["GLOBAL"] for
     *  the fallback pseudo-country category. Empty for regular style categories. */
    val countryCodes: List<String> = emptyList()
)