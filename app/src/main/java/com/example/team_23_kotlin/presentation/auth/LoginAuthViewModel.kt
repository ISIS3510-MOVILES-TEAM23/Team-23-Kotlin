// presentation/auth/LoginAuthViewModel.kt
package com.example.team_23_kotlin.presentation.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.local.StoredUser
import com.example.team_23_kotlin.data.local.UserSessionStorage
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import com.google.firebase.FirebaseNetworkException

@HiltViewModel
class LoginAuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    @ApplicationContext private val context: Context,
    private val userSessionStorage: UserSessionStorage
) : ViewModel() {

    private val allowedDomain = "uniandes.edu.co"

    fun signIn(
        email: String,
        password: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val e = email.trim()
        if (!e.substringAfter("@", "").equals(allowedDomain, ignoreCase = true)) {
            onError("Usa tu correo institucional @$allowedDomain")
            return
        }
        if (password.isBlank()) {
            onError("Ingresa tu contraseña")
            return
        }

        if (!hasValidInternet()) {
            onError("Sin conexión a Internet. No es posible iniciar sesión por primera vez.")
            return
        }

        viewModelScope.launch {
            try {
                auth.signInWithEmailAndPassword(e, password).await()

                // Bloquear si no verificó correo (opcional, recomendado)
                val user = auth.currentUser
                if (user?.isEmailVerified != true) {
                    auth.signOut()
                    userSessionStorage.clear()
                    onError("Verifica tu correo antes de iniciar sesión.")
                    return@launch
                }

                userSessionStorage.save(
                    StoredUser(
                        uid = user.uid,
                        email = user.email,
                        displayName = user.displayName,
                        photoUrl = user.photoUrl?.toString(),
                        isEmailVerified = user.isEmailVerified
                    )
                )
                Log.d(TAG, "Usuario persistido localmente con uid=${user.uid}")

                onSuccess()
            } catch (ex: Exception) {
                // Segundo filtro: si igual llegó a dispararse Firebase, unificamos el mensaje
                val msg = when (ex) {
                    is FirebaseNetworkException -> "Sin conexión a Internet. Intenta de nuevo cuando tengas señal."
                    else -> mapFirebaseError(ex.message ?: "No se pudo iniciar sesión")
                }
                onError(msg)
            }
        }
    }

    private fun hasValidInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        // INTERNET + VALIDATED asegura que hay salida real (no solo Wi-Fi sin Internet)
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun mapFirebaseError(raw: String): String = when {
        raw.contains("password is invalid", true) -> "Contraseña incorrecta."
        raw.contains("no user record", true) -> "No existe una cuenta con ese correo."
        raw.contains("badly formatted", true) -> "Correo inválido."
        else -> raw
    }

    private companion object {
        const val TAG = "LoginAuthViewModel"
    }
}
