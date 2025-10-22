package com.example.team_23_kotlin.presentation.post

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.posts.CategoryEntity
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.Post
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

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

            PostEvent.AddPhotosClick -> { /* UI handled */ }

            PostEvent.SubmitClicked -> submitPost()
        }
    }

    // -----------------------------------------------------------
    // Carga de categorías (desde el repositorio)
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
    // Validaciones y creación del post
    // -----------------------------------------------------------
    private fun submitPost() {
        val s = _state.value

        // 🔸 Validaciones básicas
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

        // Proceso asíncrono
        viewModelScope.launch {
            try {
                _state.value = s.copy(isSaving = true, errorMessage = null)

                // Prepara lista de URIs de fotos
                val uris = s.photoTokens
                    .filter { it.startsWith("uri:") }
                    .map { Uri.parse(it.removePrefix("uri:")) }

                // Crea el objeto Post con los datos actuales
                val post = Post(
                    title = s.title,
                    description = s.description,
                    price = cleanPrice.toLong(),
                    category = FirebaseFirestore.getInstance()
                        .document("/categories/${s.categoryId}")
                )

                // Llama al repositorio
                val success = repository.createPost(post, uris)

                if (success) {
                    _state.value = PostState(
                        categories = s.categories,
                        categoryId = s.categoryId,
                        categoryName = s.categoryName,
                        postedOk = true
                    )
                } else {
                    _state.value = s.copy(isSaving = false, errorMessage = "Error creating post.")
                }

            } catch (t: Throwable) {
                _state.value = s.copy(
                    isSaving = false,
                    errorMessage = t.message ?: "Error creating post"
                )
            }
        }
    }
}
