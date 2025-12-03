package com.example.team_23_kotlin.presentation.reviews

sealed class ReviewsEvent {
    data class LoadReviews(val postId: String) : ReviewsEvent()
}
