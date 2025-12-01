package com.example.team_23_kotlin.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.PostEntity
import com.example.team_23_kotlin.data.search.FirestoreSearchEventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class RecommendationsState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val posts: List<PostEntity> = emptyList()
)

class RecommendationsViewModel(
    private val postsRepo: FirestorePostsRepository,
    private val searchRepo: FirestoreSearchEventsRepository,
    private val userId: String
) : ViewModel() {

    private val _recs = MutableStateFlow<List<PostEntity>>(emptyList())
    val recs: StateFlow<List<PostEntity>> = _recs
    
    private val _state = MutableStateFlow(RecommendationsState())
    val state: StateFlow<RecommendationsState> = _state

    init {
        loadRecommendations()
    }

    // ✅ OPTIMIZADO: Suspend functions sin callbacks
    private fun loadRecommendations() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            
            try {
                // 1. Obtener categorías top del usuario
                val categories = searchRepo.getTopCategoriesForUserSuspend(userId)
                
                if (categories.isEmpty()) {
                    _recs.value = emptyList()
                    _state.value = _state.value.copy(
                        isLoading = false, 
                        posts = emptyList()
                    )
                    return@launch
                }
                
                // 2. Obtener posts de esas categorías
                val posts = postsRepo.getPostsByCategoriesSuspend(categories)
                
                _recs.value = posts
                _state.value = _state.value.copy(
                    isLoading = false,
                    posts = posts
                )
                
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading recommendations"
                )
            }
        }
    }
    
    fun refresh() {
        loadRecommendations()
    }
}
