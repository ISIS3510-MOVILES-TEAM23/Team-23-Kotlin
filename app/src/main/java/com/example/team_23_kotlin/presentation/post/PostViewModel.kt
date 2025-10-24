package com.example.team_23_kotlin.presentation.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.posts.CategoryEntity
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.Post
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PostViewModel(
    private val repository: FirestorePostsRepository = FirestorePostsRepository(
        FirebaseFirestore.getInstance()
    )
) : ViewModel() {

    private val _state = MutableStateFlow(PostState())
    val state: StateFlow<PostState> = _state

    private val firestore = FirebaseFirestore.getInstance()
    private val uid = FirebaseAuth.getInstance().currentUser?.uid

    init {
        loadCategories()
    }

    // -----------------------------------------------------------
    // Manejo de eventos desde la UI
    // -----------------------------------------------------------
    fun onEvent(e: PostEvent) {
        when (e) {
            is PostEvent.TitleChanged ->
                _state.update { it.copy(title = e.value, errorMessage = null) }

            is PostEvent.DescriptionChanged ->
                _state.update { it.copy(description = e.value, errorMessage = null) }

            is PostEvent.PriceChanged ->
                _state.update { it.copy(price = e.value, errorMessage = null) }

            is PostEvent.CategorySelected ->
                _state.update {
                    it.copy(
                        categoryId = e.id,
                        categoryName = e.name,
                        errorMessage = null
                    )
                }

            is PostEvent.PhotoAdded -> {
                val newList = _state.value.photoTokens.toMutableList().apply {
                    add("uri:${e.uri}")
                }
                _state.update { it.copy(photoTokens = newList) }
            }

            is PostEvent.PhotoRemovedAt -> {
                val newList = _state.value.photoTokens.toMutableList().apply {
                    if (e.index in indices) removeAt(e.index)
                }
                _state.update { it.copy(photoTokens = newList) }
            }

            is PostEvent.PickupPointSelected -> {
                _state.update {
                    it.copy(
                        pickupPointName = e.name,
                        pickupCoordinates = e.coordinates
                    )
                }
            }

            is PostEvent.ClearForm -> {
                _state.update {
                    it.copy(
                        title = "",
                        description = "",
                        price = "",
                        categoryId = null,
                        categoryName = null,
                        photoTokens = emptyList(),
                        pickupPointName = null,
                        pickupCoordinates = null
                    )
                }
            }

            PostEvent.ReloadCategories -> loadCategories()
            PostEvent.SubmitClicked -> submitPost()
            PostEvent.AddPhotosClick -> { /* handled in UI */ }
        }
    }

    // -----------------------------------------------------------
    // Cargar categorías
    // -----------------------------------------------------------
    private fun loadCategories() {
        viewModelScope.launch {
            _state.update { it.copy(categoriesLoading = true, categoriesError = null) }
            try {
                val list: List<CategoryEntity> = repository.getCategories()
                val sorted = list.sortedBy { it.name }

                val current = _state.value
                val selectedStillExists = sorted.any { it.id == current.categoryId }

                _state.update {
                    it.copy(
                        categories = sorted.map { c -> Category(c.id, c.name) },
                        categoriesLoading = false,
                        categoryId = if (selectedStillExists) current.categoryId else null,
                        categoryName = if (selectedStillExists) current.categoryName else null
                    )
                }
            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        categoriesLoading = false,
                        categoriesError = t.message ?: "Error loading categories"
                    )
                }
            }
        }
    }

    // -----------------------------------------------------------
    // Crear publicación
    // -----------------------------------------------------------
    private fun submitPost() {
        val s = _state.value

        // 🔸 Validaciones básicas
        when {
            s.title.isBlank() -> {
                _state.update { it.copy(errorMessage = "Please enter a title.") }
                return
            }

            s.price.filter { it.isDigit() }.isBlank() -> {
                _state.update { it.copy(errorMessage = "Please enter a valid price.") }
                return
            }

            s.categoryId == null -> {
                _state.update { it.copy(errorMessage = "Please select a category.") }
                return
            }

            s.pickupPointName.isNullOrBlank() -> {
                _state.update { it.copy(errorMessage = "Please select a pickup point.") }
                return
            }
        }

        // 🔹 Proceso asíncrono
        viewModelScope.launch {
            try {
                _state.update { it.copy(isSaving = true, errorMessage = null) }

                // Prepara URIs de fotos
                val uris = s.photoTokens
                    .filter { it.startsWith("uri:") }
                    .map { Uri.parse(it.removePrefix("uri:")) }

                // Referencia a la categoría
                val categoryRef = firestore.document("/categories/${s.categoryId}")

                // 🔹 Construir objeto Post
                val post = Post(
                    title = s.title,
                    description = s.description,
                    price = s.price.filter { it.isDigit() }.toLong(),
                    category = categoryRef,
                    pickupName = s.pickupPointName,
                    pickupCoords = s.pickupCoordinates
                )

                // 🔹 Crear publicación en el repositorio
                val success = repository.createPost(post, uris)

                if (success) {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            postedOk = true
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            isSaving = false,
                            errorMessage = "Error creating post."
                        )
                    }
                }

            } catch (t: Throwable) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = t.message ?: "Error creating post"
                    )
                }
            }
        }
    }
}
