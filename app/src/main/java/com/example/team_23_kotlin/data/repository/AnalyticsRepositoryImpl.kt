package com.example.team_23_kotlin.data.repository

import com.example.team_23_kotlin.domain.repository.AnalyticsRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.BuildConfig
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsRepositoryImpl @Inject constructor(
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : AnalyticsRepository {

    override fun logProductSearch(
        query: String?,
        selectedCategory: String?,
        source: String,
        suggested: List<String>?
    ) {
        val uid = auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "userId" to uid,
            "timestamp" to FieldValue.serverTimestamp(),
            "query" to query,
            "selectedCategory" to selectedCategory,
            "suggestedCategories" to (suggested ?: listOfNotNull(selectedCategory)),
            "source" to source,
            "sessionId" to UUID.randomUUID().toString(),
            "appVersion" to BuildConfig.VERSION_NAME
        )
        firestore.collection("product_search_events").add(data)
    }

    override fun logProductClick(postId: String, category: String, source: String) {
        val uid = auth.currentUser?.uid ?: return
        val data = hashMapOf(
            "userId" to uid,
            "timestamp" to FieldValue.serverTimestamp(),
            "postId" to postId,
            "category" to category,
            "source" to source,
            "sessionId" to UUID.randomUUID().toString(),
            "appVersion" to BuildConfig.VERSION_NAME
        )
        firestore.collection("product_click_events").add(data)
    }
}
