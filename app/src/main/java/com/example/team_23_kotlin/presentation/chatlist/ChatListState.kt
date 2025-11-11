package com.example.team_23_kotlin.presentation.chatlist

data class ChatListState(
    val chats: List<ChatSummary> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)

data class ChatSummary(
    val id: String,
    val listingTitle: String,
    val senderName: String,
    val lastMessage: String,
    val lastTime: String,
    val unreadCount: Int,
    val listingImageUrl: String? = null
)

