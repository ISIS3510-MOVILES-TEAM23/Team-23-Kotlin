package com.example.team_23_kotlin.presentation.editprofile

import EditProfileEvent
import EditProfileState
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class EditProfileViewModel : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.OnNameChanged -> {
                _state.update { it.copy(name = event.name) }
            }
            is EditProfileEvent.OnEmailChanged -> {
                _state.update { it.copy(email = event.email) }
            }
            is EditProfileEvent.OnPhoneChanged -> {
                _state.update { it.copy(phone = event.phone) }
            }
            is EditProfileEvent.OnSaveClicked -> {
                _state.update { it.copy(isSaving = true, errorMessage = null) }

                // 👉 Aquí iría la lógica real de guardado (API, DB, etc.)
                println("Saving profile: name=${_state.value.name}, email=${_state.value.email}, phone=${_state.value.phone}")

                // Simular guardado exitoso
                _state.update { it.copy(isSaving = false) }
            }
        }
    }
}
