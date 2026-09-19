package com.yunok.walzi.domain.model

data class Category(
    val id: String,
    val name: String,
    val imageUrl: String,
    val position: Int,
    val countryCodes: List<String> = emptyList()
)