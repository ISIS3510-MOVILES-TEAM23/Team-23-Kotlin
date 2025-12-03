package com.example.team_23_kotlin.presentation.product

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import com.example.team_23_kotlin.core.network.hasInternetConnection
import com.example.team_23_kotlin.data.local.PostsCacheStorage
import com.example.team_23_kotlin.data.local.SharedPostsMemoryCache
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.google.firebase.firestore.FirebaseFirestore

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HotListScreen(
	categoryName: String,
	onBack: () -> Unit,
	onItemClick: (String) -> Unit
) {
	val context = LocalContext.current
	val vm: HotViewModel = viewModel(factory = HotVmFactory(context))
	LaunchedEffect(categoryName) { vm.load(categoryName = categoryName, excludeId = null, limit = 40) }
	val s by vm.state.collectAsState()

	Scaffold(
		topBar = {
			CenterAlignedTopAppBar(
				title = { Text("Hot in ${categoryName.replaceFirstChar { it.uppercase() }}", fontWeight = FontWeight.Bold) },
				navigationIcon = {
					IconButton(onClick = onBack) {
						Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
					}
				}
			)
		}
	) { padding ->
		val isOnline = context.hasInternetConnection()
		if (!isOnline && s.items.isEmpty()) {
			// Offline first-run: show guidance
			Text(
				text = "No connection — reconnect to see hot similar products.",
				style = MaterialTheme.typography.bodyMedium,
				color = MaterialTheme.colorScheme.onSurfaceVariant,
				modifier = Modifier.padding(padding).padding(16.dp)
			)
			return@Scaffold
		}

		LazyVerticalGrid(
			columns = GridCells.Fixed(2),
			modifier = Modifier
				.fillMaxSize()
				.padding(padding)
				.padding(12.dp),
			contentPadding = PaddingValues(4.dp),
			verticalArrangement = Arrangement.spacedBy(12.dp),
			horizontalArrangement = Arrangement.spacedBy(12.dp)
		) {
			items(s.items) { p ->
				ElevatedCard(onClick = { onItemClick(p.id) }, elevation = CardDefaults.cardElevation(0.dp)) {
					AsyncImage(
						model = p.images.firstOrNull(),
						contentDescription = p.title,
						modifier = Modifier
							.clip(RoundedCornerShape(12.dp))
							.fillMaxWidth()
							.aspectRatio(1f),
						contentScale = ContentScale.Crop
					)
					Text(
						p.title,
						style = MaterialTheme.typography.bodyMedium,
						fontWeight = FontWeight.SemiBold,
						modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
					)
					Text(
						"$${p.price}",
						style = MaterialTheme.typography.bodySmall,
						modifier = Modifier.padding(horizontal = 12.dp, vertical = 0.dp)
					)
				}
			}
		}
	}
}

private class HotVmFactory(private val context: Context) : ViewModelProvider.Factory {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		val memory = SharedPostsMemoryCache.instance
		val postsRepo = FirestorePostsRepository(
			FirebaseFirestore.getInstance(),
			PostsCacheStorage(context.applicationContext),
			memory,
			isOnline = { context.hasInternetConnection() }
		)
		@Suppress("UNCHECKED_CAST")
		return HotViewModel(
			context.applicationContext,
			FirebaseFirestore.getInstance(),
			postsRepo,
			PostsCacheStorage(context.applicationContext),
			memory
		) as T
	}
}


