package com.example.team_23_kotlin.presentation.confirmpurchase

import com.example.team_23_kotlin.data.bluetooth.models.BluetoothDeviceData

// Estado para la confirmación de compra
data class ConfirmPurchaseState(
    val chatId: String = "",
    val isBluetoothEnabled: Boolean = false,
    val isConnecting: Boolean = false,
    val isConnected: Boolean = false,
    val confirmationSent: Boolean = false,
    val purchaseConfirmed: Boolean = false,
    val error: String? = null,
    val nearbyDevices: List<BluetoothDeviceInfo> = emptyList(),
    val selectedDevice: BluetoothDeviceInfo? = null,
    val connectionStatus: ConnectionStatus = ConnectionStatus.IDLE,
    val permissionsGranted: Boolean = false
)

data class BluetoothDeviceInfo(
    val name: String?,
    val address: String,
    val isConnected: Boolean = false
)

enum class ConnectionStatus {
    IDLE,
    SCANNING,
    CONNECTING,
    CONNECTED,
    FAILED,
    DISCONNECTED
}

