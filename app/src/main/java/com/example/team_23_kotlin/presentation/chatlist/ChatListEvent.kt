package com.example.team_23_kotlin.presentation.chatlist

sealed class ChatListEvent {
    object Refresh : ChatListEvent()
    data class MarkAsRead(val chatId: String) : ChatListEvent()
}
