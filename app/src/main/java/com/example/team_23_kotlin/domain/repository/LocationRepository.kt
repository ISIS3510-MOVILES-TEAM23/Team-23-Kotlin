package com.example.team_23_kotlin.domain.repository

import android.location.Location

interface LocationRepository {
    suspend fun getCurrentLocation(): Location?
    fun isInCampus(location: Location?): Boolean
}
