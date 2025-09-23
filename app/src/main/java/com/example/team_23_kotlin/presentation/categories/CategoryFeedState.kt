package com.example.team_23_kotlin.presentation.categories

import com.example.team_23_kotlin.data.posts.PostEntity

data class CategoryFeedState(
    val title: String = "",
    val isLoading: Boolean = true,
    val error: String? = null,
    val items: List<PostEntity> = emptyList()
)
