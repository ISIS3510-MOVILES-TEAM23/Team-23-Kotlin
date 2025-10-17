package com.example.team_23_kotlin.presentation.editprofile

data class EditProfileState(
    val name: String = "",
    val email: String = "",
    val phone: String? = "",
    val role: String? = "",
    val contactPreferences: List<String> = emptyList(),
    val isSaving: Boolean = false,
    val success: Boolean = false,
    val error: String? = null
)
