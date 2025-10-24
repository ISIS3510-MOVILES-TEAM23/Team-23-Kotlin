package com.example.team_23_kotlin.presentation.home

data class HomePopupState(
    val isLoading: Boolean = false,
    val favoriteCategory: String? = null,
    val error: String? = null
)