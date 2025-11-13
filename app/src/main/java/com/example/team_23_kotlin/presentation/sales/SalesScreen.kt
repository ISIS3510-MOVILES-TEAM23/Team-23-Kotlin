package com.example.team_23_kotlin.presentation.sales

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.team_23_kotlin.data.sales.SaleEntity
import com.example.team_23_kotlin.presentation.shared.rememberConnectivityStatus
import java.text.SimpleDateFormat
import java.util.*

/**
 * Pantalla principal de ventas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SalesScreen(
    onBack: () -> Unit = {},
    viewModel: SalesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isConnected by rememberConnectivityStatus()
    var wasOffline by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            if (wasOffline) {
                snackbarHostState.showSnackbar(
                    message = "Conexión restaurada. Buscando nuevas ventas..."
                )
                viewModel.onEvent(SalesEvent.OnRefresh)
                wasOffline = false
            }
        } else {
            wasOffline = true
            snackbarHostState.showSnackbar(
                message = "Conexión perdida. Mostrando ventas guardadas."
            )
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Sales",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = state.error ?: "Error desconocido",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.error
                        )
                        Button(onClick = { viewModel.onEvent(SalesEvent.OnRefresh) }) {
                            Text("Reintentar")
                        }
                    }
                }
            }

            else -> {
                SalesContent(
                    state = state,
                    onEvent = viewModel::onEvent,
                    modifier = Modifier.padding(paddingValues)
                )
            }
        }
    }
}

/**
 * Contenido principal de la pantalla de ventas
 */
@Composable
private fun SalesContent(
    state: SalesState,
    onEvent: (SalesEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Estadísticas superiores
        SalesStatsSection(
            totalSold = state.totalSold,
            completed = state.totalCompleted,
            pending = state.totalPending,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Pestañas
        SalesTabRow(
            currentTab = state.currentTab,
            onTabChange = { tab -> onEvent(SalesEvent.OnTabChange(tab)) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Lista de ventas
        if (state.filteredSales.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay ventas para mostrar",
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.filteredSales) { sale ->
                    SaleCard(
                        sale = sale,
                        onMarkAsCompleted = {
                            onEvent(SalesEvent.OnMarkAsShipped(sale.id))
                        }
                    )
                }
            }
        }
    }
}

/**
 * Sección de estadísticas (Total Sold, Completed, Pending)
 */
@Composable
private fun SalesStatsSection(
    totalSold: Int,
    completed: Int,
    pending: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        StatCard(
            value = totalSold.toString(),
            label = "Total Sold",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        StatCard(
            value = completed.toString(),
            label = "Completed",
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(12.dp))
        StatCard(
            value = pending.toString(),
            label = "Pending",
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Tarjeta individual de estadística
 */
@Composable
private fun StatCard(
    value: String,
    label: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 32.sp
            ),
            color = Color.Black
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
        )
    }
}

/**
 * Fila de pestañas
 */
@Composable
private fun SalesTabRow(
    currentTab: SalesTab,
    onTabChange: (SalesTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        SalesTabButton(
            text = "All Orders",
            isSelected = currentTab == SalesTab.ALL,
            onClick = { onTabChange(SalesTab.ALL) },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        SalesTabButton(
            text = "Pending",
            isSelected = currentTab == SalesTab.PENDING,
            onClick = { onTabChange(SalesTab.PENDING) },
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        SalesTabButton(
            text = "Completed",
            isSelected = currentTab == SalesTab.COMPLETED,
            onClick = { onTabChange(SalesTab.COMPLETED) },
            modifier = Modifier.weight(1f)
        )
    }
}

/**
 * Botón de pestaña individual
 */
@Composable
private fun SalesTabButton(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isSelected) MaterialTheme.colorScheme.primary
                else Color(0xFFF0F0F0)
            )
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            ),
            color = if (isSelected) Color.White else Color.Gray
        )
    }
}

/**
 * Tarjeta de venta individual
 */
@Composable
private fun SaleCard(
    sale: SaleEntity,
    onMarkAsCompleted: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Fila superior
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Imagen del producto
                if (sale.postImages.isNotEmpty()) {
                    AsyncImage(
                        model = sale.postImages.first(),
                        contentDescription = sale.postTitle,
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFF0F0F0)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Person,
                            contentDescription = null,
                            tint = Color.Gray
                        )
                    }
                }

                // Info del producto
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = sale.postTitle,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "$${sale.price}",
                        style = MaterialTheme.typography.bodyLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.Black
                    )
                }

                // Estado
                SaleStatusBadge(status = sale.status)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Info del comprador
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = Color.Gray,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = sale.buyerName,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = formatDate(sale.createdAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }

            // Botón si está pendiente
            if (sale.status == "pending") {
                Spacer(modifier = Modifier.height(12.dp))
                Button(
                    onClick = onMarkAsCompleted,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50) // Verde Completed
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Mark as Completed", color = Color.White)
                }
            }
        }
    }
}

/**
 * Badge de estado
 */
@Composable
private fun SaleStatusBadge(status: String) {
    val (text, backgroundColor, textColor) = when (status) {
        "pending" -> Triple("Pending", Color(0xFFFFF3E0), Color(0xFFFF6F00))
        "completed" -> Triple("Completed", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        else -> Triple(status, Color(0xFFF5F5F5), Color.Gray)
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(backgroundColor)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = FontWeight.Bold
            ),
            color = textColor
        )
    }
}

/**
 * Fecha
 */
private fun formatDate(date: Date?): String {
    if (date == null) return "Sin fecha"

    val now = Date()
    val diff = now.time - date.time
    val hours = diff / (1000 * 60 * 60)
    val days = hours / 24

    return when {
        hours < 1 -> "Hace menos de 1 hora"
        hours < 24 -> "Hace $hours hora${if (hours > 1) "s" else ""}"
        days < 7 -> "Hace $days día${if (days > 1) "s" else ""}"
        else -> {
            val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            sdf.format(date)
        }
    }
}
