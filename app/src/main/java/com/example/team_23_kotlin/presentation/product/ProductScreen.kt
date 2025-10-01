package com.example.team_23_kotlin.presentation.product

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import coil.compose.rememberAsyncImagePainter
import com.example.team_23_kotlin.core.ui.NetworkImage
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.PostsRepository
import com.example.team_23_kotlin.data.repository.AnalyticsRepositoryImpl
import com.example.team_23_kotlin.domain.repository.AnalyticsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductScreen(
    productId: String,
    onBack: () -> Unit,
    nav: NavController
) {
    // 1. Crear repositorios una sola vez (y con tipo explícito)
    val repo: PostsRepository = remember {
        FirestorePostsRepository(FirebaseFirestore.getInstance())
    }
    val analytics: AnalyticsRepository = remember {
        AnalyticsRepositoryImpl(
            FirebaseAuth.getInstance(),
            FirebaseFirestore.getInstance()
        )
    }

    // 2. Crear el ViewModel manualmente con ambos repos
    val viewModel: ProductViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return ProductViewModel(repo, analytics) as T
        }
    })

    // 3. Disparar carga del producto
    LaunchedEffect(productId) {
        viewModel.onEvent(ProductEvent.LoadProduct(productId))
    }

    val state by viewModel.state.collectAsState()

    // UI (igual que la que tú ya tienes)
    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Product details",
                        color = MaterialTheme.colorScheme.onPrimary,
                        style = MaterialTheme.typography.titleLarge
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
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
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { CircularProgressIndicator() }
            }
            state.error != null -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) { Text("Error: ${state.error}") }
            }
            else -> {
                state.product?.let { product ->
                    Column(
                        modifier = Modifier
                            .padding(padding)
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Imagen principal (usa tu componente reutilizable)
                        NetworkImage(
                            url = product.imageUrl,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(240.dp)
                                .clip(RoundedCornerShape(16.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.height(24.dp))

                        Text(
                            text = product.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(product.description, style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(24.dp))

                        Text("Price", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(product.price, style = MaterialTheme.typography.bodyMedium)

                        Spacer(Modifier.height(24.dp))

                        Text("Seller", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 8.dp)) {
                            Image(
                                painter = rememberAsyncImagePainter("https://randomuser.me/api/portraits/women/5.jpg"),
                                contentDescription = "Seller Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.size(40.dp).clip(CircleShape)
                            )
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = product.sellerName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.clickable { nav.navigate("seller/${product.id}") }
                                )
                                Text(
                                    text = "Math Student",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Spacer(Modifier.height(32.dp))

                        Button(
                            onClick = { /* TODO abrir chat */ },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                        ) {
                            Text("Contact", style = MaterialTheme.typography.titleSmall)
                        }

                        Spacer(Modifier.height(20.dp))
                    }
                }
            }
        }
    }
}
