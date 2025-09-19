// presentation/chat/ChatEvent.kt
package com.example.team_23_kotlin.presentation.chat

sealed interface ChatEvent {
    data object Load : ChatEvent
    data object Retry : ChatEvent
    data object MarkThreadAsRead : ChatEvent
    data class OnMessageInputChange(val text: String) : ChatEvent
    data class SendMessage(val text: String) : ChatEvent
    data class LoadOlder(val count: Int = 20) : ChatEvent
}
