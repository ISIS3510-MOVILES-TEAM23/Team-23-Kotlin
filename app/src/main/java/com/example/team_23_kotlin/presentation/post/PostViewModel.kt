package com.example.team_23_kotlin.presentation.post

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.core.network.hasInternetConnection
import com.example.team_23_kotlin.data.posts.CategoryEntity
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.File

class PostViewModel(
    private val repository: FirestorePostsRepository = FirestorePostsRepository(
        FirebaseFirestore.getInstance()
    )
) : ViewModel() {

    private val _state = MutableStateFlow(PostState())
    val state: StateFlow<PostState> = _state

    init {
        loadCategories()
    }

    // -----------------------------------------------------------
    // Manejo de eventos provenientes de la UI
    // -----------------------------------------------------------
    fun onEvent(e: PostEvent, context: Context? = null) {
        when (e) {
            is PostEvent.TitleChanged ->
                _state.value = _state.value.copy(title = e.value, errorMessage = null)

            is PostEvent.DescriptionChanged ->
                _state.value = _state.value.copy(description = e.value, errorMessage = null)

            is PostEvent.PriceChanged ->
                _state.value = _state.value.copy(price = e.value, errorMessage = null)

            is PostEvent.CategorySelected ->
                _state.value = _state.value.copy(
                    categoryId = e.id,
                    categoryName = e.name,
                    errorMessage = null
                )

            PostEvent.ReloadCategories -> loadCategories()

            is PostEvent.PhotoAdded -> {
                val newList = _state.value.photoTokens.toMutableList().apply {
                    add("uri:${e.uri}")
                }
                _state.value = _state.value.copy(photoTokens = newList)
            }

            is PostEvent.PhotoRemovedAt -> {
                val newList = _state.value.photoTokens.toMutableList().apply {
                    if (e.index in indices) removeAt(e.index)
                }
                _state.value = _state.value.copy(photoTokens = newList)
            }

            PostEvent.AddPhotosClick -> { /* handled by UI */ }

            PostEvent.SubmitClicked -> submitPost(context)

            PostEvent.ClearForm -> {
                _state.value = PostState(
                    categories = _state.value.categories,
                    categoryId = null,
                    categoryName = null
                )
            }

            is PostEvent.PickupPointSelected -> {
                _state.value = _state.value.copy(
                    pickupPointName = e.name,
                    pickupCoordinates = e.coordinates
                )
            }
        }
    }

    // -----------------------------------------------------------
    // Cargar categorías desde Firestore
    // -----------------------------------------------------------
    private fun loadCategories() {
        viewModelScope.launch {
            _state.value = _state.value.copy(categoriesLoading = true, categoriesError = null)
            try {
                val list: List<CategoryEntity> = repository.getCategories()
                val sorted = list.sortedBy { it.name }
                val current = _state.value
                val selectedStillExists = sorted.any { it.id == current.categoryId }

                _state.value = current.copy(
                    categories = sorted.map { Category(it.id, it.name) },
                    categoriesLoading = false,
                    categoriesError = null,
                    categoryId = if (selectedStillExists) current.categoryId else null,
                    categoryName = if (selectedStillExists) current.categoryName else null
                )
            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    categoriesLoading = false,
                    categoriesError = t.message ?: "Error loading categories"
                )
            }
        }
    }

    // -----------------------------------------------------------
    // Guardar borrador local (Local Storage)
    // -----------------------------------------------------------
    private fun saveDraftLocally(context: Context) {
        try {
            val s = _state.value
            val json = JSONObject().apply {
                put("title", s.title)
                put("description", s.description)
                put("price", s.price)
                put("categoryId", s.categoryId ?: "")
                put("categoryName", s.categoryName ?: "")
                put("photoTokens", s.photoTokens.joinToString(","))
            }
            val file = File(context.applicationContext.filesDir, "draft_post.json")
            file.writeText(json.toString())
            Log.d("LocalStorage", "✅ Draft saved at: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("LocalStorage", "⚠️ Error saving draft: ${e.message}")
        }
    }

    // -----------------------------------------------------------
    // Leer borrador local
    // -----------------------------------------------------------
    fun loadDraft(context: Context) {
        try {
            val file = File(context.applicationContext.filesDir, "draft_post.json")
            if (!file.exists()) return
            val json = JSONObject(file.readText())

            _state.value = _state.value.copy(
                title = json.optString("title", ""),
                description = json.optString("description", ""),
                price = json.optString("price", ""),
                categoryId = json.optString("categoryId", null),
                categoryName = json.optString("categoryName", null),
                photoTokens = json.optString("photoTokens", "")
                    .split(",")
                    .filter { it.isNotBlank() }
            )
        } catch (e: Exception) {
            Log.e("LocalStorage", "⚠️ Error loading draft: ${e.message}")
        }
    }

    // -----------------------------------------------------------
    // Borrar borrador local
    // -----------------------------------------------------------
    private fun clearDraft(context: Context) {
        val file = File(context.applicationContext.filesDir, "draft_post.json")
        if (file.exists()) {
            file.delete()
            Log.d("SyncDraft", "🗑️ Draft deleted successfully.")
        }
    }

    // -----------------------------------------------------------
    // Crear post o guardar borrador
    // -----------------------------------------------------------
    private fun submitPost(context: Context?) {
        val s = _state.value

        if (s.title.isBlank()) {
            _state.value = s.copy(errorMessage = "Please enter a title.")
            return
        }

        val cleanPrice = s.price.filter { it.isDigit() }
        if (cleanPrice.isBlank() || cleanPrice.toLongOrNull() == null || cleanPrice.toLong() <= 0L) {
            _state.value = s.copy(errorMessage = "Please enter a valid price.")
            return
        }

        if (s.categoryId == null || s.categoryName == null) {
            _state.value = s.copy(errorMessage = "Please select a category.")
            return
        }



        viewModelScope.launch {
            try {
                _state.value = s.copy(isSaving = true, errorMessage = null)

                val uris = s.photoTokens
                    .filter { it.startsWith("uri:") }
                    .map { Uri.parse(it.removePrefix("uri:")) }

                val post = Post(
                    title = s.title,
                    description = s.description,
                    price = cleanPrice.toLong(),
                    category = FirebaseFirestore.getInstance()
                        .document("/categories/${s.categoryId}"),
                    category_name = s.categoryName ?: "",
                    pickup_point_name = s.pickupPointName ?: "",
                    pickup_coordinates = s.pickupCoordinates ?: "",
                )


                if (context == null || !isOnline(context)) {
                    // 🚫 No hay Internet → guardar borrador localmente
                    context?.let { saveDraftLocally(it) }
                    _state.value = s.copy(
                        isSaving = false,
                        errorMessage = "No Internet — post saved locally!"
                    )
                    Log.d("SyncDraft", "📦 Post saved locally due to no connection.")
                    return@launch
                }

                // 🌐 Hay conexión → subir post
                val success = repository.createPost(post, uris)
                if (success) {
                    context?.let { clearDraft(it) }
                    _state.value = PostState(
                        categories = s.categories,
                        categoryId = s.categoryId,
                        categoryName = s.categoryName,
                        postedOk = true
                    )
                    Log.d("SyncDraft", "✅ Post uploaded to Firestore.")
                } else {
                    _state.value = s.copy(isSaving = false, errorMessage = "Error creating post.")
                }

            } catch (t: Throwable) {
                Log.e("SyncDraft", "❌ Error submitting post: ${t.message}", t)
                _state.value = s.copy(isSaving = false, errorMessage = t.message)
            }
        }
    }

    // -----------------------------------------------------------
    // 🌐 Sincronizar borrador (Eventual Connectivity)
    // -----------------------------------------------------------
    fun syncDraftIfNeeded(context: Context) {
        viewModelScope.launch {
            val file = File(context.applicationContext.filesDir, "draft_post.json")
            Log.d("SyncDraft", "🔍 Checking for local draft at ${file.absolutePath}")

            if (!file.exists()) {
                Log.d("SyncDraft", "❌ No local draft found.")
                return@launch
            }

            if (!isOnline(context)) {
                Log.d("SyncDraft", "🚫 Still offline, will retry later.")
                return@launch
            }

            try {
                val json = JSONObject(file.readText())
                val title = json.optString("title")
                val description = json.optString("description")
                val priceStr = json.optString("price").filter { it.isDigit() }
                val price = priceStr.toLongOrNull() ?: 0L
                val categoryId = json.optString("categoryId")
                val photoTokensStr = json.optString("photoTokens", "")
                val photoUris = photoTokensStr.split(",").filter { it.isNotBlank() }
                    .map { Uri.parse(it.removePrefix("uri:")) }

                Log.d("SyncDraft", "📤 Uploading saved post: $title ($price) → category=$categoryId")

                val post = Post(
                    title = title,
                    description = description,
                    price = price,
                    category = FirebaseFirestore.getInstance()
                        .document("/categories/$categoryId")
                )

                val success = repository.createPost(post, photoUris)
                if (success) {
                    clearDraft(context)
                    Log.d("SyncDraft", "✅ Draft uploaded successfully and deleted.")
                    _state.value = _state.value.copy(postedOk = true, errorMessage = null)
                } else {
                    Log.e("SyncDraft", "⚠️ Firestore upload failed.")
                }

            } catch (e: Exception) {
                Log.e("SyncDraft", "❌ Error reading local draft: ${e.message}", e)
            }
        }
    }

    // -----------------------------------------------------------
    // 🔌 Verificar conexión con ConnectivityManager
    // -----------------------------------------------------------
    private fun isOnline(context: Context): Boolean {
        return try {
            val result = context.hasInternetConnection()
            Log.d("SyncDraft", "🌐 Internet connectivity: $result")
            result
        } catch (e: Exception) {
            Log.e("SyncDraft", "⚠️ Error checking connectivity: ${e.message}")
            false
        }
    }
}
