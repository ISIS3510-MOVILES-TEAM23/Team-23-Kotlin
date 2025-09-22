// presentation/chat/ChatEvent.kt
package com.example.team_23_kotlin.presentation.chat

sealed class ChatEvent {
    data class OnMessageInputChange(val input: String) : ChatEvent()
    data class SendMessage(val message: String) : ChatEvent()
    object ShowPurchaseButton : ChatEvent()
    object HidePurchaseButton : ChatEvent()
    object LoadMessages : ChatEvent()
}