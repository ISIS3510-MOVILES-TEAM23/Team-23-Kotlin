package com.example.team_23_kotlin.presentation.reviews

data class ReviewsState(
    val isLoading: Boolean = false,
    val reviews: List<ReviewUi> = emptyList(),
    val errorMessage: String? = null
)

data class ReviewUi(
    val rating: Int,
    val comment: String,
    val images: List<String>,
    val reviewerName: String,
    val reviewerImage: String? = null,
    val createdAt: Long
)