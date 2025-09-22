package com.example.team_23_kotlin.data.bluetooth.models


data class BluetoothDeviceData(
    val name: String?,
    val address: String,
    val isConnected: Boolean = false
)