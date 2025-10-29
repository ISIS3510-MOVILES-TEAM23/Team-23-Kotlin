package com.example.team_23_kotlin.presentation.editprofile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.utils.isNetworkAvailable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    init {
        loadUserData()
    }

    // ============================================================
    // 🔹 Cargar datos del usuario (con cache Firestore)
    // ============================================================
    private fun loadUserData() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch

            try {
                val doc = firestore.collection("users").document(uid).get().await()
                if (doc.exists()) {
                    val data = doc.data ?: return@launch
                    _state.update {
                        it.copy(
                            name = data["name"] as? String ?: "",
                            email = data["email"] as? String ?: "",
                            phone = data["phone"] as? String ?: "",
                            role = data["role"] as? String ?: "",
                            contactPreferences = (data["contact_preferences"] as? List<String>) ?: emptyList()
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update { it.copy(error = e.message ?: "Error loading profile") }
            }
        }
    }

    // ============================================================
    // 🔹 Manejo de eventos
    // ============================================================
    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.OnNameChanged -> _state.update { it.copy(name = event.value) }
            is EditProfileEvent.OnEmailChanged -> _state.update { it.copy(email = event.value) }
            is EditProfileEvent.OnPhoneChanged -> _state.update { it.copy(phone = event.value) }
            is EditProfileEvent.OnSaveClicked -> saveChanges()
            is EditProfileEvent.OnContactPrefsChanged -> _state.update { it.copy(contactPreferences = event.value) }
        }
    }

    // ============================================================
    // 🔹 Guardar cambios (con verificación de red)
    // ============================================================
    private fun saveChanges() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch

            // 🔸 Verificar conexión antes de guardar
            if (!isNetworkAvailable(context)) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        success = false,
                        error = "No internet connection. Try again when you're online."
                    )
                }
                return@launch
            }

            _state.update { it.copy(isSaving = true, error = null, success = false) }

            try {
                val updates = mapOf(
                    "name" to _state.value.name,
                    "email" to _state.value.email,
                    "phone" to _state.value.phone,
                    "contact_preferences" to _state.value.contactPreferences,
                    "updated_at" to com.google.firebase.Timestamp.now()
                )

                firestore.collection("users").document(uid).update(updates).await()

                _state.update {
                    it.copy(
                        isSaving = false,
                        success = true,
                        error = null
                    )
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _state.update {
                    it.copy(
                        isSaving = false,
                        success = false,
                        error = "Failed to update profile: ${e.message}"
                    )
                }
            }
        }
    }
}
