package com.example.team_23_kotlin.domain.usecase

import android.util.Log
import com.example.team_23_kotlin.domain.repository.LocationRepository
import kotlin.math.*

class CheckInCampusUseCase(private val locationRepository: LocationRepository) {

    companion object {
        private const val CAMPUS_LAT = 4.601458
        private const val CAMPUS_LON = -74.0713693
        private const val CAMPUS_RADIUS_METERS = 400.0
    }

    suspend operator fun invoke(): Boolean {
        val currentLocation = locationRepository.getCurrentLocation()
        Log.d("UseCase", "Current location: $currentLocation")

        if (currentLocation == null) return false


        val result = FloatArray(1)
        android.location.Location.distanceBetween(
            currentLocation.latitude, currentLocation.longitude,
            CAMPUS_LAT, CAMPUS_LON, result
        )
        return result[0] <= CAMPUS_RADIUS_METERS
    }
}
