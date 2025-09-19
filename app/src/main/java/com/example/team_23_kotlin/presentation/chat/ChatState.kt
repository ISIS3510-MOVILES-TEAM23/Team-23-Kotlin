// presentation/chat/ChatState.kt
package com.example.team_23_kotlin.presentation.chat

data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean,
    val senderName: String,
    val senderAvatarUrl: String? = null
)

data class ChatHeader(
    val chatId: String,
    val peerName: String,
    val peerAvatarUrl: String? = null,
    val listingTitle: String? = null
)

data class ChatState(
    val header: ChatHeader,
    val isLoading: Boolean = false,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList(), // <- tipo explícito
    val input: String = "",
    val canSend: Boolean = false
)
