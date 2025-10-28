package com.example.team_23_kotlin.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.domain.usecase.CheckInCampusUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val checkInCampusUseCase: CheckInCampusUseCase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadUserProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.OnEditProfileClick -> println("Edit Profile Clicked")
            is ProfileEvent.OnSalesClick -> println("Sales Clicked")
            is ProfileEvent.OnProductClick -> { /* Navegación a detalle */ }
            is ProfileEvent.LoadUser -> loadUserProfile()
        }
    }

    fun onCampusStatusChanged(isInCampus: Boolean) {
        _state.update { it.copy(isInCampus = isInCampus) }
    }

    private suspend fun getUserProducts(uid: String): List<Product> {
        return try {
            val snapshot = firestore.collection("posts")
                .whereEqualTo("user_id", uid)
                .get()
                .await()

            snapshot.documents.mapNotNull { doc ->
                val data = doc.data ?: return@mapNotNull null
                val images = data["images"] as? List<*> ?: emptyList<Any>()
                val firstImage = images.firstOrNull() as? String ?: "https://picsum.photos/300/300"

                Product(
                    id = data["_id"] as? String ?: doc.id,
                    title = data["title"] as? String ?: "Sin título",
                    description = data["description"] as? String ?: "",
                    categoryName = data["category_name"] as? String ?: "",
                    imageUrl = firstImage,
                    price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                    status = data["status"] as? String ?: "",
                    createdAt = data["created_at"] as? String ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user == null) {
                    _state.update { it.copy(error = "No user logged in", isLoading = false) }
                    return@launch
                }

                val uid = user.uid
                val userDoc = firestore.collection("users").document(uid).get().await()
                val userData = userDoc.data ?: emptyMap()

                val name = userData["name"] as? String ?: user.displayName ?: "Unknown User"
                val email = userData["email"] as? String ?: user.email ?: ""
                val role = userData["role"] as? String ?: "Student"
                val major = userData["major"] as? String ?: "Not specified" // 🎓 nuevo campo

                val products = getUserProducts(uid)

                _state.update {
                    it.copy(
                        userName = name,
                        userHandle = "@${email.substringBefore("@")}",
                        userRole = role,
                        email = email,
                        major = major, // 🔹 agregado
                        products = products,
                        isLoading = false,
                        error = null
                    )
                }

            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(error = e.message ?: "Error loading profile", isLoading = false) }
            }
        }
    }
}
