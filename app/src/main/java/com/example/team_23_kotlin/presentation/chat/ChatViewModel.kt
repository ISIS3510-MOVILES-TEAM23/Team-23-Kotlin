// presentation/chat/ChatViewModel.kt
package com.example.team_23_kotlin.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ChatViewModel(
    private val chatId: String,
    private val repository: ChatThreadRepository = FakeChatThreadRepository()
) : ViewModel() {

    private val _state = MutableStateFlow(
        ChatState(
            header = ChatHeader(
                chatId = chatId,
                peerName = "Sofía",
                peerAvatarUrl = null,
                listingTitle = "iPhone 13"
            ),
            isLoading = true
        )
    )
    val state: StateFlow<ChatState> = _state

    init { onEvent(ChatEvent.Load) }

    fun onEvent(event: ChatEvent) {
        when (event) {
            ChatEvent.Load, ChatEvent.Retry -> loadThread()
            ChatEvent.MarkThreadAsRead -> markAsRead()
            is ChatEvent.OnMessageInputChange -> {
                _state.value = _state.value.copy(
                    input = event.text,
                    canSend = event.text.isNotBlank()
                )
            }
            is ChatEvent.SendMessage -> send(event.text)
            is ChatEvent.LoadOlder -> loadOlder(event.count)
        }
    }

    private fun loadThread() = viewModelScope.launch {
        _state.value = _state.value.copy(isLoading = true, error = null)
        try {
            delay(200) // simula latencia
            val msgs = repository.getRecent(chatId)
            _state.value = _state.value.copy(isLoading = false, messages = msgs)
        } catch (e: Exception) {
            _state.value = _state.value.copy(isLoading = false, error = e.message ?: "Unknown error")
        }
    }

    private fun loadOlder(count: Int) = viewModelScope.launch {
        val older = repository.getOlder(chatId, count, oldestTimestamp())
        _state.value = _state.value.copy(messages = older + _state.value.messages)
    }

    private fun oldestTimestamp(): Long =
        _state.value.messages.minOfOrNull { it.timestamp } ?: System.currentTimeMillis()

    private fun markAsRead() = viewModelScope.launch {
        repository.markAsRead(chatId)
    }

    private fun send(text: String) = viewModelScope.launch {
        if (text.isBlank()) return@launch
        val temp = ChatMessage(
            id = "local-${System.nanoTime()}",
            text = text.trim(),
            timestamp = System.currentTimeMillis(),
            isMine = true,
            senderName = "You"
        )
        _state.value = _state.value.copy(
            messages = _state.value.messages + temp,
            input = "",
            canSend = false
        )
        try {
            repository.sendMessage(chatId, text.trim())
        } catch (_: Exception) {
            // podrías marcar estado "failed" si quieres
        }
    }
}

/* ===== Repo contract + Fake ===== */

interface ChatThreadRepository {
    suspend fun getRecent(chatId: String): List<ChatMessage>
    suspend fun getOlder(chatId: String, count: Int, beforeTs: Long): List<ChatMessage>
    suspend fun sendMessage(chatId: String, text: String)
    suspend fun markAsRead(chatId: String)
}

class FakeChatThreadRepository : ChatThreadRepository {
    private val seed = listOf(
        ChatMessage("1","Hola, ¿todavía está disponible el iPhone?", 1710000100000,false,"Sofía"),
        ChatMessage("2","Sí, todavía está disponible. ¿Te interesa?",1710000400000,true,"You"),
        ChatMessage("3","Sí, me interesa. ¿Podría verlo en persona?",1710000700000,false,"Sofía"),
        ChatMessage("4","Claro, ¿cuándo te viene bien?",1710001000000,true,"You")
    )

    override suspend fun getRecent(chatId: String) = seed
    override suspend fun getOlder(
        chatId: String,
        count: Int,
        beforeTs: Long
    ): List<ChatMessage> {
        // Ejemplo: filtra mensajes con timestamp menor a beforeTs y limita a 'count'
        return seed
            .filter { it.timestamp < beforeTs }
            .sortedByDescending { it.timestamp }
            .take(count)
            .sortedBy { it.timestamp } // opcional: devuélvelos en orden cronológico
    }
    override suspend fun sendMessage(chatId: String, text: String) { /* no-op */ }
    override suspend fun markAsRead(chatId: String) { /* no-op */ }
}
