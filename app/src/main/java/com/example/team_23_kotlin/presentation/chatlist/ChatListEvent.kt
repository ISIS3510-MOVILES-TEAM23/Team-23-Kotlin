package com.example.team_23_kotlin.presentation.chatlist

sealed interface ChatListEvent {
    data object Refresh : ChatListEvent
    data class OpenChat(val chatId: String) : ChatListEvent
    data class MarkAsRead(val chatId: String) : ChatListEvent
}
