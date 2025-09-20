package com.example.team_23_kotlin.presentation.chatlist

data class ChatSummary(
    val id: String,
    val listingTitle: String,     // Título de la publicación
    val senderName: String,       // Quien escribió el último mensaje
    val lastMessage: String,      // Último mensaje
    val unreadCount: Int,         // # mensajes sin leer
    val lastTime: String,         // "2h ago", "5m ago", etc.
    val thumbnailUrl: String? = "https://picsum.photos/200"
)

data class ChatListState(
    val isLoading: Boolean = false,
    val chats: List<ChatSummary> = emptyList(),
    val error: String? = null
)
