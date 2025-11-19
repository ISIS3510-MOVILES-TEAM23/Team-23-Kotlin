package com.example.team_23_kotlin.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.team_23_kotlin.R
import com.example.team_23_kotlin.core.network.hasInternetConnection
import com.example.team_23_kotlin.presentation.shared.LocationViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onGoToEdit: () -> Unit,
    locationViewModel: LocationViewModel,
    onProductClick: (String) -> Unit,
    onGoToSales: () -> Unit = {},
    onGoToPurchases: () -> Unit = {},   // 👈 AÑADIR ESTA LÍNEA
    viewModel: ProfileViewModel = hiltViewModel()
)
 {
    val state by viewModel.state.collectAsState()
    val isInCampus by locationViewModel.isInCampus.collectAsState()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var wasOffline by remember { mutableStateOf(false) }

    // ✅ Refresca drafts al entrar y al volver al perfil
    LaunchedEffect(Unit) {
        viewModel.refreshDraftsOnly()
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.refreshDraftsOnly()
            }
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }

    // 🌐 Detecta reconexión a internet y sincroniza automáticamente
    LaunchedEffect(Unit) {
        while (true) {
            val isOnline = context.hasInternetConnection()
            if (isOnline && wasOffline) {
                viewModel.syncDraftsIfOnline()
                scope.launch {
                    snackbarHost.showSnackbar("✅ Back online — drafts posted!")
                }
                wasOffline = false
            } else if (!isOnline) {
                wasOffline = true
            }
            delay(3000) // revisa cada 3 segundos
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHost) }
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
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = state.error ?: "You’re offline. Connect to the internet.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            modifier = Modifier.padding(horizontal = 32.dp),
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.onEvent(ProfileEvent.LoadUser) },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Retry", color = Color.White)
                        }
                    }
                }
            }

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .verticalScroll(rememberScrollState())
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    // 🔹 Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                            .background(MaterialTheme.colorScheme.primary)
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Profile",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge
                        )
                    }

                    // 🔹 Contenido del perfil
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(25.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 🔹 Foto del usuario
                        Box(modifier = Modifier.size(120.dp).clip(CircleShape)) {
                            AsyncImage(
                                model = state.photoUrl ?: "https://picsum.photos/200",
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 🔹 Datos del usuario
                        Text(state.userName, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            state.userHandle,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF666666)
                        )
                        Text(
                            state.userRole,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF666666)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        // 🔹 Ubicación actual
                        LocationBadge(isInCampus == true)

                        // 🔹 Carrera / Major
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Major: ${state.major}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF444444)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 🔹 Botones
                        Button(
                            onClick = { onGoToEdit() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                        ) {
                            Text(
                                "Edit Profile",
                                color = Color(0xFF333333),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onGoToSales() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text(
                                "Sales",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { onGoToPurchases() },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                "Purchases",
                                color = Color.White,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))


                        Spacer(modifier = Modifier.height(48.dp))

                        // 🆕 Tabs para My Products y Drafts
                        var selectedTab by remember { mutableStateOf(0) }
                        val tabs = listOf("My Products", "Drafts")

                        TabRow(
                            selectedTabIndex = selectedTab,
                            modifier = Modifier.fillMaxWidth(),
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                            contentColor = MaterialTheme.colorScheme.primary
                        ) {
                            tabs.forEachIndexed { index, title ->
                                Tab(
                                    selected = selectedTab == index,
                                    onClick = { selectedTab = index },
                                    text = {
                                        Text(
                                            title,
                                            style = MaterialTheme.typography.titleSmall
                                        )
                                    }
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        when (selectedTab) {
                            0 -> {
                                // 🔹 My Products
                                Text(
                                    "My Products",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                                LazyVerticalGrid(
                                    columns = GridCells.Fixed(2),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .heightIn(max = 600.dp),
                                    contentPadding = PaddingValues(vertical = 8.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    items(state.products) { product ->
                                        ProductCard(product = product, onClick = {
                                            onProductClick(product.id)
                                        })
                                    }
                                }
                            }

                            1 -> {
                                // 🔹 Drafts
                                Text(
                                    "Drafts (offline)",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )

                                if (state.drafts.isEmpty()) {
                                    Text(
                                        text = "No drafts saved locally.",
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                } else {
                                    LazyVerticalGrid(
                                        columns = GridCells.Fixed(2),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .heightIn(max = 600.dp),
                                        contentPadding = PaddingValues(vertical = 8.dp),
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        verticalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        items(state.drafts) { draft ->
                                            ProductCard(product = draft, onClick = { })
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProductCard(product: Product, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalAlignment = Alignment.Start
    ) {
        AsyncImage(
            model = product.imageUrl,
            contentDescription = product.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(8.dp))
                .clickable { onClick() },
            placeholder = painterResource(id = R.drawable.ic_placeholder),
            error = painterResource(id = R.drawable.ic_placeholder)
        )

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = product.title,
            style = MaterialTheme.typography.bodySmall,
            color = Color.Black,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        if (product.status.contains("Draft", ignoreCase = true)) {
            Text(
                text = "📝 Draft",
                style = MaterialTheme.typography.labelSmall,
                color = Color(0xFF6D4C41),
                modifier = Modifier.padding(start = 4.dp, top = 2.dp)
            )
        }
    }
}

@Composable
fun LocationBadge(isInCampus: Boolean) {
    val text = if (isInCampus) "On Campus" else "Outside Campus"
    val bgColor = if (isInCampus) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val contentColor = if (isInCampus) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val icon = if (isInCampus) Icons.Filled.LocationOn else Icons.Filled.Public

    Row(
        modifier = Modifier
            .background(bgColor, shape = RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall,
            color = contentColor
        )
    }
}
