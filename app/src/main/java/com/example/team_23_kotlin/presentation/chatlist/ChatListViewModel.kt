// presentation/chatlist/ChatListViewModel.kt
package com.example.team_23_kotlin.presentation.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatListViewModel(
    private val repository: ChatRepository = FakeChatRepository() // cámbialo por tu repo real
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState(isLoading = true))
    val state: StateFlow<ChatListState> = _state

    init { load() }

    fun onEvent(event: ChatListEvent) {
        when (event) {
            ChatListEvent.Refresh -> load()
            is ChatListEvent.OpenChat -> {
                // Navegación se maneja desde la Screen vía callback
            }
            is ChatListEvent.MarkAsRead -> markAsRead(event.chatId)
        }
    }

    private fun load() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            // Simula latencia; elimina en prod
            delay(250)
            val data = repository.getChatSummaries()
            _state.value = ChatListState(isLoading = false, chats = data)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Unknown error")
        }
    }

    private fun markAsRead(chatId: String) = viewModelScope.launch {
        val updated = _state.value.chats.map {
            if (it.id == chatId) it.copy(unreadCount = 0) else it
        }
        _state.value = _state.value.copy(chats = updated)
        repository.markThreadAsRead(chatId) // opcional: no-op en fake
    }
}

/* ===== Repo contract + Fake ===== */

interface ChatRepository {
    suspend fun getChatSummaries(): List<ChatSummary>
    suspend fun markThreadAsRead(chatId: String)
}

class FakeChatRepository : ChatRepository {
    override suspend fun getChatSummaries(): List<ChatSummary> = listOf(
        ChatSummary(
            id = "1",
            listingTitle = "MacBook Pro M2",
            senderName = "Andrea López",
            lastMessage = "¿Sigue disponible?",
            unreadCount = 1,
            lastTime = "2h ago",
            thumbnailUrl = null
        ),
        ChatSummary(
            id = "2",
            listingTitle = "Calculus Textbook",
            senderName = "María García",
            lastMessage = "Sí, podemos vernos mañana",
            unreadCount = 0,
            lastTime = "5h ago",
            thumbnailUrl = null
        )
    )

    override suspend fun markThreadAsRead(chatId: String) { /* no-op */ }
}
