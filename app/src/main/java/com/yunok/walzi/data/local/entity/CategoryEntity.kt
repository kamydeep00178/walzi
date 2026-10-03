package com.yunok.walzi.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yunok.walzi.domain.model.Category

@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val imageUrl: String,
    val position: Int,
    val countryCodes: List<String> = emptyList(),
    @ColumnInfo(defaultValue = "") val thumbUrl: String = ""
)

fun CategoryEntity.toDomain() = Category(
    id = id,
    name = name,
    imageUrl = imageUrl,
    position = position,
    countryCodes = countryCodes,
    thumbUrl = thumbUrl
)

fun Category.toEntity() = CategoryEntity(
    id = id,
    name = name,
    imageUrl = imageUrl,
    position = position,
    countryCodes = countryCodes,
    thumbUrl = thumbUrl
)