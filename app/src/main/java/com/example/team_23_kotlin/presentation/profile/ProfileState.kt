package com.example.team_23_kotlin.presentation.profile

data class ProfileState(
    val userName: String = "Sofia Ramirez",
    val userHandle: String = "@sofia_ramirez",
    val userRole: String = "Math Student",
    val isInCampus: Boolean = true, // 👈 NUEVO
    val products: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null
)


data class Product(
    val id: String,
    val name: String,
    val imageUrl: String // Puedes usar esto con Coil para cargar desde URL
)
