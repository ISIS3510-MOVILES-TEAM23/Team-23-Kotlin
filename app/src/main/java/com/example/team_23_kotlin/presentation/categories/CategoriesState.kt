package com.example.team_23_kotlin.presentation.categories

import com.example.team_23_kotlin.data.categories.CategoryEntity

data class CategoriesState(
    val query: String = "",
    val isLoading: Boolean = false,
    val categories: List<CategoryEntity> = emptyList(),
    val error: String? = null
)
