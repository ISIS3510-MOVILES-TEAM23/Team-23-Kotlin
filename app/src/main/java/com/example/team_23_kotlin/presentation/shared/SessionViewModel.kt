package com.example.team_23_kotlin.presentation.shared

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.local.StoredUser
import com.example.team_23_kotlin.data.local.UserSessionStorage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SessionState(
    val isLoading: Boolean = true,
    val user: StoredUser? = null
)

@HiltViewModel
class SessionViewModel @Inject constructor(
    private val sessionStorage: UserSessionStorage
) : ViewModel() {

    private val _state = MutableStateFlow(SessionState())
    val state: StateFlow<SessionState> = _state

    init {
        loadUser()
    }

    fun loadUser() {
        viewModelScope.launch {
            val saved = sessionStorage.get()
            _state.value = SessionState(isLoading = false, user = saved)
        }
    }

    fun clearSession() {
        viewModelScope.launch {
            sessionStorage.clear()
            _state.value = SessionState(isLoading = false, user = null)
        }
    }

    fun updateSession(user: StoredUser) {
        viewModelScope.launch {
            sessionStorage.save(user)
            _state.value = SessionState(isLoading = false, user = user)
        }
    }
}

