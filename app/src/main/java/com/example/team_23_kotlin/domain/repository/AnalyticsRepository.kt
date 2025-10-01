package com.example.team_23_kotlin.domain.repository

interface AnalyticsRepository {
    fun logProductSearch(
        query: String?,              // texto buscado (si aplica)
        selectedCategory: String?,   // p.ej. "Books"
        source: String,              // "search_bar" | "category_chip"
        suggested: List<String>? = null
    )

    fun logProductClick(
        postId: String,
        category: String,
        source: String
    )
}
