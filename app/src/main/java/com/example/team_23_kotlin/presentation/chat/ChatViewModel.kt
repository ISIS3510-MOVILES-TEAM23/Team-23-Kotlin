package com.example.team_23_kotlin.presentation.chat

import android.content.Context
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.util.LruCache
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
import com.google.firebase.firestore.MetadataChanges
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File


@HiltViewModel
class ChatViewModel @Inject constructor(
    private val firestore: FirebaseFirestore,
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ChatState())
    val state: StateFlow<ChatState> = _state.asStateFlow()
    private val _exportMessage = MutableStateFlow<String?>(null)
    val exportMessage: StateFlow<String?> = _exportMessage.asStateFlow()


    private val messageCache = LruCache<String, List<ChatMessage>>(5)

    private val gson = Gson()

    // =====================================================
    // 🔹 Cargar un chat específico
    // =====================================================
    fun loadChat(chatId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            messageCache.get(chatId)?.let { cachedMessages ->
                _state.value = _state.value.copy(
                    messages = cachedMessages,
                    isLoading = false,
                    error = null
                )
                Log.d("ChatViewModel", "⚡ Mensajes cargados desde memoria (LRUCache)")
                return@launch
            }

            try {
                // Intentar cargar el chat desde Firestore
                val chatDoc = firestore.collection("chats").document(chatId).get().await()
                if (!chatDoc.exists()) throw Exception("Chat not found")

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
                Log.e("ChatViewModel", "Error loading from Firestore: ${e.message}")

                // Si Firestore falla, intentar cargar respaldo local
                val file = File(context.filesDir, "chat_${chatId}.json")
                if (file.exists()) {
                    try {
                        val json = file.readText()
                        val backup = gson.fromJson(json, ChatBackup::class.java)

                        _state.value = _state.value.copy(
                            header = ChatHeader(
                                chatId = backup.chatId,
                                peerName = backup.peerName,
                                listingTitle = backup.listingTitle
                            ),
                            messages = backup.messages,
                            isLoading = false,
                            error = null
                        )

                        _exportMessage.value = "💾 Chat cargado desde respaldo local"
                    } catch (ex: Exception) {
                        _state.value = _state.value.copy(error = "Error leyendo respaldo local")
                    }
                } else {
                    _state.value = _state.value.copy(error = "No se pudo cargar el chat")
                }
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
            // 🔹 Incluye metadata para recibir eventos locales y remotos
            .addSnapshotListener(MetadataChanges.INCLUDE) { snapshot, e ->
                if (e != null) {
                    _state.value = _state.value.copy(error = "Error loading messages: ${e.message}")
                    return@addSnapshotListener
                }

                val messages = snapshot?.documents?.map { doc ->
                    val senderId = doc.getString("sender_id") ?: ""
                    val pending = doc.metadata.hasPendingWrites()
                    val status = if (pending) MessageStatus.SENDING else MessageStatus.SENT

                    ChatMessage(
                        id = doc.id,
                        text = doc.getString("content") ?: "",
                        timestamp = doc.getTimestamp("sent_at")?.toDate()?.time ?: 0L,
                        isMine = senderId == currentUid,
                        senderName = if (senderId == currentUid) "You" else _state.value.header.peerName,
                        senderAvatarUrl = if (senderId == currentUid) null else _state.value.header.peerAvatarUrl,
                        deliveryStatus = status
                    )
                } ?: emptyList()

                // 🔹 Guarda los mensajes en la caché
                messageCache.put(chatId, messages)
                Log.d("ChatViewModel", "💨 Mensajes guardados en LRUCache (${messages.size})")

                _state.value = _state.value.copy(messages = messages)
                try {
                    val backup = ChatBackup(
                        chatId = chatId,
                        peerName = _state.value.header.peerName,
                        listingTitle = _state.value.header.listingTitle,
                        messages = messages
                    )
                    val file = File(context.filesDir, "chat_${chatId}.json")
                    file.writeText(gson.toJson(backup))
                    Log.d("ChatViewModel", "💾 Respaldo local actualizado")
                } catch (ex: Exception) {
                    Log.e("ChatViewModel", "Error guardando respaldo local: ${ex.message}")
                }

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
            _state.value = _state.value.copy(input = "", canSend = false)

            try {
                msgRef.set(messageData).await()

                firestore.collection("chats").document(chatId).update(
                    mapOf(
                        "last_message" to message,
                        "updated_at" to Timestamp.now()
                    )
                ).await()



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
    // =====================================================
    // 🔹 Exportar chat a archivo local JSON
    // =====================================================
    fun exportChatToJson() {
        viewModelScope.launch {
            try {
                val chatId = _state.value.header.chatId
                val messages = _state.value.messages

                if (messages.isEmpty()) {
                    Log.e("ChatViewModel", "No messages to export")
                    return@launch
                }

                val exportData = mapOf(
                    "chatId" to chatId,
                    "peerName" to _state.value.header.peerName,
                    "listingTitle" to _state.value.header.listingTitle,
                    "messages" to messages.map {
                        mapOf(
                            "sender" to it.senderName,
                            "text" to it.text,
                            "timestamp" to it.timestamp,
                            "isMine" to it.isMine,
                            "status" to it.deliveryStatus.name
                        )
                    }
                )

                val file = File(context.filesDir, "chat_${chatId}.json")
                file.writeText(gson.toJson(exportData))
                _exportMessage.value = "✅ Chat exportado correctamente"
                println("✅ Chat exportado correctamente a ${file.absolutePath}")
                Log.e("ChatViewModel", "✅ Chat exportado correctamente a ${file.absolutePath}")



                // (Opcional) feedback en UI
                _state.value = _state.value.copy(
                    error = null
                )

            } catch (e: Exception) {
                e.printStackTrace()
                _state.value = _state.value.copy(error = "Error al exportar chat: ${e.message}")
            }
        }
    }
}
