package com.example.team_23_kotlin.presentation.wishlist

import com.example.team_23_kotlin.data.posts.PostEntity

data class WishlistState(
	val ids: Set<String> = emptySet(),
	val items: List<PostEntity> = emptyList(),
	val isLoading: Boolean = false,
	val error: String? = null
)


