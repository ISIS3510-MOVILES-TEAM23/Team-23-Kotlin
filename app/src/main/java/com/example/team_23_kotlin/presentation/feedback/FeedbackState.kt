package com.example.team_23_kotlin.presentation.feedback

data class FeedbackState(
    val purchaseId: String = "",
    val sellerId: String = "",
    val rating: Int = 0, // 0 = no seleccionado, 1-5 = estrellas
    val comment: String = "",
    val photoTokens: List<String> = emptyList(), // "uri:<...>" o URLs de Firebase
    val isSaving: Boolean = false,
    val uploadProgress: Float = 0f,
    val errorMessage: String? = null,
    val submitSuccess: Boolean = false,
    val existingFeedbackId: String? = null, // ID del feedback si ya existe
    val isEditMode: Boolean = false, // true si está editando, false si es nuevo
    val isLoading: Boolean = false // true mientras carga feedback existente
)
