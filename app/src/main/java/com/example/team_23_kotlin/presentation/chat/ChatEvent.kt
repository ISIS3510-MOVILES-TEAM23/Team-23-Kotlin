package com.example.team_23_kotlin.presentation.chat

sealed class ChatEvent {
    data class OnMessageInputChange(val input: String) : ChatEvent()
    data class SendMessage(val message: String) : ChatEvent()

    // 🔹 Nuevo evento para cargar un chat específico
    data class LoadChat(val chatId: String) : ChatEvent()

    object LoadMessages : ChatEvent()
    object ShowPurchaseButton : ChatEvent()
    object HidePurchaseButton : ChatEvent()
}
