package com.example.team_23_kotlin.data.bluetooth.models


data class ConnectionResult(
    val isConnected: Boolean,
    val device: BluetoothDeviceData? = null,
    val error: String? = null
) {

    val isSuccess: Boolean
        get() = device != null && error == null
    companion object {
        fun success(device: BluetoothDeviceData): ConnectionResult =
            ConnectionResult(true, device)


        fun failure(error: String): ConnectionResult =
            ConnectionResult(false, null, error)


        fun default(): ConnectionResult = ConnectionResult(false)
    }
}