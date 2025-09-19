// presentation/auth/LoginViewModel.kt
package com.example.team_23_kotlin.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    var state = LoginState()
        private set

    fun onEvent(e: LoginEvent) {
        when (e) {
            is LoginEvent.OnEmailChanged -> {
                state = state.copy(email = e.value, emailError = null)
            }
            is LoginEvent.OnPasswordChanged -> {
                state = state.copy(password = e.value, passwordError = null)
            }
            LoginEvent.OnTogglePassword -> {
                state = state.copy(isPasswordVisible = !state.isPasswordVisible)
            }
            LoginEvent.OnClearErrors -> {
                state = state.copy(emailError = null, passwordError = null)
            }
            LoginEvent.OnGoogle -> {
                // TODO: dispara tu flujo de Google One Tap / Firebase
            }
            LoginEvent.OnSubmit -> {
                // Validación simple + “fake” login
                val emailOk = state.email.contains("@")
                val passOk = state.password.length >= 6
                state = state.copy(
                    emailError = if (!emailOk) "Email inválido" else null,
                    passwordError = if (!passOk) "Mínimo 6 caracteres" else null
                )
                if (emailOk && passOk) {
                    viewModelScope.launch {
                        state = state.copy(isLoading = true)
                        // TODO: llama a tu repo/auth
                        delay(800)
                        state = state.copy(isLoading = false)
                    }
                }
            }
        }
    }
}
