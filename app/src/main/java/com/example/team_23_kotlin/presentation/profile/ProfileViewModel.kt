package com.example.team_23_kotlin.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(ProfileState())
    val state: StateFlow<ProfileState> = _state.asStateFlow()

    init {
        loadUserProfile()
    }

    fun onEvent(event: ProfileEvent) {
        when (event) {
            is ProfileEvent.OnEditProfileClick -> {
                println("Edit Profile Clicked")
                // TODO: Navegar a pantalla de edición
            }

            is ProfileEvent.OnSalesClick -> {
                println("Sales Clicked")
                // TODO: Navegar a pantalla de ventas
            }

            is ProfileEvent.OnProductClick -> {
                //println("Clicked on product ID: ${event.productId}")
                // TODO: Mostrar detalle del producto o navegar
            }
        }
    }

    private fun loadUserProfile() {
        viewModelScope.launch {
            val userProfile = ProfileState(
                userName = "Sofia Ramirez",
                userHandle = "@sofia_ramirez",
                userRole = "Math Student",
                products = listOf(
                    Product("1", "Calculus Book", "calculus_book"),
                    Product("2", "Scientific Calculator", "scientific_calculator"),
                    Product("3", "Backpack", "backpack")
                )
            )
            _state.update { userProfile }
        }
    }
}
