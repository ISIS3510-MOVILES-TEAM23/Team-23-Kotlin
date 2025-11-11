package com.example.team_23_kotlin.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chats")
data class ChatEntity(
    @PrimaryKey val id: String,
    val listingTitle: String,
    val senderName: String,
    val lastMessage: String,
    val lastTime: String,
    val unreadCount: Int,
    val listingImageUrl: String? = null // ✅
)
