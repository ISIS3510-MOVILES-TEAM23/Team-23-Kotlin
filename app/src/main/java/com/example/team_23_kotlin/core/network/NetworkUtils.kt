package com.example.team_23_kotlin.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log

fun Context.hasInternetConnection(): Boolean {
    return try {
        val connectivityManager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    } catch (e: Exception) {
        Log.e("NetworkUtils", "Error checking connectivity", e)
        false
    }
}


