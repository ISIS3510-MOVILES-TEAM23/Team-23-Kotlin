package com.example.team_23_kotlin.presentation.confirmpurchase

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun ConfirmPurchaseScreen(
    chatId: String,
    onCancel: () -> Unit,
    onPurchaseSuccess: () -> Unit,
    viewModel: ConfirmPurchaseViewModel
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    // Launcher para habilitar Bluetooth
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        // El usuario habilitó/deshabilitó Bluetooth
        viewModel.onEvent(ConfirmPurchaseEvent.CheckBluetooth)
    }

    // Launcher para permisos
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        viewModel.onEvent(ConfirmPurchaseEvent.PermissionsResult(allGranted))
    }

    // Solicitar permisos al inicio
    LaunchedEffect(Unit) {
        Log.d("ConfirmPurchaseScreen", "Estado inicial: $state")
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        permissionLauncher.launch(requiredPermissions)
    }

    // Verificar estado después de permisos
    LaunchedEffect(state.permissionsGranted) {
        if (state.permissionsGranted) {
            viewModel.onEvent(ConfirmPurchaseEvent.CheckBluetooth)
        }
    }

    // Manejar compra exitosa
    LaunchedEffect(state.purchaseConfirmed) {
        Log.d("ConfirmPurchaseScreen", "LaunchedEffect triggered - purchaseConfirmed = ${state.purchaseConfirmed}")
        if (state.purchaseConfirmed) {

            kotlinx.coroutines.delay(2000) // Mostrar confirmación por 2 segundos
            viewModel.resetState()
            Log.d("ConfirmPurchaseScreen", "ESTADO: ${state}")
            onPurchaseSuccess()


        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Confirm Purchase",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(20.dp))

        when {
            !state.permissionsGranted -> {
                PermissionRequiredContent(
                    onRequestPermissions = {
                        viewModel.onEvent(ConfirmPurchaseEvent.RequestPermissions)
                    }
                )
            }

            !state.isBluetoothEnabled && state.connectionStatus != ConnectionStatus.CONNECTED -> {
                BluetoothDisabledContent(
                    onEnableBluetooth = {
                        val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                        enableBluetoothLauncher.launch(enableBtIntent)
                    }
                )
            }


            state.connectionStatus == ConnectionStatus.IDLE -> {
                IdleContent(
                    onStartConnection = {
                        viewModel.onEvent(ConfirmPurchaseEvent.StartConnection)
                        viewModel.onEvent(ConfirmPurchaseEvent.StartScanning)
                    }
                )
            }

            state.connectionStatus == ConnectionStatus.SCANNING -> {
                ScanningContent(
                    devices = state.nearbyDevices,
                    onDeviceSelected = { device ->
                        viewModel.onEvent(ConfirmPurchaseEvent.ConnectToDevice(device))
                    },
                    onStopScanning = {
                        viewModel.onEvent(ConfirmPurchaseEvent.StopScanning)
                    }
                )
            }

            state.connectionStatus == ConnectionStatus.CONNECTING || state.isConnecting -> {
                ConnectingContent()
            }

            state.connectionStatus == ConnectionStatus.CONNECTED || state.isConnected -> {
                ConnectedContent(
                    confirmationSent = state.confirmationSent,
                    purchaseConfirmed = state.purchaseConfirmed,
                    onSendConfirmation = {
                        viewModel.onEvent(ConfirmPurchaseEvent.SendConfirmation)
                    }
                )
            }

            state.connectionStatus == ConnectionStatus.FAILED -> {
                ErrorContent(
                    error = state.error ?: "Error de conexión",
                    onRetry = {
                        viewModel.onEvent(ConfirmPurchaseEvent.ClearError)
                        viewModel.onEvent(ConfirmPurchaseEvent.StartConnection)
                    }
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Mostrar error si existe
        state.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = error,
                    modifier = Modifier.padding(16.dp),
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // Botón cancelar
        TextButton(
            onClick = {
                viewModel.onEvent(ConfirmPurchaseEvent.Cancel)
                viewModel.resetState()
                onCancel()
            }
        ) {
            Text(
                text = "Cancel",
                color = MaterialTheme.colorScheme.error,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun PermissionRequiredContent(
    onRequestPermissions: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Se requieren permisos de Bluetooth",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRequestPermissions) {
            Text("Otorgar Permisos")
        }
    }
}

@Composable
private fun BluetoothDisabledContent(
    onEnableBluetooth: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Bluetooth está deshabilitado",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onEnableBluetooth) {
            Text("Habilitar Bluetooth")
        }
    }
}

@Composable
private fun IdleContent(
    onStartConnection: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Ready to Connect",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Bring your phones with Bluetooth on closer together to confirm the purchase",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onStartConnection) {
            Text("Buscar dispositivos", fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ScanningContent(
    devices: List<BluetoothDeviceInfo>,
    onDeviceSelected: (BluetoothDeviceInfo) -> Unit,
    onStopScanning: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Buscando dispositivos...",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(24.dp))

        if (devices.isNotEmpty()) {
            Text(
                text = "Dispositivos encontrados:",
                style = MaterialTheme.typography.titleSmall
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn {
                items(devices) { device ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        onClick = { onDeviceSelected(device) }
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = device.name ?: "Dispositivo desconocido",
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                text = device.address,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        TextButton(onClick = onStopScanning) {
            Text("Detener búsqueda")
        }
    }
}

@Composable
private fun ConnectingContent() {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Conectando...",
            fontSize = 18.sp
        )
        Spacer(modifier = Modifier.height(16.dp))
        CircularProgressIndicator()
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Estableciendo conexión con el otro dispositivo",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
    }
}

@Composable
private fun ConnectedContent(
    confirmationSent: Boolean,
    purchaseConfirmed: Boolean,
    onSendConfirmation: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        when {
            purchaseConfirmed -> {
                Text(
                    text = "✅ ¡Compra Confirmada!",
                    fontSize = 20.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "La transacción se completó exitosamente",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            confirmationSent -> {
                Text(
                    text = "Confirmación enviada",
                    fontSize = 18.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Esperando respuesta del otro dispositivo...",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            else -> {
                Text(
                    text = "Conectado exitosamente",
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Los dispositivos están conectados. Presiona confirmar para completar la compra.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onSendConfirmation,
                    modifier = Modifier.fillMaxWidth(0.7f)
                ) {
                    Text("Confirmar Compra")
                }
            }
        }
    }
}

@Composable
private fun ErrorContent(
    error: String,
    onRetry: () -> Unit
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = "Error de conexión",
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = error,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(horizontal = 32.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onRetry) {
            Text("Reintentar")
        }
    }
}

