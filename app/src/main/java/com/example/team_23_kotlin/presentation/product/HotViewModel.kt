package com.example.team_23_kotlin.presentation.product

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.core.network.hasInternetConnection
import com.example.team_23_kotlin.data.local.PostsCacheStorage
import com.example.team_23_kotlin.data.local.PostsMemoryCache
import com.example.team_23_kotlin.data.posts.FirestorePostsRepository
import com.example.team_23_kotlin.data.posts.PostEntity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class HotViewModel(
	private val appContext: Context,
	private val db: FirebaseFirestore,
	private val postsRepo: FirestorePostsRepository,
	private val cacheDisk: PostsCacheStorage,
	private val cacheMem: PostsMemoryCache
) : ViewModel() {

	private val _state = MutableStateFlow(HotState())
	val state: StateFlow<HotState> = _state

	/**
	 * Loads hot posts for a given category using counts in product_click_events.
	 * Falls back to cached results when offline or when Firestore fails.
	 */
	fun load(categoryName: String, excludeId: String?, limit: Int) {
		val key = "hot_${categoryName.lowercase()}"
		viewModelScope.launch {
			_state.value = HotState(isLoading = true)

			if (!appContext.hasInternetConnection()) {
				// offline → serve from cache if available
				val mem = cacheMem.getList(key).orEmpty()
				val disk = cacheDisk.loadPosts(key).orEmpty()
				val items = (mem + disk)
					.filter { it.id != excludeId }
					.distinctBy { it.id }
					.take(limit)
				_state.value = HotState(isLoading = false, items = items)
				return@launch
			}

			try {
				// Pull up to 500 recent click events for this category and count postId
				val snap = db.collection("product_click_events")
					.whereEqualTo("category", categoryName)
					.limit(500)
					.get()
					.await()

				val counts: MutableMap<String, Int> = mutableMapOf()
				for (doc in snap.documents) {
					val pid = doc.getString("postId") ?: continue
					if (pid == excludeId) continue
					counts[pid] = (counts[pid] ?: 0) + 1
				}

				val orderedIds = counts.entries
					.sortedByDescending { it.value }
					.map { it.key }
					.take(limit * 2) // fetch a few more for safety

				val items: List<PostEntity> = if (orderedIds.isNotEmpty()) {
					withContext(Dispatchers.IO) {
						orderedIds.map { id ->
							viewModelScope.async(Dispatchers.IO) { postsRepo.getPostById(id) }
						}.mapNotNull { runCatching { it.await() }.getOrNull() }
							.filter { it.id != excludeId }
							.take(limit)
					}
				} else {
					emptyList()
				}

				// Cache and publish
				if (items.isNotEmpty()) {
					cacheMem.putList(key, items)
					cacheDisk.savePosts(key, items)
				}
				_state.value = HotState(isLoading = false, items = items)
			} catch (e: Exception) {
				// failure → try cache
				val cached = cacheMem.getList(key) ?: cacheDisk.loadPosts(key) ?: emptyList()
				_state.value = HotState(isLoading = false, items = cached, error = e.message)
			}
		}
	}
}


