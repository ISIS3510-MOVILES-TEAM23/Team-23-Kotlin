// domain/repository/BluetoothRepository.kt
package com.example.team_23_kotlin.domain.repository

import com.example.team_23_kotlin.data.bluetooth.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface BluetoothRepository {
    val bluetoothState: Flow<BluetoothState>
    val discoveredDevices: Flow<List<BluetoothDeviceData>>
    val connectionState: StateFlow<ConnectionResult>
    val incomingMessages: Flow<MessageResult>

    suspend fun isBluetoothAvailable(): Boolean
    suspend fun hasRequiredPermissions(): Boolean
    suspend fun startDeviceDiscovery(): Result<Unit>
    suspend fun stopDeviceDiscovery(): Result<Unit>
    suspend fun connectToDevice(device: BluetoothDeviceData): Result<ConnectionResult>
    suspend fun startListening(): Result<Unit>
    suspend fun sendPurchaseConfirmation(chatId: String): Result<MessageResult>

    suspend fun sendPurchaseAccepted(): Result<MessageResult>
    suspend fun disconnect(): Result<Unit>
    fun cleanup()
}
