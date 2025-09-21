package com.example.team_23_kotlin.presentation.product

sealed class ProductEvent {
    data class LoadProduct(val productId: String) : ProductEvent()
}
