package com.example.team_23_kotlin.data.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.util.Log
import com.example.team_23_kotlin.domain.repository.LocationRepository
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class LocationRepositoryImpl(private val context: Context) : LocationRepository {

    private val fusedLocationClient: FusedLocationProviderClient by lazy {
        LocationServices.getFusedLocationProviderClient(context)
    }

    @SuppressLint("MissingPermission")
    override suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { cont ->
        Log.d("LocationRepo", "Getting location...") // 👈 DEBUG

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                Log.d("LocationRepo", "Got location: $location") // 👈 DEBUG
                cont.resume(location)
            }
            .addOnFailureListener {
                Log.e("LocationRepo", "Failed to get location", it) // 👈 DEBUG
                cont.resume(null)
            }
    }

    override fun isInCampus(location: Location?): Boolean {
        val uniLatitude = 4.6014581
        val uniLongitude = -74.0687083
        val radiusMeters = 400

        return location?.let {
            val distance = FloatArray(1)
            Location.distanceBetween(
                it.latitude, it.longitude,
                uniLatitude, uniLongitude,
                distance
            )
            distance[0] <= radiusMeters
        } ?: false
    }

}
