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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.team_23_kotlin.presentation.shared.LocationViewModel
import androidx.compose.ui.res.painterResource
import com.example.team_23_kotlin.R


@Composable
fun ProfileScreen(
    onGoToEdit: () -> Unit,
    locationViewModel: LocationViewModel,
    onProductClick: (String) -> Unit,
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState()
    val isInCampus = locationViewModel.isInCampus.collectAsState()

    Scaffold { paddingValues ->
        when {
            state.value.isLoading -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.value.error != null -> {
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
                            text = state.value.error ?: "You’re offline. Connect to the internet.",
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
                        modifier = Modifier.fillMaxWidth().padding(25.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // 🔹 Foto del usuario
                        Box(
                            modifier = Modifier.size(120.dp).clip(CircleShape)
                        ) {
                            AsyncImage(
                                model = state.value.photoUrl ?: "https://picsum.photos/200",
                                contentDescription = "Foto de perfil",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.matchParentSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // 🔹 Datos del usuario
                        Text(state.value.userName, style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(state.value.userHandle, style = MaterialTheme.typography.labelMedium, color = Color(0xFF666666))
                        Text(state.value.userRole, style = MaterialTheme.typography.labelMedium, color = Color(0xFF666666))
                        Spacer(modifier = Modifier.height(4.dp))

                        // 🔹 Ubicación actual
                        LocationBadge(isInCampus.value == true)

                        // 🔹 Carrera / Major
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Major: ${state.value.major}",
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF444444)
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // 🔹 Botones
                        Button(
                            onClick = { onGoToEdit() },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE0E0E0))
                        ) {
                            Text("Edit Profile", color = Color(0xFF333333), style = MaterialTheme.typography.titleSmall)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { viewModel.onEvent(ProfileEvent.OnSalesClick) },
                            modifier = Modifier.fillMaxWidth().height(40.dp),
                            shape = RoundedCornerShape(7.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Sales", color = Color.White, style = MaterialTheme.typography.titleSmall)
                        }

                        Spacer(modifier = Modifier.height(48.dp))

                        // 🔹 Productos del usuario
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text("My Products", style = MaterialTheme.typography.titleMedium, modifier = Modifier.padding(bottom = 16.dp))
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(2),
                                modifier = Modifier.fillMaxWidth().heightIn(max = 600.dp),
                                contentPadding = PaddingValues(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(state.value.products) { product ->
                                    ProductCard(product = product, onClick = { onProductClick(product.id) })
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
            // 🔹 Placeholders para evitar pantallas vacías
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
    }
}


@Composable
fun LocationBadge(isInCampus: Boolean) {
    val text = if (isInCampus) "On Campus" else "Outside Campus"
    val bgColor = if (isInCampus) Color(0xFFE8F5E9) else Color(0xFFFFEBEE)
    val contentColor = if (isInCampus) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error
    val icon = if (isInCampus) Icons.Filled.LocationOn else Icons.Filled.Public

    Row(
        modifier = Modifier.background(bgColor, shape = RoundedCornerShape(12.dp)).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = text, style = MaterialTheme.typography.titleSmall, color = contentColor)
    }
}
