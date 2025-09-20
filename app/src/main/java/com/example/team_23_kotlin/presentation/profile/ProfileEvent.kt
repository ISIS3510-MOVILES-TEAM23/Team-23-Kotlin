package com.example.team_23_kotlin.presentation.profile

sealed class ProfileEvent {
    object OnEditProfileClick : ProfileEvent()
    object OnSalesClick : ProfileEvent()
    object OnProductClick : ProfileEvent() // Ejemplo
}