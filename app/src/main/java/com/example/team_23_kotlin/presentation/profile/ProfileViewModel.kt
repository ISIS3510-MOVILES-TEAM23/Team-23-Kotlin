package com.example.team_23_kotlin.presentation.profile

import android.content.Context
import android.net.Uri
import android.util.Log
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.core.network.hasInternetConnection
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.Post
import com.example.team_23_kotlin.domain.usecase.CheckInCampusUseCase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONObject
import java.io.File
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val checkInCampusUseCase: CheckInCampusUseCase,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val appContext: Context
) : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadUserProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.OnEditProfileClick -> Unit
            is ProfileEvent.OnSalesClick -> Unit
            is ProfileEvent.OnProductClick -> Unit
            is ProfileEvent.LoadUser -> loadUserProfile()
        }
    }

    fun onCampusStatusChanged(isInCampus: Boolean) {
        _state.update { it.copy(isInCampus = isInCampus) }
    }

    // ---------- Productos Firestore ----------
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
                    id = (data["_id"] as? String) ?: doc.id,
                    title = data["title"] as? String ?: "Sin título",
                    description = data["description"] as? String ?: "",
                    categoryName = data["category_name"] as? String ?: "",
                    imageUrl = firstImage,
                    price = (data["price"] as? Number)?.toDouble() ?: 0.0,
                    status = data["status"] as? String ?: "",
                    createdAt = (data["created_at"] as? String) ?: ""
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    // ---------- Draft local ----------
    private fun loadLocalDrafts(): List<Product> {
        val drafts = mutableListOf<Product>()
        try {
            val file = File(appContext.filesDir, "draft_post.json")
            if (!file.exists()) return emptyList()

            val json = JSONObject(file.readText())
            val title = json.optString("title", "Untitled draft")
            val description = json.optString("description", "")
            val price = json.optString("price", "0").filter { it.isDigit() }.toDoubleOrNull() ?: 0.0
            val categoryName = json.optString("categoryName", "No category")
            val photoTokens = json.optString("photoTokens", "")
                .split(",")
                .filter { it.isNotBlank() }

            val firstLocalImage = photoTokens.firstOrNull()?.removePrefix("uri:")
                ?: "https://picsum.photos/300/300"

            drafts.add(
                Product(
                    id = "draft_local",
                    title = title,
                    description = description,
                    categoryName = categoryName,
                    imageUrl = firstLocalImage,
                    price = price,
                    status = "Draft (offline)",
                    createdAt = "Local only"
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return drafts
    }

    fun refreshDraftsOnly() {
        val drafts = loadLocalDrafts()
        _state.update { it.copy(drafts = drafts) }
    }

    // ---------- Sync al volver al perfil ----------
    fun syncDraftsIfOnline() {
        viewModelScope.launch {
            val file = File(appContext.filesDir, "draft_post.json")
            if (!file.exists()) return@launch
            if (!appContext.hasInternetConnection()) return@launch

            try {
                val json = JSONObject(file.readText())
                val uris = json.optString("photoTokens", "")
                    .split(",").filter { it.isNotBlank() }
                    .map { Uri.parse(it.removePrefix("uri:")) }

                val post = Post(
                    title = json.optString("title"),
                    description = json.optString("description"),
                    price = json.optString("price").filter { it.isDigit() }.toLongOrNull() ?: 0L,
                    category = firestore.document("/categories/${json.optString("categoryId")}"),
                    category_name = json.optString("categoryName"),
                    pickup_point_name = json.optString("pickupPointName"),
                    pickup_coordinates = json.optString("pickupCoordinates")
                )

                // Instancia inline, sin Hilt extra
                val repo = FirestorePostsRepository(firestore)
                val success = repo.createPost(post, uris)

                if (success) {
                    file.delete()
                    // refrescar listas (productos y drafts)
                    val uid = auth.currentUser?.uid
                    val products = if (uid != null) getUserProducts(uid) else emptyList()
                    _state.update {
                        it.copy(
                            products = products,
                            drafts = emptyList(),
                            // 👇 dispara el toast
                            toastMessage = "Draft posted because you’re back online"
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ProfileVM", "❌ Error syncing draft: ${e.message}")
            }
        }
    }

    fun consumeToastMessage() {
        _state.update { it.copy(toastMessage = null) }
    }

    // ---------- Carga perfil + productos + drafts ----------
    fun loadUserProfile() {
        viewModelScope.launch {
            try {
                val user = auth.currentUser
                if (user == null) {
                    _state.update { it.copy(error = "No user logged in", isLoading = false) }
                    return@launch
                }

                val uid = user.uid

                val userDoc = firestore.collection("users").document(uid)
                    .get(Source.DEFAULT)
                    .await()
                val userData = userDoc.data ?: emptyMap<String, Any?>()

                val name = (userData["name"] as? String) ?: user.displayName ?: "Unknown User"
                val email = (userData["email"] as? String) ?: user.email.orEmpty()
                val role = (userData["role"] as? String) ?: "Student"
                val major = (userData["major"] as? String) ?: "Not specified"

                val products = getUserProducts(uid)
                val drafts = loadLocalDrafts()

                _state.update {
                    it.copy(
                        userName = name,
                        userHandle = "@${email.substringBefore("@")}",
                        userRole = role,
                        email = email,
                        major = major,
                        products = products,
                        drafts = drafts,
                        isLoading = false,
                        error = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                val message = if (e.message?.contains("offline", ignoreCase = true) == true)
                    "You’re offline. Connect to the internet to load your profile."
                else
                    "Error loading profile. Please try again."

                _state.update { it.copy(error = message, isLoading = false) }
            }
        }
    }
}
