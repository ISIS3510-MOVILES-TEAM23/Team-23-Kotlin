package com.example.team_23_kotlin.presentation.seller

data class SellerState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val seller: SellerUiModel? = null
)

data class SellerUiModel(
    val id: String,
    val name: String,
    val role: String,
    val rating: Float,
    val profileImageUrl: String?,
    val isInCampus: Boolean,
    val products: List<ProductItem> = emptyList()
)

data class ProductItem(
    val id: String,
    val title: String,
    val price: String,
    val imageUrl: String
)

