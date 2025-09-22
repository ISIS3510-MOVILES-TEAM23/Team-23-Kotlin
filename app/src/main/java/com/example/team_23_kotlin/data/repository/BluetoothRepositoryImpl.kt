package com.example.team_23_kotlin.data.repository

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.example.team_23_kotlin.data.bluetooth.models.*
import com.example.team_23_kotlin.domain.repository.BluetoothRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton

class BluetoothRepositoryImpl @Inject constructor(
    private val context: Context
) : BluetoothRepository {
    companion object {
        private const val TAG = "BluetoothRepo"
    }


    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var connectedSocket: BluetoothSocket? = null

    private val _bluetoothState = MutableStateFlow(BluetoothState.DISABLED)
    override val bluetoothState: Flow<BluetoothState> = _bluetoothState

    private val _discoveredDevices = MutableStateFlow<List<BluetoothDeviceData>>(emptyList())
    override val discoveredDevices: StateFlow<List<BluetoothDeviceData>> = _discoveredDevices

    private val _connectionState = MutableStateFlow(ConnectionResult.default())
    override val connectionState: StateFlow<ConnectionResult> = _connectionState

    private val _incomingMessages = MutableStateFlow(MessageResult.default())
    override val incomingMessages: StateFlow<MessageResult> = _incomingMessages

    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // SPP UUID

    private var discoveryJob: Job? = null
    private var listenJob: Job? = null
    private var readJob: Job? = null

    override suspend fun isBluetoothAvailable(): Boolean {
        val available = bluetoothAdapter != null && bluetoothAdapter.isEnabled
        Log.d(TAG, "isBluetoothAvailable: $available")
        _bluetoothState.value = if (available) BluetoothState.ENABLED else BluetoothState.DISABLED
        return available
    }

    override suspend fun hasRequiredPermissions(): Boolean {
        // Aquí puedes implementar verificación si quieres más lógica
        return true
    }

    override suspend fun startDeviceDiscovery(): Result<Unit> {
        return try {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                return Result.failure(SecurityException("No se tienen permisos de BLUETOOTH_SCAN"))
            }

            bluetoothAdapter?.startDiscovery()
            discoveryJob = CoroutineScope(Dispatchers.IO).launch {
                val bondedDevices = bluetoothAdapter?.bondedDevices?.map {
                    BluetoothDeviceData(name = it.name, address = it.address)
                } ?: emptyList()
                _discoveredDevices.emit(bondedDevices)
            }
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(SecurityException("Permisos de Bluetooth rechazados: ${e.message}"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


    override suspend fun stopDeviceDiscovery(): Result<Unit> {
        return try {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED

            if (permissionGranted) {
                bluetoothAdapter?.cancelDiscovery()
            } else {
                return Result.failure(SecurityException("Permiso BLUETOOTH_SCAN no concedido"))
            }

            discoveryJob?.cancel()
            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }




    override suspend fun connectToDevice(device: BluetoothDeviceData): Result<ConnectionResult> {
        Log.d(TAG, "Intentando conectar con ${device.name} (${device.address})")
        return try {
            // Verificar permiso necesario (según API)
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                return Result.failure(SecurityException("Permiso BLUETOOTH_CONNECT no concedido"))
            }

            val deviceObj = bluetoothAdapter?.getRemoteDevice(device.address)
            clientSocket = deviceObj?.createRfcommSocketToServiceRecord(uuid)

            bluetoothAdapter?.cancelDiscovery()
            clientSocket?.connect()
            connectedSocket = clientSocket

            _bluetoothState.emit(BluetoothState.CONNECTED)
            _connectionState.emit(ConnectionResult.success(device))

            startReadingFromSocket(clientSocket!!)
            Log.d(TAG, "Conexión exitosa con ${device.address}")
            Result.success(ConnectionResult.success(device))


        } catch (e: SecurityException) {
            Log.e(TAG, "Error de permisos: ${e.message}")
            _connectionState.emit(ConnectionResult.failure("Sin permisos de Bluetooth"))
            Result.failure(e)
        } catch (e: Exception) {
            Log.e(TAG, "Error al conectar: ${e.message}")
            _connectionState.emit(ConnectionResult.failure(e.message ?: "Error"))
            Result.failure(e)
        }
    }


    override suspend fun startListening(): Result<Unit> {
        Log.d(TAG, "Iniciando servidor Bluetooth...")
        return try {
            val permissionGranted = ActivityCompat.checkSelfPermission(
                context,
                android.Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED

            if (!permissionGranted) {
                return Result.failure(SecurityException("Permiso BLUETOOTH_CONNECT no concedido"))
            }

            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord("Mercandes", uuid)

            listenJob = CoroutineScope(Dispatchers.IO).launch {
                try {
                    Log.d(TAG, "Esperando conexión entrante...")
                    val socket = serverSocket?.accept()
                    socket?.let {
                        Log.d(TAG, "Conexión entrante aceptada desde ${it.remoteDevice.name}")
                        connectedSocket = it
                        _bluetoothState.emit(BluetoothState.CONNECTED)
                        _connectionState.emit(
                            ConnectionResult.success(
                                BluetoothDeviceData(
                                    name = it.remoteDevice.name,
                                    address = it.remoteDevice.address
                                )
                            )
                        )
                        startReadingFromSocket(it)
                    }
                } catch (e: IOException) {
                    Log.e(TAG, "Error al aceptar conexión: ${e.message}")
                    _connectionState.emit(ConnectionResult.failure("Error al aceptar conexión"))
                } catch (e: SecurityException) {
                    _connectionState.emit(ConnectionResult.failure("Permiso denegado al aceptar conexión"))
                }
            }

            Result.success(Unit)
        } catch (e: SecurityException) {
            Result.failure(SecurityException("Permiso BLUETOOTH_CONNECT no concedido"))
        } catch (e: Exception) {
            Log.e(TAG, "Excepción en startListening: ${e.message}")
            Result.failure(e)
        }
    }


    private fun startReadingFromSocket(socket: BluetoothSocket) {
        Log.d(TAG, "Iniciando lectura del socket...")
        readJob = CoroutineScope(Dispatchers.IO).launch {
            val buffer = ByteArray(1024)
            var bytes: Int
            val inputStream: InputStream = socket.inputStream

            try {
                while (true) {

                    bytes = inputStream.read(buffer)

                    val message = String(buffer, 0, bytes)
                    Log.d(TAG, "Mensaje recibido: $message")
                    _incomingMessages.emit(MessageResult.success(message))
                }
            } catch (e: IOException) {
                Log.e(TAG, "Error al leer mensaje: ${e.message}")
                _incomingMessages.emit(MessageResult.failure("Error al leer mensaje"))
            }
        }
    }



    override suspend fun sendPurchaseConfirmation(chatId: String): Result<MessageResult> {
        Log.d(TAG, "Enviando confirmación de compra con chatId: $chatId")
        return try {
            Log.d(TAG, "Mensaje enviado correctamente")
            val outputStream: OutputStream? = connectedSocket?.outputStream
            val message = "PURCHASE_CONFIRMATION:$chatId"
            outputStream?.write(message.toByteArray())
            Result.success(MessageResult.success("Mensaje enviado"))
        } catch (e: IOException) {
            Log.e(TAG, "Error al enviar mensaje: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun sendPurchaseAccepted(): Result<MessageResult> {
        return try {
            val outputStream: OutputStream? = connectedSocket?.outputStream
            if (outputStream != null) {
                val message = "PURCHASE_ACCEPTED"
                outputStream.write(message.toByteArray())
                Log.d(TAG, "Mensaje enviado: $message")
                Result.success(MessageResult.success(message))
            } else {
                Log.e(TAG, "OutputStream es null. No se puede enviar el mensaje.")
                Result.failure(IOException("No se pudo obtener el OutputStream del socket"))
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error al enviar mensaje: ${e.message}")
            Result.failure(e)
        }
    }


    override suspend fun disconnect(): Result<Unit> {
        Log.d(TAG, "Desconectando sockets y limpiando recursos...")
        return try {
            readJob?.cancel()
            listenJob?.cancel()
            clientSocket?.close()
            serverSocket?.close()
            connectedSocket?.close()
            Log.d(TAG, "Sockets cerrados y estado actualizado a DISCONNECTED")

            _bluetoothState.emit(BluetoothState.DISCONNECTED)

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error al desconectar: ${e.message}")
            Result.failure(e)
        }
    }


    override fun cleanup() {
        CoroutineScope(Dispatchers.IO).launch {
            disconnect()
        }
    }
}
