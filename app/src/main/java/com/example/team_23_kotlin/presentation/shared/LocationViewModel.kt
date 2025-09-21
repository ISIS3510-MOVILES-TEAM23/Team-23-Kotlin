package com.example.team_23_kotlin.presentation.shared

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.domain.repository.LocationRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LocationViewModel @Inject constructor(
    app: Application,
    private val locationRepo: LocationRepository
) : AndroidViewModel(app) {

    private val _isInCampus = MutableStateFlow<Boolean?>(null)
    val isInCampus: StateFlow<Boolean?> = _isInCampus

    init {
        fetchAndCheckCampusLocation()
    }

    fun fetchAndCheckCampusLocation() {
        viewModelScope.launch {

            Log.d("LocationVM", "🔍 Empezando a obtener ubicación...")

            val location = locationRepo.getCurrentLocation()
            Log.d("LocationVM", "📍 Ubicación obtenida: $location")

            val result = locationRepo.isInCampus(location)
            Log.d("LocationVM", "🏫 ¿Está en campus?: $result")

            _isInCampus.value = result
        }
    }
}
