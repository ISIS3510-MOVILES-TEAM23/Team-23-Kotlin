package com.example.team_23_kotlin.presentation.reviews

import android.util.LruCache
import androidx.lifecycle.ViewModel
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

@HiltViewModel
class ReviewsViewModel @Inject constructor(
    private val firestore: FirebaseFirestore
) : ViewModel() {

    // ⭐ LRU Cache integrado en el ViewModel
    private val reviewsCache = object : LruCache<String, List<ReviewUi>>(10) {}

    private val _state = MutableStateFlow(ReviewsState())
    val state = _state.asStateFlow()

    fun onEvent(event: ReviewsEvent) {
        when (event) {
            is ReviewsEvent.LoadReviews -> loadReviews(event.postId)
        }
    }

    private fun loadReviews(postId: String) {

        // ⭐ 1. Lectura instantánea desde cache (Eventual Connectivity)
        val cachedReviews = reviewsCache.get(postId)
        if (cachedReviews != null) {
            _state.update {
                it.copy(
                    reviews = cachedReviews,
                    isLoading = false
                )
            }
        }

        // ⭐ 2. Intentar obtener online
        _state.update { it.copy(isLoading = true) }

        firestore.collection("sales")
            .whereEqualTo("post_ref", firestore.document("posts/$postId"))
            .whereEqualTo("status", "completed")
            .get()
            .addOnSuccessListener { salesSnapshot ->

                val saleIds = salesSnapshot.documents.mapNotNull { it.id }

                if (saleIds.isEmpty()) {
                    _state.update { it.copy(isLoading = false, reviews = emptyList()) }
                    return@addOnSuccessListener
                }

                firestore.collection("feedbacks")
                    .whereIn("purchaseId", saleIds)
                    .get()
                    .addOnSuccessListener { feedbackSnapshot ->

                        val feedbacks = feedbackSnapshot.documents

                        if (feedbacks.isEmpty()) {
                            _state.update { it.copy(isLoading = false, reviews = emptyList()) }
                            return@addOnSuccessListener
                        }

                        val userIds = feedbacks.mapNotNull { it.getString("buyerId") }.distinct()

                        firestore.collection("users")
                            .whereIn(FieldPath.documentId(), userIds)
                            .get()
                            .addOnSuccessListener { usersSnapshot ->

                                val userMap = usersSnapshot.documents.associateBy { it.id }

                                val reviewUiList = feedbacks.mapNotNull { doc ->

                                    val rating = doc.getLong("rating")?.toInt() ?: return@mapNotNull null
                                    val comment = doc.getString("comment") ?: ""
                                    val images = doc.get("images") as? List<String> ?: emptyList()
                                    val createdAt = doc.getTimestamp("createdAt")?.toDate()?.time ?: 0L
                                    val buyerId = doc.getString("buyerId") ?: return@mapNotNull null

                                    val userData = userMap[buyerId]

                                    ReviewUi(
                                        rating = rating,
                                        comment = comment,
                                        images = images,
                                        reviewerName = userData?.getString("name") ?: "Unknown",
                                        reviewerImage = userData?.getString("profile_image"),
                                        createdAt = createdAt
                                    )
                                }.sortedByDescending { it.createdAt }

                                // ⭐ 3. Actualizar la cache con los últimos 10
                                reviewsCache.put(postId, reviewUiList.take(10))

                                _state.update {
                                    it.copy(
                                        isLoading = false,
                                        reviews = reviewUiList
                                    )
                                }
                            }
                    }
            }
            .addOnFailureListener { e ->
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage
                    )
                }
            }
    }
}
