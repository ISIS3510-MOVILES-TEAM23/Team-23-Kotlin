package com.example.team_23_kotlin.presentation.wishlist

import android.content.Context
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.team_23_kotlin.data.local.PostsCacheStorage
import com.example.team_23_kotlin.data.local.PostsMemoryCache
import com.example.team_23_kotlin.data.local.SharedPostsMemoryCache
import com.example.team_23_kotlin.data.local.WishlistStorage
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.presentation.profile.Product
import com.example.team_23_kotlin.presentation.profile.ProductCard
import com.google.firebase.firestore.FirebaseFirestore
import com.example.team_23_kotlin.presentation.shared.rememberConnectivityStatus
import com.example.team_23_kotlin.core.network.hasInternetConnection

@Composable
fun WishlistScreen(
	onItemClick: (String) -> Unit = {}
) {
	val context = LocalContext.current
	val vm: WishlistViewModel = viewModel(factory = WishlistVmFactory(context))
	val s by vm.state.collectAsState()

	// Retry hydration automatically when connectivity returns.
	val isConnected by rememberConnectivityStatus()
	LaunchedEffect(isConnected) {
		if (isConnected) vm.retryPending()
	}

	Text(
		"Wishlist",
		style = MaterialTheme.typography.titleMedium,
		modifier = Modifier.padding(bottom = 16.dp),
		fontWeight = FontWeight.Bold
	)

	if (s.items.isEmpty()) {
		// Distinguish between truly empty vs. offline without hydrated details
		if (s.ids.isEmpty()) {
			Text("Your wishlist is empty.")
		} else {
			Text("Offline: showing only saved items available on this device.")
		}
		return
	}

	LazyVerticalGrid(
		columns = GridCells.Fixed(2),
		modifier = Modifier
			.fillMaxWidth()
			.heightIn(max = 600.dp),
		contentPadding = PaddingValues(vertical = 8.dp),
		horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp),
		verticalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(12.dp)
	) {
		items(s.items) { post ->
			val product = Product(
				id = post.id,
				title = post.title,
				description = post.description,
				price = post.price.toDouble(),
				imageUrl = post.images.firstOrNull() ?: ""
			)
			ProductCard(product = product, onClick = { onItemClick(post.id) })
		}
	}
}

private class WishlistVmFactory(private val context: Context) : ViewModelProvider.Factory {
	override fun <T : ViewModel> create(modelClass: Class<T>): T {
		// Reuse the same memory cache across app screens
		val memory = SharedPostsMemoryCache.instance
		val repo = FirestorePostsRepository(
			FirebaseFirestore.getInstance(),
			PostsCacheStorage(context.applicationContext),
			memory,
			isOnline = { context.hasInternetConnection() }
		)
		@Suppress("UNCHECKED_CAST")
		return WishlistViewModel(
			context.applicationContext,
			WishlistStorage(context.applicationContext),
			repo,
			memory
		) as T
	}
}


