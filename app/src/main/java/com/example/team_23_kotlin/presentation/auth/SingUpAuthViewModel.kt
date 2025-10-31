package com.example.team_23_kotlin.presentation.auth

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.user.UserProfile
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

private val Context.signupDs by preferencesDataStore("signup_ecn")

data class SignUpUiState(
    val isOnline: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null,     // mensaje rojo encima del botón
    val draftEmail: String = "",
    val draftName: String = ""
)

@HiltViewModel
class SignUpAuthViewModel @Inject constructor(
    private val auth: FirebaseAuth,
    private val db: FirebaseFirestore,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val allowedDomain = "uniandes.edu.co"
    private val OFFLINE_MSG = "Sin conexión. Para registrarte por primera vez necesitas Internet."

    // Draft keys
    private val KEY_DRAFT_EMAIL = stringPreferencesKey("draft_email")
    private val KEY_DRAFT_NAME  = stringPreferencesKey("draft_name")

    private val _ui = MutableStateFlow(SignUpUiState())
    val ui: StateFlow<SignUpUiState> = _ui.asStateFlow()

    init {
        // Cargar drafts
        viewModelScope.launch {
            context.signupDs.data.collect { p ->
                _ui.update {
                    it.copy(
                        draftEmail = p[KEY_DRAFT_EMAIL] ?: "",
                        draftName  = p[KEY_DRAFT_NAME] ?: ""
                    )
                }
            }
        }
        observeConnectivity()
    }

    /** Guarda borrador para no perder lo escrito (name/email). */
    fun onDraftChange(email: String? = null, name: String? = null) {
        val newEmail = email ?: _ui.value.draftEmail
        val newName  = name  ?: _ui.value.draftName
        _ui.update { it.copy(draftEmail = newEmail, draftName = newName) }
        viewModelScope.launch {
            context.signupDs.edit { p ->
                p[KEY_DRAFT_EMAIL] = newEmail
                p[KEY_DRAFT_NAME]  = newName
            }
        }
    }

    /** Registro: si no hay Internet, error inmediato (sin loading). */
    fun register(
        form: SignUpForm,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val email = form.email.trim()
        val pass = form.password
        val name = form.name.trim()

        val domainOk = email.substringAfter("@", "").equals(allowedDomain, ignoreCase = true)
        if (!domainOk) { setError("Usa tu correo institucional @$allowedDomain", onError); return }
        if (pass.length < 6) { setError("La contraseña debe tener al menos 6 caracteres", onError); return }
        if (name.isBlank()) { setError("Ingresa tu nombre", onError); return }

        // 🚫 Sin Internet validado -> error inmediato, sin loading
        if (!hasValidInternet()) {
            setError(OFFLINE_MSG, onError)
            return
        }

        viewModelScope.launch {
            _ui.update { it.copy(isLoading = true, error = null) }
            try {
                // 1) Auth
                auth.createUserWithEmailAndPassword(email, pass).await()

                // 2) Verificación (best-effort)
                runCatching { auth.currentUser?.sendEmailVerification()?.await() }

                // 3) Perfil Firestore
                val uid = auth.currentUser?.uid ?: throw IllegalStateException("No UID")
                val profile = UserProfile(
                    email = email,
                    is_verified = false,
                    name = name
                    // created_at via @ServerTimestamp en tu modelo
                )
                db.collection("users").document(uid).set(profile).await()

                _ui.update { it.copy(isLoading = false, error = null) }
                onSuccess()
            } catch (e: Exception) {
                _ui.update { it.copy(isLoading = false) }
                val msg = when (e) {
                    is FirebaseNetworkException -> "Se perdió la conexión. Intenta de nuevo cuando tengas Internet."
                    else -> mapFirebaseError(e.message ?: "Error al registrar")
                }
                setError(msg, onError)
            }
        }
    }

    /** ==== Conectividad (VALIDATED) ==== */
    private fun observeConnectivity() {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val flow = callbackFlow<Boolean> {
            val initial = hasValidInternet()
            trySend(initial)
            val cb = object : ConnectivityManager.NetworkCallback() {
                override fun onAvailable(network: Network) { trySend(true) }
                override fun onLost(network: Network) { trySend(hasValidInternet()) }
                override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) {
                    trySend(
                        caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                    )
                }
            }
            cm.registerDefaultNetworkCallback(cb)
            awaitClose { cm.unregisterNetworkCallback(cb) }
        }.distinctUntilChanged()

        viewModelScope.launch {
            flow.collect { online ->
                // Solo actualizamos conectividad; NO ponemos error automático.
                _ui.update { it.copy(isOnline = online) }
                // Si vuelve la conexión y el error actual era el de offline, limpiarlo:
                if (online && _ui.value.error == OFFLINE_MSG) {
                    _ui.update { it.copy(error = null) }
                }
            }
        }
    }

    private fun hasValidInternet(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val nw = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(nw) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private fun setError(msg: String, onError: (String) -> Unit) {
        _ui.update { it.copy(error = msg) }
        onError(msg)
    }

    private fun mapFirebaseError(raw: String): String = when {
        raw.contains("email address is already in use", true) -> "Este correo ya está registrado."
        raw.contains("badly formatted", true) -> "Correo inválido."
        raw.contains("WEAK_PASSWORD", true) || raw.contains("password", true) -> "Contraseña débil."
        else -> raw
    }
}
