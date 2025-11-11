package com.example.team_23_kotlin.presentation.post

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.core.network.hasInternetConnection
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
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

    fun onEvent(e: PostEvent, context: Context? = null) {
        when (e) {
            is PostEvent.TitleChanged -> _state.value = _state.value.copy(title = e.value)
            is PostEvent.DescriptionChanged -> _state.value = _state.value.copy(description = e.value)
            is PostEvent.PriceChanged -> _state.value = _state.value.copy(price = e.value)
            is PostEvent.CategorySelected -> _state.value =
                _state.value.copy(categoryId = e.id, categoryName = e.name)
            is PostEvent.PhotoAdded -> {
                val list = _state.value.photoTokens.toMutableList().apply { add("uri:${e.uri}") }
                _state.value = _state.value.copy(photoTokens = list)
            }
            is PostEvent.PhotoRemovedAt -> {
                val list = _state.value.photoTokens.toMutableList().apply {
                    if (e.index in indices) removeAt(e.index)
                }
                _state.value = _state.value.copy(photoTokens = list)
            }
            is PostEvent.PickupPointSelected -> _state.value =
                _state.value.copy(
                    pickupPointName = e.name,
                    pickupCoordinates = e.coordinates
                )
            PostEvent.ReloadCategories -> loadCategories()
            PostEvent.SubmitClicked -> submitPost(context)
            PostEvent.ClearForm -> _state.value = PostState(categories = _state.value.categories)
            PostEvent.AddPhotosClick -> TODO()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            try {
                val list = repository.getCategories().sortedBy { it.name }
                _state.value = _state.value.copy(categories = list.map { Category(it.id, it.name) })
            } catch (t: Throwable) {
                Log.e("Categories", "⚠️ ${t.message}")
            }
        }
    }

    // ---------------------- BORRADOR LOCAL ----------------------
    // ------------------------------------------------------------
    // ------------------LOCAL STORAGE: Local Files ---------------
    // ------------------------------------------------------------
    private fun saveDraftLocally(context: Context) {
        try {
            val s = _state.value
            val json = JSONObject().apply {
                put("title", s.title)
                put("description", s.description)
                put("price", s.price)
                put("categoryId", s.categoryId ?: "")
                put("categoryName", s.categoryName ?: "")
                put("pickupPointName", s.pickupPointName ?: "")
                put("pickupCoordinates", s.pickupCoordinates ?: "")
                put("photoTokens", s.photoTokens.joinToString(","))
            }
            val file = File(context.filesDir, "draft_post.json")
            file.writeText(json.toString())
            Log.d("LocalStorage", "✅ Draft saved at: ${file.absolutePath}")
        } catch (e: Exception) {
            Log.e("LocalStorage", "❌ Error saving draft: ${e.message}")
        }
    }

    private fun clearDraft(context: Context) {
        val file = File(context.filesDir, "draft_post.json")
        if (file.exists()) file.delete()
    }

    // ---------------------- CREAR POST ----------------------

    private fun submitPost(context: Context?) {
        val s = _state.value

        if (s.title.isBlank() || s.categoryId == null || s.categoryName == null) {
            _state.value = s.copy(errorMessage = "Please complete all fields.")
            return
        }

        viewModelScope.launch(Dispatchers.Main) {
            try {
                _state.value = s.copy(isSaving = true, errorMessage = null, uploadProgress = 0f)

                val uris: List<Uri> = s.photoTokens
                    .filter { it.startsWith("uri:") }
                    .map { Uri.parse(it.removePrefix("uri:")) }

                val post = Post(
                    title = s.title,
                    description = s.description,
                    price = s.price.filter { it.isDigit() }.toLongOrNull() ?: 0L,
                    category = FirebaseFirestore.getInstance()
                        .document("/categories/${s.categoryId}"),
                    category_name = s.categoryName ?: "",
                    pickup_point_name = s.pickupPointName ?: "",
                    pickup_coordinates = s.pickupCoordinates ?: ""
                )

                val hasInternet = context?.hasInternetConnection() == true

                if (!hasInternet) {
                    // OFFLINE — guardar localmente
                    context?.let { saveDraftLocally(it) }
                    _state.value = s.copy(
                        isSaving = false,
                        postedOk = false,
                        errorMessage = "📦 No connection — post saved locally!"
                    )
                    Log.d("SyncDraft", "📦 Draft saved locally.")
                    return@launch
                }

                val uid = FirebaseAuth.getInstance().currentUser?.uid ?: "anonymous"

                // ================================
                // 🔹 Subida de imágenes en paralelo (IO)
                // ================================
                // ================================
                // proper dispatchers (IO / Main) and async parallel execution.
                // ================================
                val uploadedUrls = if (uris.isNotEmpty()) {
                    supervisorScope {
                        val total = uris.size
                        var completed = 0

                        val jobs: List<Deferred<String?>> = uris.map { uri ->
                            async(Dispatchers.IO) {
                                val url = repository.uploadImage(uid, uri)

                                withContext(Dispatchers.Main) {
                                    completed++
                                    val progress = completed.toFloat() / total
                                    _state.value = _state.value.copy(uploadProgress = progress)
                                }
                                url
                            }
                        }

                        jobs.awaitAll().filterNotNull()
                    }
                } else emptyList()

                // ================================
                // 🔹 Crear post final (IO)
                // ================================
                val success = withContext(Dispatchers.IO) {
                    repository.createPost(post, uris)
                }

                if (success) {
                    context?.let { clearDraft(it) }
                    _state.value = s.copy(
                        isSaving = false,
                        postedOk = true,
                        uploadProgress = 1f,
                        errorMessage = "✅ Post created with ${uploadedUrls.size} images!"
                    )
                    Log.d("SyncDraft", "✅ Post uploaded.")
                } else {
                    _state.value = s.copy(isSaving = false, errorMessage = "⚠️ Error creating post.")
                }

            } catch (e: Exception) {
                _state.value = s.copy(isSaving = false, errorMessage = e.message)
                Log.e("SubmitPost", "❌ ${e.message}")
            }
        }
    }

    // ---------------------- EVENTUAL CONNECTIVITY ----------------------

    fun syncDraftIfNeeded(context: Context) {
        viewModelScope.launch {
            val file = File(context.filesDir, "draft_post.json")
            if (!file.exists()) return@launch
            if (!context.hasInternetConnection()) return@launch

            try {
                val json = JSONObject(file.readText())
                val uris = json.optString("photoTokens", "")
                    .split(",").filter { it.isNotBlank() }
                    .map { Uri.parse(it.removePrefix("uri:")) }

                val post = Post(
                    title = json.optString("title"),
                    description = json.optString("description"),
                    price = json.optString("price").filter { it.isDigit() }.toLongOrNull() ?: 0L,
                    category = FirebaseFirestore.getInstance()
                        .document("/categories/${json.optString("categoryId")}"),
                    category_name = json.optString("categoryName"),
                    pickup_point_name = json.optString("pickupPointName"),
                    pickup_coordinates = json.optString("pickupCoordinates")
                )

                val success = repository.createPost(post, uris)
                if (success) {
                    clearDraft(context)
                    _state.value = _state.value.copy(errorMessage = "✅ Draft uploaded successfully!")
                    Log.d("SyncDraft", "✅ Draft uploaded and deleted.")
                }
            } catch (e: Exception) {
                Log.e("SyncDraft", "❌ Error uploading draft: ${e.message}")
            }
        }
    }
}
