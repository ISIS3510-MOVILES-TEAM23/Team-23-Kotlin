package com.example.team_23_kotlin.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()

    // =====================================================
    // 🔹 Cargar un chat específico
    // =====================================================
    fun loadChat(chatId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)

            try {
                val chatDoc = firestore.collection("chats").document(chatId).get().await()
                if (!chatDoc.exists()) {
                    _state.value = _state.value.copy(error = "Chat not found", isLoading = false)
                    return@launch
                }

                val data = chatDoc.data ?: emptyMap()
                val productId = data["product_id"] as? String
                val buyerId = data["buyer_id"] as? String
                val sellerId = data["seller_id"] as? String
                val currentUid = auth.currentUser?.uid ?: ""

                // Determinar el peer (la otra persona)
                val peerId = if (buyerId == currentUid) sellerId else buyerId
                val peerDoc = peerId?.let { firestore.collection("users").document(it).get().await() }
                val peerName = peerDoc?.getString("name") ?: "User"
                val peerAvatarUrl = peerDoc?.getString("photoUrl")

                // Cargar título del producto
                val productDoc = productId?.let { firestore.collection("posts").document(it).get().await() }
                val productTitle = productDoc?.getString("title") ?: "Product"

                // Actualizar header
                _state.value = _state.value.copy(
                    header = ChatHeader(
                        chatId = chatId,
                        peerName = peerName,
                        peerAvatarUrl = peerAvatarUrl,
                        listingTitle = productTitle
                    ),
                    isLoading = false
                )

                // Escuchar mensajes en tiempo real
                listenToMessages(chatId)

            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(error = e.message, isLoading = false)
            }
        }
    }

    // =====================================================
    // 🔹 Escuchar mensajes en tiempo real
    // =====================================================
    private fun listenToMessages(chatId: String) {
        val currentUid = auth.currentUser?.uid ?: return

        firestore.collection("chats")
            .document(chatId)
            .collection("messages")
            .orderBy("sent_at")
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    _state.value = _state.value.copy(error = "Error loading messages: ${e.message}")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.map { doc ->
                    ChatMessage(
                        id = doc.id,
                        text = doc.getString("content") ?: "",
                        timestamp = doc.getTimestamp("sent_at")?.toDate()?.time ?: 0L,
                        isMine = doc.getString("sender_id") == currentUid,
                        senderName = if (doc.getString("sender_id") == currentUid)
                            "You" else _state.value.header.peerName,
                        senderAvatarUrl = if (doc.getString("sender_id") == currentUid)
                            null else _state.value.header.peerAvatarUrl
                    )
                } ?: emptyList()

                _state.value = _state.value.copy(messages = messages, isLoading = false)
            }
    }

    // =====================================================
    // 🔹 Enviar mensaje
    // =====================================================
    fun sendMessage(chatId: String, message: String) {
        viewModelScope.launch {
            val user = auth.currentUser ?: return@launch
            val msgRef = firestore.collection("chats").document(chatId)
                .collection("messages").document()

            val messageData = mapOf(
                "content" to message,
                "sender_id" to user.uid,
                "read" to false,
                "sent_at" to Timestamp.now()
            )

            try {
                msgRef.set(messageData).await()

                firestore.collection("chats").document(chatId).update(
                    mapOf(
                        "last_message" to message,
                        "updated_at" to Timestamp.now()
                    )
                ).await()

                _state.value = _state.value.copy(input = "", canSend = false)

            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(error = "Error sending message: ${e.message}")
            }
        }
    }

    // =====================================================
    // 🔹 Marcar mensajes como leídos
    // =====================================================
    fun markMessagesAsRead(chatId: String) {
        viewModelScope.launch {
            val userId = auth.currentUser?.uid ?: return@launch
            try {
                val unreadMessages = firestore.collection("chats")
                    .document(chatId)
                    .collection("messages")
                    .whereEqualTo("read", false)
                    .get()
                    .await()

                for (doc in unreadMessages.documents) {
                    val senderId = doc.getString("sender_id")
                    if (senderId != userId) {
                        doc.reference.update("read", true)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // =====================================================
    // 🔹 Manejar eventos desde ChatScreen
    // =====================================================
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.OnMessageInputChange -> {
                _state.value = _state.value.copy(
                    input = event.input,
                    canSend = event.input.isNotBlank()
                )
            }

            is ChatEvent.SendMessage -> {
                if (event.message.isNotBlank()) {
                    sendMessage(_state.value.header.chatId, event.message.trim())
                }
            }

            is ChatEvent.LoadChat -> {
                loadChat(event.chatId)
            }

            ChatEvent.LoadMessages -> {
                listenToMessages(_state.value.header.chatId)
            }

            ChatEvent.ShowPurchaseButton -> {
                _state.value = _state.value.copy(showPurchaseButton = true)
            }

            ChatEvent.HidePurchaseButton -> {
                _state.value = _state.value.copy(showPurchaseButton = false)
            }
        }
    }

    fun clearError() {
        _state.value = _state.value.copy(error = null)
    }
}
