package com.example.team_23_kotlin.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.posts.PostsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

data class HomePostUi(
    val id: String,
    val title: String,
    val description: String,
    val priceFormatted: String,
    val imageUrl: String
)

data class HomePostsState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val items: List<HomePostUi> = emptyList()
)

class HomePostsViewModel(
    private val repo: PostsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(HomePostsState())
    val state: StateFlow<HomePostsState> = _state

    init { refresh() }

    fun refresh(limit: Int = 20) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val posts = repo.getActivePosts(limit)
                val nf = NumberFormat.getCurrencyInstance(Locale("es", "CO"))
                val ui = posts.map {
                    HomePostUi(
                        id = it.id,
                        title = it.title,
                        description = it.description,
                        priceFormatted = nf.format(it.price),
                        imageUrl = it.images.firstOrNull().orEmpty()
                    )
                }
                _state.value = HomePostsState(isLoading = false, items = ui)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading posts"
                )
            }
        }
    }
}
