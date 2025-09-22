// presentation/confirmpurchase/ConfirmPurchaseViewModel.kt
package com.example.team_23_kotlin.presentation.confirmpurchase

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.domain.repository.BluetoothRepository
import com.example.team_23_kotlin.data.bluetooth.models.BluetoothState
import com.example.team_23_kotlin.data.bluetooth.models.ConnectionResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
@HiltViewModel
class ConfirmPurchaseViewModel @Inject constructor(
    private val bluetoothRepository: BluetoothRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ConfirmPurchaseState())
    val state: StateFlow<ConfirmPurchaseState> = _state.asStateFlow()

    init {
        observeBluetoothStates()
    }

    private fun observeBluetoothStates() {
        // Observar estado del Bluetooth
        viewModelScope.launch {
            bluetoothRepository.bluetoothState.collect { bluetoothState ->
                _state.value = _state.value.copy(
                    isBluetoothEnabled = bluetoothState == BluetoothState.ENABLED,
                    isConnecting = bluetoothState == BluetoothState.CONNECTING,
                    isConnected = bluetoothState == BluetoothState.CONNECTED,
                    connectionStatus = bluetoothState.toConnectionStatus()
                )
            }
        }

        // Observar dispositivos descubiertos
        viewModelScope.launch {
            bluetoothRepository.discoveredDevices.collect { devices ->
                val deviceInfoList = devices.map { device ->
                    BluetoothDeviceInfo(
                        name = device.name,
                        address = device.address,
                        isConnected = device.isConnected
                    )
                }
                _state.value = _state.value.copy(nearbyDevices = deviceInfoList)
            }
        }

        // Observar estado de conexión
        viewModelScope.launch {
            bluetoothRepository.connectionState.collect { connectionResult ->
                if (connectionResult.isSuccess) {
                    _state.value = _state.value.copy(
                        isConnected = true,
                        selectedDevice = connectionResult.device?.let { device ->
                            BluetoothDeviceInfo(
                                name = device.name,
                                address = device.address,
                                isConnected = device.isConnected
                            )
                        },
                        error = null
                    )
                } else {
                    _state.value = _state.value.copy(
                        error = connectionResult.error
                    )
                }
            }
        }

        // Observar mensajes entrantes
        viewModelScope.launch {
            bluetoothRepository.incomingMessages.collect { messageResult ->
                if (messageResult.isSuccess) {
                    handleIncomingMessage(messageResult.message)
                } else {
                    _state.value = _state.value.copy(
                        error = messageResult.error
                    )
                }
            }
        }
    }

    fun onEvent(event: ConfirmPurchaseEvent) {
        when (event) {
            is ConfirmPurchaseEvent.CheckBluetooth -> {
                checkBluetoothAvailability()
            }

            is ConfirmPurchaseEvent.RequestPermissions -> {
                checkPermissions()
            }

            is ConfirmPurchaseEvent.PermissionsResult -> {
                _state.value = _state.value.copy(permissionsGranted = event.granted)
                if (event.granted) {
                    checkBluetoothAvailability()
                }
            }

            is ConfirmPurchaseEvent.StartScanning -> {
                startDeviceScanning()
            }

            is ConfirmPurchaseEvent.StopScanning -> {
                stopDeviceScanning()
            }

            is ConfirmPurchaseEvent.StartConnection -> {
                startListening()
            }

            is ConfirmPurchaseEvent.ConnectToDevice -> {
                connectToDevice(event.device)
            }

            is ConfirmPurchaseEvent.SendConfirmation -> {
                sendPurchaseConfirmation()
            }

            is ConfirmPurchaseEvent.Cancel -> {
                cancelConnection()
            }

            is ConfirmPurchaseEvent.ClearError -> {
                _state.value = _state.value.copy(error = null)
            }

            else -> {
                // Otros eventos
            }
        }
    }

    private fun checkBluetoothAvailability() {
        viewModelScope.launch {
            val isAvailable = bluetoothRepository.isBluetoothAvailable()
            val hasPermissions = bluetoothRepository.hasRequiredPermissions()

            _state.value = _state.value.copy(
                isBluetoothEnabled = isAvailable,
                permissionsGranted = hasPermissions
            )

            if (!isAvailable) {
                _state.value = _state.value.copy(error = "Bluetooth no está disponible o habilitado")
            } else if (!hasPermissions) {
                _state.value = _state.value.copy(error = "Se requieren permisos de Bluetooth")
            }
        }
    }

    private fun checkPermissions() {
        viewModelScope.launch {
            val hasPermissions = bluetoothRepository.hasRequiredPermissions()
            _state.value = _state.value.copy(permissionsGranted = hasPermissions)
        }
    }

    private fun startDeviceScanning() {
        viewModelScope.launch {
            _state.value = _state.value.copy(connectionStatus = ConnectionStatus.SCANNING)

            bluetoothRepository.startDeviceDiscovery()
                .onSuccess {
                    // El scanning se está ejecutando
                }
                .onFailure { exception ->
                    _state.value = _state.value.copy(
                        error = "Error al iniciar búsqueda: ${exception.message}",
                        connectionStatus = ConnectionStatus.FAILED
                    )
                }
        }
    }

    private fun stopDeviceScanning() {
        viewModelScope.launch {
            bluetoothRepository.stopDeviceDiscovery()
            _state.value = _state.value.copy(connectionStatus = ConnectionStatus.IDLE)
        }
    }

    private fun startListening() {
        viewModelScope.launch {
            bluetoothRepository.startListening()
                .onSuccess {
                    // El servidor está escuchando
                }
                .onFailure { exception ->
                    _state.value = _state.value.copy(
                        error = "Error al iniciar servidor: ${exception.message}",
                        connectionStatus = ConnectionStatus.FAILED
                    )
                }
        }
    }

    private fun connectToDevice(device: BluetoothDeviceInfo) {
        viewModelScope.launch {
            val deviceData = com.example.team_23_kotlin.data.bluetooth.models.BluetoothDeviceData(
                name = device.name,
                address = device.address
            )

            _state.value = _state.value.copy(selectedDevice = device)

            bluetoothRepository.connectToDevice(deviceData)
                .onSuccess { connectionResult ->
                    if (!connectionResult.isSuccess) {
                        _state.value = _state.value.copy(
                            error = connectionResult.error ?: "Error de conexión"
                        )
                    }
                }
                .onFailure { exception ->
                    _state.value = _state.value.copy(
                        error = "Error de conexión: ${exception.message}"
                    )
                }
        }
    }

    private fun sendPurchaseConfirmation() {
        viewModelScope.launch {
            val chatId = _state.value.chatId

            bluetoothRepository.sendPurchaseConfirmation(chatId)
                .onSuccess { messageResult ->
                    if (messageResult.isSuccess) {
                        _state.value = _state.value.copy(confirmationSent = true)
                    } else {
                        _state.value = _state.value.copy(
                            error = messageResult.error ?: "Error al enviar confirmación"
                        )
                    }
                }
                .onFailure { exception ->
                    _state.value = _state.value.copy(
                        error = "Error al enviar confirmación: ${exception.message}"
                    )
                }
        }
    }

    private fun cancelConnection() {
        viewModelScope.launch {
            bluetoothRepository.disconnect()
        }
    }

    private fun handleIncomingMessage(message: String?) {
        message?.let { msg ->
            when {
                msg.startsWith("PURCHASE_CONFIRMATION") -> {
                    // Este dispositivo ha recibido solicitud de confirmación → responder
                    viewModelScope.launch {
                        bluetoothRepository.sendPurchaseAccepted() // <-- agrega esta función
                    }
                    _state.value = _state.value.copy(purchaseConfirmed = true)
                }
                msg.startsWith("PURCHASE_ACCEPTED") -> {
                    _state.value = _state.value.copy(purchaseConfirmed = true)
                }
                msg.startsWith("PURCHASE_REJECTED") -> {
                    _state.value = _state.value.copy(
                        error = "La compra fue rechazada",
                        purchaseConfirmed = false
                    )
                }
            }
        }
    }


    override fun onCleared() {
        super.onCleared()
        bluetoothRepository.cleanup()
    }

    // Función helper para convertir BluetoothState a ConnectionStatus
    private fun BluetoothState.toConnectionStatus(): ConnectionStatus {
        return when (this) {
            BluetoothState.DISABLED -> ConnectionStatus.IDLE
            BluetoothState.ENABLED -> ConnectionStatus.IDLE
            BluetoothState.CONNECTING -> ConnectionStatus.CONNECTING
            BluetoothState.CONNECTED -> ConnectionStatus.CONNECTED
            BluetoothState.DISCONNECTED -> ConnectionStatus.DISCONNECTED
            BluetoothState.ERROR -> ConnectionStatus.FAILED
        }
    }
}