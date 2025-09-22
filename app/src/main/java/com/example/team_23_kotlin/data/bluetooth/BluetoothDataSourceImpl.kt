// data/bluetooth/BluetoothDataSourceImpl.kt
package com.example.team_23_kotlin.data.bluetooth

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.team_23_kotlin.data.bluetooth.models.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class BluetoothDataSourceImpl @Inject constructor(
    private val context: Context
) : BluetoothDataSource {

    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()

    // UUID único para tu aplicación
    private val APP_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
    private val SERVICE_NAME = "MarketplaceConfirmation"

    // Estados internos
    private val _bluetoothState = MutableStateFlow(BluetoothState.DISABLED)
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceData>>(emptyList())
    private val _connectionState = MutableStateFlow(ConnectionResult(false, null))
    private val _incomingMessages = MutableStateFlow(MessageResult(false, null, null))

    // Sockets
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var connectedSocket: BluetoothSocket? = null

    // Flows públicos - CORREGIDOS con tipos explícitos
    override val bluetoothState: Flow<BluetoothState> = _bluetoothState.asStateFlow()
    override val discoveredDevices: Flow<List<BluetoothDeviceData>> = _discoveredDevices.asStateFlow()
    override val connectionState: Flow<ConnectionResult> = _connectionState.asStateFlow()
    override val incomingMessages: Flow<MessageResult> = _incomingMessages.asStateFlow()

    init {
        updateBluetoothState()
    }

    override fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled ?: false
    }

    override fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
                    ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        }
    }

    override suspend fun startDiscovery(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (!hasBluetoothPermissions()) {
            continuation.resume(Result.failure(SecurityException("No hay permisos de Bluetooth")))
            return@suspendCancellableCoroutine
        }

        try {
            bluetoothAdapter?.let { adapter ->
                // Verificar permisos antes de cada operación
                if (ActivityCompat.checkSelfPermission(context, getRequiredPermission()) != PackageManager.PERMISSION_GRANTED) {
                    continuation.resume(Result.failure(SecurityException("Sin permisos de Bluetooth")))
                    return@let
                }

                if (adapter.isDiscovering) {
                    adapter.cancelDiscovery()
                }

                // Limpiar dispositivos anteriores
                _discoveredDevices.value = emptyList()

                // Crear receiver para dispositivos descubiertos
                val discoveryReceiver = object : BroadcastReceiver() {
                    override fun onReceive(context: Context, intent: Intent) {
                        when (intent.action) {
                            BluetoothDevice.ACTION_FOUND -> {
                                val device: BluetoothDevice? = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                                device?.let { btDevice ->
                                    try {
                                        val deviceName = if (hasBluetoothPermissions()) {
                                            btDevice.name
                                        } else {
                                            "Dispositivo desconocido"
                                        }

                                        val deviceData = BluetoothDeviceData(
                                            name = deviceName,
                                            address = btDevice.address
                                        )

                                        val currentList = _discoveredDevices.value.toMutableList()
                                        if (!currentList.any { it.address == deviceData.address }) {
                                            currentList.add(deviceData)
                                            _discoveredDevices.value = currentList
                                        }
                                    } catch (e: SecurityException) {
                                        // Sin permisos para obtener nombre - crear con info limitada
                                        val deviceData = BluetoothDeviceData(
                                            name = "Dispositivo desconocido",
                                            address = btDevice.address
                                        )
                                        val currentList = _discoveredDevices.value.toMutableList()
                                        if (!currentList.any { it.address == deviceData.address }) {
                                            currentList.add(deviceData)
                                            _discoveredDevices.value = currentList
                                        }
                                    }
                                }
                            }
                            BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                                try {
                                    context.unregisterReceiver(this)
                                } catch (e: IllegalArgumentException) {
                                    // Receiver ya fue unregistered
                                }
                                if (continuation.isActive) {
                                    continuation.resume(Result.success(Unit))
                                }
                            }
                        }
                    }
                }

                // Registrar receiver
                val filter = IntentFilter().apply {
                    addAction(BluetoothDevice.ACTION_FOUND)
                    addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                }

                try {
                    context.registerReceiver(discoveryReceiver, filter)
                } catch (e: Exception) {
                    continuation.resume(Result.failure(e))
                    return@let
                }

                // Iniciar descubrimiento
                val started = adapter.startDiscovery()
                if (!started && continuation.isActive) {
                    try {
                        context.unregisterReceiver(discoveryReceiver)
                    } catch (e: IllegalArgumentException) {
                        // Receiver no estaba registrado
                    }
                    continuation.resume(Result.failure(Exception("No se pudo iniciar el descubrimiento")))
                }

            } ?: continuation.resume(Result.failure(Exception("Bluetooth no disponible")))

        } catch (e: Exception) {
            continuation.resume(Result.failure(e))
        }
    }

    override suspend fun stopDiscovery(): Result<Unit> {
        return try {
            if (hasBluetoothPermissions()) {
                if (ActivityCompat.checkSelfPermission(context, getRequiredPermission()) == PackageManager.PERMISSION_GRANTED) {
                    bluetoothAdapter?.cancelDiscovery()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun startAsServer(): Result<Unit> = suspendCancellableCoroutine { continuation ->
        if (!hasBluetoothPermissions()) {
            continuation.resume(Result.failure(SecurityException("No hay permisos de Bluetooth")))
            return@suspendCancellableCoroutine
        }

        Thread {
            try {
                _bluetoothState.value = BluetoothState.CONNECTING

                // Verificar permisos antes de usar Bluetooth
                if (ActivityCompat.checkSelfPermission(context, getRequiredPermission()) != PackageManager.PERMISSION_GRANTED) {
                    continuation.resume(Result.failure(SecurityException("Sin permisos de Bluetooth")))
                    return@Thread
                }

                serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(
                    SERVICE_NAME, APP_UUID
                )

                val socket = serverSocket?.accept() // Bloquea hasta conexión
                socket?.let {
                    connectedSocket = it
                    _bluetoothState.value = BluetoothState.CONNECTED

                    val deviceName = try {
                        if (hasBluetoothPermissions()) {
                            it.remoteDevice.name ?: "Dispositivo desconocido"
                        } else {
                            "Dispositivo desconocido"
                        }
                    } catch (e: SecurityException) {
                        "Dispositivo desconocido"
                    }

                    val deviceData = BluetoothDeviceData(
                        name = deviceName,
                        address = it.remoteDevice.address,
                        isConnected = true
                    )

                    _connectionState.value = ConnectionResult(true, deviceData)
                    startMessageListener(it)

                    if (continuation.isActive) {
                        continuation.resume(Result.success(Unit))
                    }
                }
            } catch (e: IOException) {
                _bluetoothState.value = BluetoothState.ERROR
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            } catch (e: SecurityException) {
                _bluetoothState.value = BluetoothState.ERROR
                if (continuation.isActive) {
                    continuation.resume(Result.failure(e))
                }
            }
        }.start()
    }

    override suspend fun connectToDevice(device: BluetoothDeviceData): Result<ConnectionResult> =
        suspendCancellableCoroutine { continuation ->
            if (!hasBluetoothPermissions()) {
                continuation.resume(Result.failure(SecurityException("No hay permisos de Bluetooth")))
                return@suspendCancellableCoroutine
            }

            Thread {
                try {
                    _bluetoothState.value = BluetoothState.CONNECTING

                    // Verificar permisos antes de usar Bluetooth
                    if (ActivityCompat.checkSelfPermission(context, getRequiredPermission()) != PackageManager.PERMISSION_GRANTED) {
                        continuation.resume(Result.failure(SecurityException("Sin permisos de Bluetooth")))
                        return@Thread
                    }

                    bluetoothAdapter?.cancelDiscovery()

                    val bluetoothDevice = bluetoothAdapter?.getRemoteDevice(device.address)
                    clientSocket = bluetoothDevice?.createRfcommSocketToServiceRecord(APP_UUID)
                    clientSocket?.connect()

                    clientSocket?.let { socket ->
                        connectedSocket = socket
                        _bluetoothState.value = BluetoothState.CONNECTED

                        val connectedDeviceData = device.copy(isConnected = true)
                        val result = ConnectionResult(true, connectedDeviceData)
                        _connectionState.value = result

                        startMessageListener(socket)

                        if (continuation.isActive) {
                            continuation.resume(Result.success(result))
                        }
                    }
                } catch (e: IOException) {
                    _bluetoothState.value = BluetoothState.ERROR
                    val result = ConnectionResult(false, null, e.message)
                    if (continuation.isActive) {
                        continuation.resume(Result.success(result))
                    }

                    try {
                        clientSocket?.close()
                    } catch (closeException: IOException) {
                        // Error al cerrar
                    }
                } catch (e: SecurityException) {
                    _bluetoothState.value = BluetoothState.ERROR
                    val result = ConnectionResult(false, null, "Sin permisos de Bluetooth")
                    if (continuation.isActive) {
                        continuation.resume(Result.success(result))
                    }
                }
            }.start()
        }

    override suspend fun disconnect(): Result<Unit> {
        return try {
            connectedSocket?.close()
            clientSocket?.close()
            serverSocket?.close()

            connectedSocket = null
            clientSocket = null
            serverSocket = null

            _bluetoothState.value = BluetoothState.DISCONNECTED
            _connectionState.value = ConnectionResult(false, null)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun sendMessage(message: String): Result<MessageResult> {
        return try {
            connectedSocket?.let { socket ->
                socket.outputStream.write(message.toByteArray())
                socket.outputStream.flush()

                val result = MessageResult(true, message)
                Result.success(result)
            } ?: Result.success(MessageResult(false, null, "No hay conexión activa"))

        } catch (e: IOException) {
            Result.success(MessageResult(false, null, e.message))
        }
    }

    private fun startMessageListener(socket: BluetoothSocket) {
        Thread {
            val inputStream: InputStream = socket.inputStream
            val buffer = ByteArray(1024)

            try {
                while (socket.isConnected) {
                    val bytes = inputStream.read(buffer)
                    val message = String(buffer, 0, bytes)

                    _incomingMessages.value = MessageResult(true, message)
                }
            } catch (e: IOException) {
                _bluetoothState.value = BluetoothState.DISCONNECTED
                _incomingMessages.value = MessageResult(false, null, "Conexión perdida")
            }
        }.start()
    }

    private fun updateBluetoothState() {
        _bluetoothState.value = when {
            bluetoothAdapter == null -> BluetoothState.DISABLED
            !bluetoothAdapter.isEnabled -> BluetoothState.DISABLED
            else -> BluetoothState.ENABLED
        }
    }

    override fun cleanup() {
        try {
            // Cleanup sin suspend - operaciones síncronas
            if (hasBluetoothPermissions() &&
                ActivityCompat.checkSelfPermission(context, getRequiredPermission()) == PackageManager.PERMISSION_GRANTED) {
                bluetoothAdapter?.cancelDiscovery()
            }

            // Cerrar conexiones
            connectedSocket?.close()
            clientSocket?.close()
            serverSocket?.close()

            connectedSocket = null
            clientSocket = null
            serverSocket = null

            _bluetoothState.value = BluetoothState.DISCONNECTED
            _connectionState.value = ConnectionResult(false, null)
        } catch (e: Exception) {
            // Cleanup silencioso
        }
    }

    // Helper para obtener el permiso requerido según la versión de Android
    private fun getRequiredPermission(): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            Manifest.permission.BLUETOOTH_CONNECT
        } else {
            Manifest.permission.BLUETOOTH
        }
    }
}