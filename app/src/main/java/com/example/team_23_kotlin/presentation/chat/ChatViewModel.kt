// presentation/chat/ChatViewModel.kt
package com.example.team_23_kotlin.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch



// ViewModel
class ChatViewModel(private val chatId: String) : ViewModel() {

    private val _state = MutableStateFlow(
        ChatState(
            header = ChatHeader(
                chatId = chatId,
                peerName = "",
                peerAvatarUrl = null,
                listingTitle = null
            )
        )
    )
    val state: StateFlow<ChatState> = _state.asStateFlow()

    init {
        loadInitialData()
    }

    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.OnMessageInputChange -> {
                _state.value = _state.value.copy(
                    input = event.input,
                    canSend = event.input.trim().isNotEmpty()
                )
            }

            is ChatEvent.SendMessage -> {
                if (event.message.trim().isNotEmpty()) {
                    sendMessage(event.message.trim())
                }
            }

            ChatEvent.ShowPurchaseButton -> {
                _state.value = _state.value.copy(showPurchaseButton = true)
            }

            ChatEvent.HidePurchaseButton -> {
                _state.value = _state.value.copy(showPurchaseButton = false)
            }

            ChatEvent.LoadMessages -> {
                loadMessages()
            }
        }
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                // Simular carga de datos del header
                val updatedHeader = ChatHeader(
                    chatId = chatId,
                    peerName = "Juan Pérez",
                    peerAvatarUrl = "https://picsum.photos/200/300",
                    listingTitle = "iPhone 14 Pro Max - Usado"
                )

                // Simular mensajes iniciales
                val initialMessages = listOf(
                    ChatMessage(
                        id = "1",
                        text = "Hola, estoy interesado en tu iPhone",
                        timestamp = System.currentTimeMillis() - 300000, // 5 min ago
                        isMine = true,
                        senderName = "Tú",
                        senderAvatarUrl = null
                    ),
                    ChatMessage(
                        id = "2",
                        text = "¡Hola! Perfecto, te puedo dar más detalles",
                        timestamp = System.currentTimeMillis() - 240000, // 4 min ago
                        isMine = false,
                        senderName = updatedHeader.peerName,
                        senderAvatarUrl = updatedHeader.peerAvatarUrl
                    ),
                    ChatMessage(
                        id = "3",
                        text = "¿En qué estado está? ¿Tiene la caja original?",
                        timestamp = System.currentTimeMillis() - 180000, // 3 min ago
                        isMine = true,
                        senderName = "Tú",
                        senderAvatarUrl = null
                    ),
                    ChatMessage(
                        id = "4",
                        text = "Está en excelente estado, sin rayones. Sí incluye la caja y todos los accesorios originales",
                        timestamp = System.currentTimeMillis() - 120000, // 2 min ago
                        isMine = false,
                        senderName = updatedHeader.peerName,
                        senderAvatarUrl = updatedHeader.peerAvatarUrl
                    ),
                    ChatMessage(
                        id = "5",
                        text = "Perfecto, me interesa comprarlo. ¿Cuándo podemos hacer la entrega?",
                        timestamp = System.currentTimeMillis() - 60000, // 1 min ago
                        isMine = true,
                        senderName = "Tú",
                        senderAvatarUrl = null
                    )
                )

                _state.value = _state.value.copy(
                    header = updatedHeader,
                    messages = initialMessages,
                    isLoading = false,
                    error = null,
                    showPurchaseButton = true // Mostrar después de la conversación inicial
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error al cargar el chat: ${e.message}"
                )
            }
        }
    }

    private fun loadMessages() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)

            try {
                // Aquí implementarías la lógica para cargar mensajes desde tu API/base de datos
                // Por ejemplo:
                // val messages = chatRepository.getMessages(chatId)

                _state.value = _state.value.copy(
                    isLoading = false,
                    error = null
                )

            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error al cargar mensajes: ${e.message}"
                )
            }
        }
    }

    private fun sendMessage(message: String) {
        viewModelScope.launch {
            val newMessage = ChatMessage(
                id = "msg_${System.currentTimeMillis()}",
                text = message,
                timestamp = System.currentTimeMillis(),
                isMine = true,
                senderName = "Tú",
                senderAvatarUrl = null
            )

            // Agregar el mensaje inmediatamente a la UI
            _state.value = _state.value.copy(
                messages = _state.value.messages + newMessage,
                input = "",
                canSend = false
            )

            try {
                // Aquí enviarías el mensaje a tu API
                // chatRepository.sendMessage(chatId, message)

                // Simular respuesta automática después de unos segundos
                simulateResponse()

            } catch (e: Exception) {
                // En caso de error, podrías remover el mensaje o marcarlo como fallido
                _state.value = _state.value.copy(
                    error = "Error al enviar mensaje: ${e.message}"
                )
            }
        }
    }

    private fun simulateResponse() {
        viewModelScope.launch {
            kotlinx.coroutines.delay(1500) // Simular delay de respuesta

            val responses = listOf(
                "Podemos encontrarnos mañana en la tarde si te parece bien",
                "¿Te parece bien el precio de $800.000?",
                "Puedo enviarte más fotos si gustas",
                "¿Prefieres que nos encontremos en el centro comercial?",
                "Perfecto, hagamos el trato entonces",
                "¿Ya tienes el dinero listo?"
            )

            val randomResponse = responses.random()
            val responseMessage = ChatMessage(
                id = "response_${System.currentTimeMillis()}",
                text = randomResponse,
                timestamp = System.currentTimeMillis(),
                isMine = false,
                senderName = _state.value.header.peerName,
                senderAvatarUrl = _state.value.header.peerAvatarUrl
            )

            _state.value = _state.value.copy(
                messages = _state.value.messages + responseMessage
            )

            // Mostrar botón de compra después de cierta cantidad de mensajes
            if (_state.value.messages.size >= 7 && !_state.value.showPurchaseButton) {
                _state.value = _state.value.copy(showPurchaseButton = true)
            }
        }
    }

    // Funciones públicas para control manual del botón de compra
    fun showPurchaseButton() {
        _state.value = _state.value.copy(showPurchaseButton = true)
    }

    fun hidePurchaseButton() {
        _state.value = _state.value.copy(showPurchaseButton = false)
    }

    // Función para limpiar errores
    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
