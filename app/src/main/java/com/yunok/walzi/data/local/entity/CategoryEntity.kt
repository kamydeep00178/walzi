package com.yunok.walzi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yunok.walzi.domain.model.Category

/** Room-cached mirror of a `categories/{id}` Firestore document. */
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String,
    val position: Int,
    val countryCodes: List<String> = emptyList()
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    imageUrl = imageUrl,
    position = position,
    countryCodes = countryCodes
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    imageUrl = imageUrl,
    position = position,
    countryCodes = countryCodes
)