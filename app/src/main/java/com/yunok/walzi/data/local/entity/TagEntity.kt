package com.yunok.walzi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.yunok.walzi.domain.model.Tag

@Entity(tableName = "tags")
data class TagEntity(
    @PrimaryKey val name: String,
    val wallpaperCount: Int
)

fun TagEntity.toDomain() = Tag(name = name, wallpaperCount = wallpaperCount)

fun Tag.toEntity() = TagEntity(name = name, wallpaperCount = wallpaperCount)