package com.example.team_23_kotlin.presentation.chat

enum class MessageStatus { SENDING, SENT }

data class ChatMessage(
    val id: String,
    val text: String,
    val timestamp: Long,
    val isMine: Boolean,
    val senderName: String,
    val senderAvatarUrl: String? = null,
    val deliveryStatus: MessageStatus = MessageStatus.SENT
)


data class ChatHeader(
    val chatId: String = "",
    val peerName: String = "",
    val peerAvatarUrl: String? = null,
    val listingTitle: String? = null
)

data class ChatState(
    val header: ChatHeader = ChatHeader(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val messages: List<ChatMessage> = emptyList(),
    val input: String = "",
    val canSend: Boolean = false,
    val showPurchaseButton: Boolean = true
)
