package com.example.team_23_kotlin.presentation.wishlist

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.local.PostsMemoryCache
import com.example.team_23_kotlin.data.local.WishlistStorage
import com.example.team_23_kotlin.data.posts.PostEntity
import com.example.team_23_kotlin.data.posts.PostsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class WishlistViewModel(
	private val appContext: Context,
	private val storage: WishlistStorage,
	private val postsRepo: PostsRepository,
	private val memoryCache: PostsMemoryCache?
) : ViewModel() {

	private val _state = MutableStateFlow(WishlistState())
	val state: StateFlow<WishlistState> = _state

	// Track items we couldn't hydrate while offline; retry on connectivity.
	private var pendingIds: Set<String> = emptySet()

	init {
		viewModelScope.launch {
			storage.idsFlow.collectLatest { ids ->
				_state.value = _state.value.copy(ids = ids, isLoading = true, error = null)
				loadItems(ids)
			}
		}
	}

	fun onToggle(id: String, onResult: (Boolean) -> Unit) {
		viewModelScope.launch(Dispatchers.IO) {
			val added = storage.toggle(id)
			withContext(Dispatchers.Main) { onResult(added) }
		}
	}

	// Called by UI when connectivity returns to hydrate missing details.
	fun retryPending() {
		viewModelScope.launch { loadItems(state.value.ids) }
	}

	private suspend fun loadItems(ids: Set<String>) {
		if (ids.isEmpty()) {
			_state.value = _state.value.copy(items = emptyList(), isLoading = false)
			return
		}

		try {
			// Parallel fetch with structured concurrency (IO dispatcher).
			val results: List<Pair<String, PostEntity?>> = withContext(Dispatchers.IO) {
				ids.map { id ->
					viewModelScope.async(Dispatchers.IO) {
						val cached = memoryCache?.getDetail(id)
						if (cached != null) return@async id to cached
						// Try repository; it may fail when offline.
						val fetched = runCatching { postsRepo.getPostById(id) }.getOrNull()
						id to fetched
					}
				}.map { it.await() }
			}

			val items = results.mapNotNull { it.second }
			pendingIds = results.filter { it.second == null }.map { it.first }.toSet()

			// Update cache on the way back to main
			items.forEach { memoryCache?.putDetail(it) }
			_state.value = _state.value.copy(items = items, isLoading = false)
		} catch (e: Exception) {
			_state.value = _state.value.copy(isLoading = false, error = e.message ?: "Unknown error")
		}
	}
}


