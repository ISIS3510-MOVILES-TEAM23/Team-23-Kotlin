package com.example.team_23_kotlin.presentation.seller

sealed class SellerEvent {
    data class LoadSeller(val sellerId: String) : SellerEvent()
}
