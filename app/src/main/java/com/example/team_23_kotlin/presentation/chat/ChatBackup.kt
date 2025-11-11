package com.example.team_23_kotlin.presentation.chat

data class ChatBackup(
    val chatId: String,
    val peerName: String,
    val listingTitle: String?,
    val messages: List<ChatMessage>
)
