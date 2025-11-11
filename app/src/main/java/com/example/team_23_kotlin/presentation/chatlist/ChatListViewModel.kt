package com.example.team_23_kotlin.presentation.chatlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.local.room.ChatEntity
import com.example.team_23_kotlin.data.local.room.LocalChatRepository
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ChatListViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    private val localRepo: LocalChatRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ChatListState())
    val state: StateFlow<ChatListState> = _state.asStateFlow()

    init {
        loadChats()
    }

    fun onEvent(event: ChatListEvent) {
        when (event) {
            is ChatListEvent.Refresh -> loadChats()
            is ChatListEvent.MarkAsRead -> markChatAsRead(event.chatId)
        }
    }

    private var listenerRegistration: ListenerRegistration? = null


    private fun loadChats() {
        val currentUser = auth.currentUser ?: run {
            _state.update { it.copy(error = "User not logged in", isLoading = false) }
            return
        }

        val uid = currentUser.uid
        _state.update { it.copy(isLoading = true, error = null) }

        listenerRegistration?.remove()

        // 🔹 Primero intenta cargar los chats locales (modo offline)
        viewModelScope.launch {
            val localChats = localRepo.getChats()
            if (localChats.isNotEmpty()) {
                _state.update {
                    it.copy(
                        chats = localChats.map { chat ->
                            ChatSummary(
                                id = chat.id,
                                listingTitle = chat.listingTitle,
                                senderName = chat.senderName,
                                lastMessage = chat.lastMessage,
                                lastTime = chat.lastTime,
                                unreadCount = chat.unreadCount
                            )
                        },
                        isLoading = false
                    )
                }
            }
        }

        // 🔹 Luego escucha los cambios en Firestore (modo online)
        listenerRegistration = firestore.collection("chats")
            .whereArrayContains("participant_ids", uid)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _state.update { it.copy(isLoading = false, error = error.message) }
                    return@addSnapshotListener
                }

                val chats = snapshot?.documents?.mapNotNull { doc ->
                    val data = doc.data ?: return@mapNotNull null
                    val productId = data["product_id"] as? String

                    var listingTitle = "Product"
                    var imageUrl: String? = null

                    if (!productId.isNullOrEmpty()) {
                        firestore.collection("posts").document(productId).get()
                            .addOnSuccessListener { postDoc ->
                                listingTitle = postDoc.getString("title") ?: "Product"
                                val images = postDoc.get("images") as? List<*>
                                val imageUrl = images?.firstOrNull() as? String


                                _state.update { s ->
                                    val newChats = s.chats.map {
                                        if (it.id == doc.id) it.copy(
                                            listingTitle = listingTitle,
                                            listingImageUrl = imageUrl
                                        ) else it
                                    }
                                    s.copy(chats = newChats)
                                }
                            }
                    }

                    ChatSummary(
                        id = doc.id,
                        listingTitle = listingTitle,
                        senderName = getParticipantName(uid, data),
                        lastMessage = data["last_message"] as? String ?: "",
                        lastTime = formatTimestamp(data["updated_at"]),
                        unreadCount = getUnreadCountForUser(uid, data),
                        listingImageUrl = imageUrl // ✅ nuevo campo
                    )
                } ?: emptyList()


                val sortedChats = chats.sortedByDescending { it.lastTime }

                // 🔹 Actualiza UI
                _state.update {
                    it.copy(chats = sortedChats, isLoading = false, error = null)
                }

                // 🔹 Guarda localmente los chats
                viewModelScope.launch {
                    val entities = sortedChats.map {
                        ChatEntity(
                            id = it.id,
                            listingTitle = it.listingTitle,
                            senderName = it.senderName,
                            lastMessage = it.lastMessage,
                            lastTime = it.lastTime,
                            unreadCount = it.unreadCount,
                            listingImageUrl = it.listingImageUrl
                        )
                    }
                    localRepo.saveChats(entities)
                }
            }
    }


    override fun onCleared() {
        super.onCleared()
        listenerRegistration?.remove()
    }


    private fun markChatAsRead(chatId: String) {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            try {
                val chatRef = firestore.collection("chats").document(chatId)

                val chat = chatRef.get().await()
                val data = chat.data ?: return@launch

                // 🔹 Actualiza el campo correspondiente
                val updateField = if (data["buyer_id"] == uid)
                    "unread_count_buyer"
                else
                    "unread_count_seller"

                chatRef.update(updateField, 0).await()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // 🔹 Auxiliar: obtiene el nombre del otro participante (opcional)
    private fun getParticipantName(currentUid: String, data: Map<String, Any>): String {
        val buyerId = data["buyer_id"] as? String
        val sellerId = data["seller_id"] as? String

        return when (currentUid) {
            buyerId -> "Seller"
            sellerId -> "Buyer"
            else -> "User"
        }
    }

    // 🔹 Auxiliar: convierte timestamp a texto legible
    private fun formatTimestamp(timestamp: Any?): String {
        return try {
            val ts = when (timestamp) {
                is Timestamp -> timestamp.toDate()
                is Date -> timestamp
                is Long -> Date(timestamp)
                else -> return ""
            }

            val formatter = SimpleDateFormat("hh:mm a", Locale.getDefault()) // Ej: "10:45 PM"
            formatter.format(ts)
        } catch (e: Exception) {
            ""
        }
    }

    // 🔹 Auxiliar: elige el unread count que corresponde al usuario
    private fun getUnreadCountForUser(uid: String, data: Map<String, Any>): Int {
        val buyerId = data["buyer_id"] as? String
        val sellerId = data["seller_id"] as? String
        val unreadBuyer = (data["unread_count_buyer"] as? Number)?.toInt() ?: 0
        val unreadSeller = (data["unread_count_seller"] as? Number)?.toInt() ?: 0

        return when (uid) {
            buyerId -> unreadBuyer
            sellerId -> unreadSeller
            else -> 0
        }
    }
}
