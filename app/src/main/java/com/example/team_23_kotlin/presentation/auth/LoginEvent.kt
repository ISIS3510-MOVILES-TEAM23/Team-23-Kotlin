// presentation/auth/LoginEvent.kt
package com.example.team_23_kotlin.presentation.auth

sealed class LoginEvent {
    data class OnEmailChanged(val value: String): LoginEvent()
    data class OnPasswordChanged(val value: String): LoginEvent()
    object OnTogglePassword : LoginEvent()
    object OnSubmit : LoginEvent()
    object OnGoogle : LoginEvent()
    object OnClearErrors : LoginEvent()
}
