package com.example.team_23_kotlin.presentation.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.posts.PostsRepository
import com.example.team_23_kotlin.domain.repository.AnalyticsRepository
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ProductViewModel(
    private val repo: PostsRepository,
    private val analytics: AnalyticsRepository
) : ViewModel() {

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(ProductState())
    val state: kotlinx.coroutines.flow.StateFlow<ProductState> = _state

    fun onEvent(event: ProductEvent) {
        when (event) {
            is ProductEvent.LoadProduct -> load(event.productId)
        }
    }

    private fun load(productId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val entity = repo.getPostById(productId)

                val sellerName = repo.getUserNameById(entity.userId)

                val ui = ProductUiModel(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    price = "$${entity.price}",
                    images = entity.images,
                    sellerName = sellerName,
                    sellerRating = 4.5f
                )

                println("📦 CategoryName being logged: ${entity.categoryName}")

                analytics.logProductClick(
                    postId = entity.id,
                    category = entity.categoryName,
                    source = "product_screen"
                )

                _state.value = ProductState(product = ui, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading product"
                )
            }
        }
    }

    fun contactSeller(productId: String, onChatCreated: (String) -> Unit) {
        viewModelScope.launch {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val auth = com.google.firebase.auth.FirebaseAuth.getInstance()
            val currentUser = auth.currentUser ?: return@launch

            try {
                // 1️⃣ Obtener el producto y el vendedor
                val productDoc = firestore.collection("posts").document(productId).get().await()
                val sellerId = productDoc.getString("user_id") ?: return@launch

                // 2️⃣ Evitar crear chat contigo mismo
                if (sellerId == currentUser.uid) return@launch

                // 3️⃣ Buscar si ya existe chat entre comprador y vendedor para este producto
                val existing = firestore.collection("chats")
                    .whereEqualTo("product_id", productId)
                    .whereArrayContains("participant_ids", currentUser.uid)
                    .get()
                    .await()

                val chatId = if (existing.documents.isNotEmpty()) {
                    existing.documents.first().id
                } else {
                    // 4️⃣ Crear el nuevo documento en /chats
                    val newChat = hashMapOf(
                        "buyer_id" to currentUser.uid,
                        "seller_id" to sellerId,
                        "product_id" to productId,
                        "participant_ids" to listOf(currentUser.uid, sellerId),
                        "last_message" to "Hola, estoy interesado en tu producto",
                        "unread_count_buyer" to 0,
                        "unread_count_seller" to 1,
                        "created_at" to com.google.firebase.Timestamp.now(),
                        "updated_at" to com.google.firebase.Timestamp.now()
                    )

                    val ref = firestore.collection("chats").add(newChat).await()

                    // ✅ 5️⃣ Crear la subcolección "messages" con el primer mensaje
                    val messageData = mapOf(
                        "content" to "Hola, estoy interesado en tu producto",
                        "sender_id" to currentUser.uid,
                        "image" to null,
                        "read" to false,
                        "sent_at" to com.google.firebase.Timestamp.now()
                    )

                    firestore.collection("chats")
                        .document(ref.id)
                        .collection("messages")
                        .add(messageData)
                        .await()

                    ref.id
                }

                // 6️⃣ Callback → navega al chat existente o nuevo
                onChatCreated(chatId)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


}
