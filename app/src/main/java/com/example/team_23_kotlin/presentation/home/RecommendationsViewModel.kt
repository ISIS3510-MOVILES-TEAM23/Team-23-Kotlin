package com.example.team_23_kotlin.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.PostEntity
import com.example.team_23_kotlin.data.search.FirestoreSearchEventsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class RecommendationsViewModel(
    private val postsRepo: FirestorePostsRepository,
    private val searchRepo: FirestoreSearchEventsRepository,
    private val userId: String
) : ViewModel() {

    private val _recs = MutableStateFlow<List<PostEntity>>(emptyList())
    val recs: StateFlow<List<PostEntity>> = _recs

    init {
        loadRecommendations()
    }

    private fun loadRecommendations() {
        viewModelScope.launch {
            searchRepo.getTopCategoriesForUser(userId) { categories ->
                if (categories.isEmpty()) {
                    _recs.value = emptyList()
                    return@getTopCategoriesForUser
                }
                postsRepo.getPostsByCategories(categories) { posts ->
                    _recs.value = posts
                }
            }
        }
    }
}
