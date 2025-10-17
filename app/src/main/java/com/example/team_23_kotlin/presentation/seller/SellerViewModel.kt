package com.example.team_23_kotlin.presentation.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class SellerViewModel(private val sellerId: String) : ViewModel() {

    private val db = FirebaseFirestore.getInstance()
    private val _state = MutableStateFlow(SellerState())
    val state: StateFlow<SellerState> = _state

    init {
        onEvent(SellerEvent.LoadSeller(sellerId))
    }

    fun onEvent(event: SellerEvent) {
        when (event) {
            is SellerEvent.LoadSeller -> loadSeller(event.sellerId)
        }
    }

    private fun loadSeller(sellerId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                // 🔹 1️⃣ Obtener los datos del usuario
                val userDoc = db.collection("users").document(sellerId).get().await()
                if (!userDoc.exists()) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = "Vendedor no encontrado"
                    )
                    return@launch
                }

                val name = userDoc.getString("name") ?: "Usuario desconocido"
                val email = userDoc.getString("email") ?: "Sin correo"
                val profileImageUrl =
                    userDoc.getString("profileImageUrl")
                        ?: "https://cdn-icons-png.flaticon.com/512/149/149071.png"

                // ⬇️ AQUÍ es donde agregas esta línea
                val isInCampus = userDoc.getBoolean("isInCampus") ?: true

                // 🔹 2️⃣ Obtener los productos del vendedor
                val postsSnapshot = db.collection("posts")
                    .whereEqualTo("user_id", sellerId)
                    .get()
                    .await()

                val products = postsSnapshot.documents.map { doc ->
                    val title = doc.getString("title") ?: ""
                    val priceValue = doc.getDouble("price") ?: 0.0
                    val price = "$${"%,.0f".format(priceValue)}"
                    val images =
                        (doc.get("images") as? List<*>)?.filterIsInstance<String>() ?: emptyList()
                    val imageUrl = images.firstOrNull()
                        ?: "https://cdn-icons-png.flaticon.com/512/679/679922.png"

                    ProductItem(
                        id = doc.id,
                        title = title,
                        price = price,
                        imageUrl = imageUrl
                    )
                }

                val seller = SellerUiModel(
                    id = sellerId,
                    name = name,
                    role = email,
                    rating = 4.8f,
                    profileImageUrl = profileImageUrl,
                    isInCampus = isInCampus,
                    products = products
                )

                _state.value = SellerState(seller = seller, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = "Error al cargar vendedor: ${e.message}"
                )
            }
        }
    }
}