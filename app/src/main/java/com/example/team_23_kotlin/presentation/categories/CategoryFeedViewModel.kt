// presentation/categories/CategoryFeedViewModel.kt
package com.example.team_23_kotlin.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.posts.PostEntity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class CategoryFeedViewModel(
    private val db: FirebaseFirestore,
    private val categoryId: String
) : ViewModel() {

    private val _state = MutableStateFlow(CategoryFeedState())
    val state: StateFlow<CategoryFeedState> = _state

    init {
        listenCategory()
    }

    private fun listenCategory() {
        _state.value = _state.value.copy(isLoading = true, error = null)

        val flow = callbackFlow<List<PostEntity>> {
            val categoryRef = db.document("/category/$categoryId")

            val query = db.collection("posts")
                .whereEqualTo("status", "active")
                .whereEqualTo("category", categoryRef)

            val reg = query.addSnapshotListener { snap, e ->
                if (e != null) {
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val docs = snap?.documents.orEmpty().sortedByDescending { d ->
                    (d.get("created_at") as? com.google.firebase.Timestamp)
                        ?.toDate()
                        ?.time ?: 0L
                }

                val items = docs.map { d ->
                    val data = d.data ?: emptyMap<String, Any?>()
                    PostEntity(
                        id = d.id,
                        title = data["title"] as? String ?: "",
                        description = data["description"] as? String ?: "",
                        price = when (val p = data["price"]) {
                            is Long -> p
                            is Int -> p.toLong()
                            is Double -> p.toLong()
                            else -> 0L
                        },
                        images = (data["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                        userRef = data["user_ref"] as? String ?: "",
                        status = data["status"] as? String ?: ""
                    )
                }

                trySend(items)
            }

            awaitClose { reg.remove() }
        }

        viewModelScope.launch {
            flow.collect { list ->
                _state.value = _state.value.copy(isLoading = false, error = null, items = list)
            }
        }
    }

}
