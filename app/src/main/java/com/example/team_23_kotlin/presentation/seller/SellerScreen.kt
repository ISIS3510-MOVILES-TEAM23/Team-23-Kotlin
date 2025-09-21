package com.example.team_23_kotlin.presentation.seller

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.rememberAsyncImagePainter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerScreen(
    sellerId: String,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    vmFactory: (String) -> SellerViewModel = { SellerViewModel(it) }
) {
    val viewModel: SellerViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return vmFactory(sellerId) as T
        }
    })

    val state by viewModel.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text("Seller Profile", color = MaterialTheme.colorScheme.onPrimary)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Volver", tint = MaterialTheme.colorScheme.onPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            )
        }
    ) { padding ->
        when {
            state.isLoading -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }

            state.error != null -> {
                Box(modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Error: ${state.error}")
                }
            }

            else -> {
                state.seller?.let { seller ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Imagen de perfil
                        Image(
                            painter = rememberAsyncImagePainter(seller.profileImageUrl),
                            contentDescription = "Foto del vendedor",
                            modifier = Modifier
                                .size(100.dp)
                                .clip(CircleShape)
                                .align(Alignment.CenterHorizontally),
                            contentScale = ContentScale.Crop
                        )

                        // Nombre y rol
                        Text(
                            text = seller.name,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        Text(
                            text = seller.role,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        // Rating
                        Text(
                            text = "⭐ ${seller.rating}",
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )

                        // Campus info
                        if (seller.isInCampus) {
                            Text(
                                text = "📍 En el campus",
                                color = MaterialTheme.colorScheme.primary,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                        }

                        Divider()

                        Text(
                            text = "Publicaciones de este vendedor",
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.Start)
                        )

                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            seller.products.forEach { product ->
                                ElevatedCard(
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = { onProductClick(product.id) }
                                ) {
                                    Row(modifier = Modifier.padding(12.dp)) {
                                        Image(
                                            painter = rememberAsyncImagePainter(product.imageUrl),
                                            contentDescription = product.title,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(MaterialTheme.shapes.medium),
                                            contentScale = ContentScale.Crop
                                        )
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(product.title, style = MaterialTheme.typography.bodyLarge)
                                            Text(product.price, style = MaterialTheme.typography.bodyMedium)
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

//preview
@Preview
@Composable
fun SellerScreenPreview() {
    SellerScreen(
        sellerId = "1",
        onBack = {},
        onProductClick = {}
    )
}
