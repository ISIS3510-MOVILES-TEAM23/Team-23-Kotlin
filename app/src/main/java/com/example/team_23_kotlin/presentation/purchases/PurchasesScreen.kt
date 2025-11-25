package com.example.team_23_kotlin.presentation.purchases

import android.os.Build
import androidx.annotation.RequiresApi
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.team_23_kotlin.data.purchases.PurchaseEntity
import com.example.team_23_kotlin.presentation.shared.rememberConnectivityStatus
import java.text.SimpleDateFormat
import java.util.*

@RequiresApi(Build.VERSION_CODES.Q)
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PurchasesScreen(
    onBack: () -> Unit = {},
    onFeedbackClick: (String, String) -> Unit = { _, _ -> },
    viewModel: PurchasesViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val isConnected by rememberConnectivityStatus()
    var wasOffline by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (isConnected) {
            if (wasOffline) {
                snackbarHostState.showSnackbar("Conexión restaurada. Actualizando compras…")
                viewModel.onEvent(PurchasesEvent.OnRefresh)
                wasOffline = false
            }
        } else {
            wasOffline = true
            snackbarHostState.showSnackbar("Conexión perdida. Mostrando compras guardadas.")
        }
    }
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }


    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "My Purchases",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, null, tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { paddingValues ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null -> Box(
                Modifier.fillMaxSize().padding(paddingValues),
                Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error ?: "Unknown error")
                    Button(onClick = { viewModel.onEvent(PurchasesEvent.OnRefresh) }) {
                        Text("Retry")
                    }
                }
            }

            else -> PurchasesContent(
                state = state,
                onEvent = viewModel::onEvent,
                onFeedbackClick = onFeedbackClick,
                modifier = Modifier.padding(paddingValues)
            )
        }
    }
}

@Composable
private fun PurchasesContent(
    state: PurchasesState,
    onEvent: (PurchasesEvent) -> Unit,
    onFeedbackClick: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        PurchasesStatsSection(
            total = state.totalPurchased,
            completed = state.totalCompleted,
            pending = state.totalPending,
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(16.dp)
        )

        Spacer(Modifier.height(8.dp))

        PurchasesTabRow(
            currentTab = state.currentTab,
            onTabChange = { onEvent(PurchasesEvent.OnTabChange(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
        )

        Spacer(Modifier.height(8.dp))

        if (state.filteredPurchases.isEmpty()) {
            Box(Modifier.fillMaxSize(), Alignment.Center) {
                Text("No purchases to display", color = Color.Gray)
            }
        } else {
            LazyColumn(
                Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(state.filteredPurchases) { purchase ->
                    PurchaseCard(
                        purchase = purchase,
                        onReceiptAction = {
                            onEvent(PurchasesEvent.OnDownloadReceipt(it))
                        },
                        onFeedbackClick = { onFeedbackClick(purchase.id, purchase.sellerId) }
                    )
                }
            }
        }
    }
}

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


@Composable
private fun PurchasesStatsSection(
    total: Int,
    completed: Int,
    pending: Int,
    modifier: Modifier = Modifier
) {
    Row(modifier, horizontalArrangement = Arrangement.SpaceEvenly) {
        StatCard(value = total.toString(), label = "Total", modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        StatCard(value = completed.toString(), label = "Completed", modifier = Modifier.weight(1f))
        Spacer(Modifier.width(12.dp))
        StatCard(value = pending.toString(), label = "Pending", modifier = Modifier.weight(1f))
    }
}

@Composable
private fun PurchasesTabRow(
    currentTab: PurchasesTab,
    onTabChange: (PurchasesTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        PurchasesTabButton("All", currentTab == PurchasesTab.ALL, { onTabChange(PurchasesTab.ALL) })
        Spacer(Modifier.width(8.dp))
        PurchasesTabButton("Pending", currentTab == PurchasesTab.PENDING, { onTabChange(PurchasesTab.PENDING) })
        Spacer(Modifier.width(8.dp))
        PurchasesTabButton("Completed", currentTab == PurchasesTab.COMPLETED, { onTabChange(PurchasesTab.COMPLETED) })
    }
}

@Composable
private fun PurchasesTabButton(
    text: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) MaterialTheme.colorScheme.primary else Color(0xFFF0F0F0))
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 12.dp),
        Alignment.Center
    ) {
        Text(
            text,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            color = if (selected) Color.White else Color.Gray
        )
    }
}

@Composable
private fun PurchaseCard(
    purchase: PurchaseEntity,
    onReceiptAction: (PurchaseEntity) -> Unit,
    onFeedbackClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (purchase.postImages.isNotEmpty()) {
                    AsyncImage(
                        model = purchase.postImages.first(),
                        contentDescription = purchase.title,
                        modifier = Modifier.size(60.dp).clip(RoundedCornerShape(8.dp)),
                        contentScale = ContentScale.Crop
                    )
                }

                Column(Modifier.weight(1f)) {
                    Text(
                        purchase.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "$${purchase.price}",
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
                    )
                }

                PurchaseStatusBadge(status = purchase.status)
            }

            Spacer(Modifier.height(12.dp))

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Filled.Person, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(16.dp))
                Text(
                    purchase.sellerName,
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(Modifier.weight(1f))
                Text(
                    formatDate(purchase.createdAt),
                    color = Color.Gray,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            // 🔥 Si ya fue descargado → abrir PDF
            if (purchase.isDownloaded) {
                Button(
                    onClick = { onReceiptAction(purchase) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Abrir PDF", color = Color.White)
                }
            }
// 🧾 Si NO ha sido descargado → generar PDF
            else {
                Button(
                    onClick = { onReceiptAction(purchase) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Descargar recibo", color = Color.White)
                }
            }

            // 🌟 Botón de Feedback (para todas las compras completadas)
            if (purchase.status == "completed") {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = onFeedbackClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (purchase.hasFeedback) "Editar Feedback" else "Dejar Feedback")
                }
            }


        }
    }
}

@Composable
private fun PurchaseStatusBadge(status: String) {
    val (label, bg, color) = when (status) {
        "pending" -> Triple("Pending", Color(0xFFFFF3E0), Color(0xFFFF6F00))
        "completed" -> Triple("Completed", Color(0xFFE8F5E9), Color(0xFF2E7D32))
        else -> Triple(status, Color(0xFFF5F5F5), Color.Gray)
    }

    Box(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(bg)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(label, color = color, fontWeight = FontWeight.Bold)
    }
}

private fun formatDate(date: Date?): String {
    if (date == null) return "No date"

    val now = Date()
    val diff = now.time - date.time
    val hours = diff / (1000 * 60 * 60)
    val days = hours / 24

    return when {
        hours < 1 -> "Less than 1h ago"
        hours < 24 -> "$hours hour${if (hours != 1L) "s" else ""} ago"
        days < 7 -> "$days day${if (days != 1L) "s" else ""} ago"
        else -> SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date)
    }
}
