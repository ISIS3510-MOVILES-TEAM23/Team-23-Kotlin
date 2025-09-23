// presentation/auth/SignUpAuthViewModel.kt
package com.example.team_23_kotlin.presentation.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.user.UserProfile
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

@HiltViewModel
class SignUpAuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore
) : ViewModel() {

    private val allowedDomain = "uniandes.edu.co"

    fun register(
        form: SignUpForm,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val email = form.email.trim()
        val pass = form.password

        val domainOk = email.substringAfter("@", "").equals(allowedDomain, ignoreCase = true)
        if (!domainOk) { onError("Usa tu correo institucional @$allowedDomain"); return }
        if (pass.length < 6) { onError("La contraseña debe tener al menos 6 caracteres"); return }
        if (form.name.isBlank()) { onError("Ingresa tu nombre"); return }

        viewModelScope.launch {
            try {
                // 1) Crear en Auth
                auth.createUserWithEmailAndPassword(email, pass).await()

                // 2) Enviar verificación (recomendado)
                val firebaseUser = auth.currentUser
                firebaseUser?.sendEmailVerification()?.await()

                // 3) Guardar perfil en Firestore (sin password)
                val uid = firebaseUser?.uid ?: throw IllegalStateException("No UID")
                val profile = UserProfile(
                    // created_at = serverTimestamp por @ServerTimestamp del modelo
                    email = email,
                    is_verified = false, // hasta que confirme email
                    name = form.name,
                )
                db.collection("users").document(uid).set(profile).await()

                onSuccess()
            } catch (e: Exception) {
                onError(mapFirebaseError(e.message ?: "Error al registrar"))
            }
        }
    }

    private fun mapFirebaseError(raw: String): String = when {
        raw.contains("email address is already in use", true) -> "Este correo ya está registrado."
        raw.contains("badly formatted", true) -> "Correo inválido."
        raw.contains("WEAK_PASSWORD", true) || raw.contains("password", true) -> "Contraseña débil."
        else -> raw
    }
}
