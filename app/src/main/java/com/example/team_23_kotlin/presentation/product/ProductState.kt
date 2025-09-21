package com.example.team_23_kotlin.presentation.product

data class ProductState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val product: ProductUiModel? = null
)

data class ProductUiModel(
    val id: String,
    val title: String,
    val description: String,
    val price: String,
    val imageUrl: String,
    val sellerName: String,
    val sellerRating: Float
)
