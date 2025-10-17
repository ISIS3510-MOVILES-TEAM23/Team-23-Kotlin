package com.example.team_23_kotlin.presentation.post

data class Category(
    val id: String,
    val name: String
)

data class PostState(
    val title: String = "",
    val description: String = "",
    val price: String = "",

    val categoryId: String? = null,      // "c2"
    val categoryName: String? = null,    // "Bikes"

    // tokens: "uri:<...>" o "res:<id>"
    val photoTokens: List<String> = emptyList(),

    // --- categorías desde Firestore ---
    val categories: List<Category> = emptyList(),
    val categoriesLoading: Boolean = false,
    val categoriesError: String? = null,

    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val postedOk: Boolean = false
)
