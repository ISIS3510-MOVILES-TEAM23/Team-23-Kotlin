package com.example.team_23_kotlin.presentation.profile

data class ProfileState(
    val userName: String = "",
    val userHandle: String = "",
    val userRole: String = "",
    val email: String = "",
    val major: String = "", // 🎓 nuevo campo
    val photoUrl: String? = null,
    val isInCampus: Boolean = false,
    val isLoading: Boolean = true,
    val error: String? = null,
    val products: List<Product> = emptyList()
)

data class Product(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryName: String = "",
    val imageUrl: String = "",
    val price: Double = 0.0,
    val status: String = "",
    val createdAt: String = ""
)
