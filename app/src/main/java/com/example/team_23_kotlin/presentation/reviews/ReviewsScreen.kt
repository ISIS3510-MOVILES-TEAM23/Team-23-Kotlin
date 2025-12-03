package com.example.team_23_kotlin.presentation.reviews

import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.team_23_kotlin.presentation.shared.rememberConnectivityStatus
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewsScreen(
    postId: String,
    viewModel: ReviewsViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    val isConnected by rememberConnectivityStatus()
    var wasOffline by remember { mutableStateOf(false) }

    LaunchedEffect(isConnected) {
        if (!isConnected) {
            wasOffline = true
            snackbarHostState.showSnackbar(
                "Conexión perdida. Mostrando reviews guardados. Solo verás los últimos 10."
            )
        } else if (wasOffline) {
            snackbarHostState.showSnackbar("Conexión restaurada. Actualizando…")
            viewModel.onEvent(ReviewsEvent.LoadReviews(postId))
            wasOffline = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.onEvent(ReviewsEvent.LoadReviews(postId))
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Reviews",
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
    ) { padding ->

        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                Alignment.Center
            ) {
                CircularProgressIndicator()
            }

            state.reviews.isEmpty() -> Box(
                Modifier.fillMaxSize().padding(padding),
                Alignment.Center
            ) {
                Text("No reviews yet.", color = Color.Gray)
            }

            else -> LazyColumn(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.reviews) { review ->
                    ReviewCard(review = review)
                }
            }
        }
    }
}
@Composable
fun ReviewCard(review: ReviewUi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(Modifier.fillMaxWidth().padding(16.dp)) {

            // ⭐ Primera fila: avatar + nombre + rating
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Filled.Person,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )

                Spacer(Modifier.width(12.dp))

                Column(Modifier.weight(1f)) {
                    Text(
                        review.reviewerName,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        formatReviewDate(review.createdAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Text(
                    text = "⭐ ${review.rating}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Spacer(Modifier.height(12.dp))

            // ⭐ Comentario
            Text(
                review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Black
            )

            Spacer(Modifier.height(12.dp))

            // ⭐ Galería de fotos (si existen)
            if (review.images.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    review.images.forEach { imageUrl ->
                        AsyncImage(
                            model = imageUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(110.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
}
fun formatReviewDate(timestamp: Long): String {
    return try {
        val date = Date(timestamp)
        val formatter = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
        formatter.format(date)
    } catch (e: Exception) {
        ""
    }
}

