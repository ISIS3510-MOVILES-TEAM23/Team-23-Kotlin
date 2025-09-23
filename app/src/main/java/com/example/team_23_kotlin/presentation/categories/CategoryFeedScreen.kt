// presentation/categories/CategoryFeedScreen.kt
package com.example.team_23_kotlin.presentation.categories

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFeedScreen(
    categoryId: String,
    categoryTitle: String,
    onBack: () -> Unit,
    onItemClick: (String) -> Unit
) {
    val vm: CategoryFeedViewModel = viewModel(
        factory = CategoryFeedVMFactory(FirebaseFirestore.getInstance(), categoryId)
    )
    val state by vm.state.collectAsState()

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text(categoryTitle) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        when {
            state.isLoading -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator() }

            state.error != null   -> Box(
                Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) { Text("Error: ${state.error}") }

            else -> LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(state.items, key = { it.id }) { post ->
                    CategoryItemCard(
                        title = post.title.ifBlank { "Untitled" },
                        priceLabel = "$${"%,d".format(post.price)}",
                        imageUrl = post.images.firstOrNull(),
                        onClick = { onItemClick(post.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CategoryItemCard(
    title: String,
    priceLabel: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    ElevatedCard(
        onClick = onClick,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        AsyncImage(
            model = imageUrl,
            contentDescription = null,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(12.dp)),
            contentScale = ContentScale.Crop
        )
        Column(Modifier.padding(10.dp)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            Text(priceLabel, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
