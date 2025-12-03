package com.example.team_23_kotlin.presentation.product

import com.example.team_23_kotlin.data.posts.PostEntity

data class HotState(
	val isLoading: Boolean = true,
	val error: String? = null,
	val items: List<PostEntity> = emptyList()
)


