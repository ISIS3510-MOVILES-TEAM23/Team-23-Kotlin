package com.example.team_23_kotlin.presentation.post

data class Category(
    val id: String,
    val name: String
)

data class PickupPoint(
    val name: String,
    val coordinates: String
)

val pickupPoints = listOf(
    PickupPoint("Edificio SD", "4.6030761,-74.0676922"),
    PickupPoint("Edificio ML", "4.6030761,-74.0676922"),
    PickupPoint("El Bobo", "4.6012414,-74.0659291"),
    PickupPoint("La Caneca", "4.6001055,-74.0647088")
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
    val postedOk: Boolean = false,

    val pickupPointName: String? = null,
    val pickupCoordinates: String? = null,

    )
