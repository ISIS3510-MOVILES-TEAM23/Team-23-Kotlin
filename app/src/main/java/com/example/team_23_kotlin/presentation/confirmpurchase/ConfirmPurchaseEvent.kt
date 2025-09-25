package com.example.team_23_kotlin.presentation.confirmpurchase

import com.example.team_23_kotlin.data.bluetooth.models.BluetoothDeviceData

// Eventos para el Bluetooth
sealed class ConfirmPurchaseEvent {
    object CheckBluetooth : ConfirmPurchaseEvent()
    object EnableBluetooth : ConfirmPurchaseEvent()
    object StartScanning : ConfirmPurchaseEvent()
    object StopScanning : ConfirmPurchaseEvent()
    object StartConnection : ConfirmPurchaseEvent()
    data class ConnectToDevice(val device: BluetoothDeviceInfo) : ConfirmPurchaseEvent()
    object SendConfirmation : ConfirmPurchaseEvent()
    object Cancel : ConfirmPurchaseEvent()
    object RequestPermissions : ConfirmPurchaseEvent()
    data class PermissionsResult(val granted: Boolean) : ConfirmPurchaseEvent()
    object ClearError : ConfirmPurchaseEvent()

}