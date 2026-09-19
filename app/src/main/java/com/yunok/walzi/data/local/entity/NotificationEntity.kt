package com.yunok.walzi.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "notifications")
data class NotificationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val body: String,
    val imageUrl: String?,
    val screen: String,
    val wallpaperId: String?,
    val categoryId: String?,
    val receivedAt: Long,
    val isRead: Boolean = false
)