package com.example.team_23_kotlin.presentation.post

import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.flow.update

class PostViewModel(
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
) : ViewModel() {

    private val _state = MutableStateFlow(PostState())
    val state: StateFlow<PostState> = _state

    init {
        loadCategories()
    }

    fun onEvent(e: PostEvent) {
        when (e) {
            is PostEvent.TitleChanged ->
                _state.value = _state.value.copy(title = e.value, errorMessage = null)
            is PostEvent.DescriptionChanged ->
                _state.value = _state.value.copy(description = e.value, errorMessage = null)
            is PostEvent.PriceChanged ->
                _state.value = _state.value.copy(price = e.value, errorMessage = null)
            is PostEvent.CategorySelected ->
                _state.value = _state.value.copy(
                    categoryId = e.id, categoryName = e.name, errorMessage = null
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
            is PostEvent.ClearForm -> {
                _state.update { it.copy(
                    title = "",
                    description = "",
                    price = "",
                    categoryId = null,
                    categoryName = null,
                    photoTokens = emptyList()
                ) }
            }
            is PostEvent.PickupPointSelected -> {
                _state.update {
                    it.copy(
                        pickupPointName = e.name,
                        pickupCoordinates = e.coordinates
                    )
                }
            }



            PostEvent.AddPhotosClick -> {}
            PostEvent.SubmitClicked -> submitPost()
        }
    }

    private fun loadCategories() {
        viewModelScope.launch {
            _state.value = _state.value.copy(categoriesLoading = true, categoriesError = null)
            try {
                val snap = firestore.collection("categories").get().await()
                val list = snap.documents.mapNotNull { doc ->
                    val name = doc.getString("name") ?: return@mapNotNull null
                    Category(id = doc.id, name = name)
                }.sortedBy { it.name }

                val current = _state.value
                val selectedStillExists = list.any { it.id == current.categoryId }
                _state.value = current.copy(
                    categories = list,
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

    private fun submitPost() {
        val s = _state.value
        val uid = auth.currentUser?.uid ?: "anonymous"

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
        if (s.pickupPointName == null) {
            _state.value = s.copy(errorMessage = "Please select a pickup point.")
            return
        }

        viewModelScope.launch {
            if (auth.currentUser == null) {
                auth.signInAnonymously().await()
            }
            try {
                _state.value = s.copy(isSaving = true, errorMessage = null)

                // --- Subir imágenes nuevas y guardar URLs ---
                val downloadUrls = mutableListOf<String>()
                for (token in s.photoTokens.filter { it.startsWith("uri:") }) {
                    val uri = Uri.parse(token.removePrefix("uri:"))
                    uploadToStorage(uid, uri)?.let { url ->
                        downloadUrls.add(url)
                    }
                }

                // --- Crear documento en Firestore ---
                val postsRef = firestore.collection("posts")
                val newDoc = postsRef.document()
                val categoryRef: DocumentReference = firestore.document("/categories/${s.categoryId}")

                val data = hashMapOf(
                    "_id" to newDoc.id,
                    "title" to s.title,
                    "description" to s.description,
                    "price" to cleanPrice.toLong(),
                    "images" to downloadUrls,
                    "category_id" to categoryRef,
                    "category_name" to s.categoryName,
                    "status" to "active",
                    "created_at" to FieldValue.serverTimestamp(),
                    "user_id" to uid,
                    "pickup_point_name" to s.pickupPointName,
                    "pickup_coordinates" to s.pickupCoordinates
                )

                newDoc.set(data).await()

                // --- Resetear el estado después de publicar ---
                _state.value = PostState(
                    categories = s.categories,
                    categoryId = s.categoryId,
                    categoryName = s.categoryName,
                    postedOk = true
                )

            } catch (t: Throwable) {
                _state.value = _state.value.copy(
                    isSaving = false,
                    errorMessage = t.message ?: "Error creating post"
                )
            }
        }
    }

    private suspend fun uploadToStorage(uid: String, uri: Uri): String? {
        return try {
            // Generar identificadores únicos
            val productId = UUID.randomUUID().toString()
            val fileName = "img_${System.currentTimeMillis()}.jpg"

            // Definir la ruta según tus reglas
            val ref = storage.reference
                .child("public/products/$uid/$productId/$fileName")

            // Agregar metadatos requeridos por tus reglas
            val metadata = storageMetadata {
                contentType = "image/jpeg"
                setCustomMetadata("ownerUid", uid)
            }

            // Subir el archivo con metadatos
            ref.putFile(uri, metadata).await()

            // Obtener la URL de descarga
            val url = ref.downloadUrl.await().toString()
            android.util.Log.d("PostVM", "✅ Uploaded URL: $url")
            url
        } catch (e: Exception) {
            android.util.Log.e("PostVM", "❌ uploadToStorage failed for $uri: ${e.message}", e)
            null
        }
    }
}
