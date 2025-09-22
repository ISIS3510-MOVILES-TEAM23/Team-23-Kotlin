package com.example.team_23_kotlin.data.bluetooth

import com.example.team_23_kotlin.data.bluetooth.models.*
import kotlinx.coroutines.flow.Flow

interface BluetoothDataSource {

    // Estado del Bluetooth
    val bluetoothState: Flow<BluetoothState>
    val discoveredDevices: Flow<List<BluetoothDeviceData>>
    val connectionState: Flow<ConnectionResult>
    val incomingMessages: Flow<MessageResult>

    // Verificaciones
    fun isBluetoothEnabled(): Boolean
    fun hasBluetoothPermissions(): Boolean

    // Operaciones de descubrimiento
    suspend fun startDiscovery(): Result<Unit>
    suspend fun stopDiscovery(): Result<Unit>

    // Operaciones de conexión
    suspend fun startAsServer(): Result<Unit>
    suspend fun connectToDevice(device: BluetoothDeviceData): Result<ConnectionResult>
    suspend fun disconnect(): Result<Unit>

    // Operaciones de mensajería
    suspend fun sendMessage(message: String): Result<MessageResult>

    // Cleanup
    fun cleanup()
}