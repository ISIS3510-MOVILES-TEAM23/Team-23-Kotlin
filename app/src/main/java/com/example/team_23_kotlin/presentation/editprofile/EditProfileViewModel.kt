package com.example.team_23_kotlin.presentation.editprofile

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.local.UserPreferences
import com.example.team_23_kotlin.utils.isNetworkAvailable
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
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

    private val prefs = UserPreferences(context)

    private val _state = MutableStateFlow(EditProfileState())
    val state: StateFlow<EditProfileState> = _state.asStateFlow()

    init {
        // 1️⃣ Cargar siempre primero lo local
        loadLocalData()
        // 2️⃣ Luego intentar sincronizar con Firestore si hay conexión
        syncLocalToRemote()
    }

    // ============================================================
    // 🔹 Cargar datos locales desde DataStore
    // ============================================================
    private fun loadLocalData() {
        viewModelScope.launch {
            prefs.userProfile.collect { local ->
                _state.update {
                    it.copy(
                        name = local.name,
                        phone = local.phone,
                        contactPreferences = local.contactPrefs
                    )
                }
            }
        }
    }

    // ============================================================
    // 🔹 Intentar sincronizar datos locales a Firestore al iniciar
    // ============================================================
    private fun syncLocalToRemote() {
        viewModelScope.launch {
            if (!isNetworkAvailable(context)) return@launch

            val uid = auth.currentUser?.uid ?: return@launch

            // ⬇️ antes: prefs.userProfile.collect { data -> ... }
            val data = prefs.userProfile.firstOrNull() ?: return@launch

            try {
                val updates = mapOf(
                    "name" to data.name,
                    "phone" to data.phone,
                    "contact_preferences" to data.contactPrefs,
                    "updated_at" to com.google.firebase.Timestamp.now()
                )
                firestore.collection("users").document(uid).update(updates).await()

                _state.update { it.copy(success = true, error = null) }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }


    // ============================================================
    // 🔹 Manejar eventos
    // ============================================================
    fun onEvent(event: EditProfileEvent) {
        when (event) {
            is EditProfileEvent.OnNameChanged -> _state.update { it.copy(name = event.value) }
            is EditProfileEvent.OnPhoneChanged -> _state.update { it.copy(phone = event.value) }
            is EditProfileEvent.OnContactPrefsChanged -> _state.update { it.copy(contactPreferences = event.value) }
            is EditProfileEvent.OnSaveClicked -> saveChanges()
            else -> {}
        }
    }

    // ============================================================
    // 🔹 Guardar cambios (offline/online)
    // ============================================================
    private fun saveChanges() {
        viewModelScope.launch {
            val uid = auth.currentUser?.uid ?: return@launch
            val s = _state.value

            _state.update { it.copy(isSaving = true, error = null, success = false) }

            // 🔸 1. Guardar SIEMPRE localmente
            prefs.saveProfile(
                name = s.name,
                phone = s.phone ?: "",
                prefs = s.contactPreferences
            )

            if (!isNetworkAvailable(context)) {
                // 🔸 2. Si no hay conexión, avisar que se guardó localmente
                _state.update {
                    it.copy(
                        isSaving = false,
                        success = true,
                        error = "Changes saved locally. Will sync when online."
                    )
                }
                return@launch
            }

            try {
                // 🔸 3. Si hay conexión, también subir a Firestore
                val updates = mapOf(
                    "name" to s.name,
                    "phone" to s.phone,
                    "contact_preferences" to s.contactPreferences,
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
                        error = "Failed to sync with server: ${e.message}"
                    )
                }
            }
        }
    }
}
