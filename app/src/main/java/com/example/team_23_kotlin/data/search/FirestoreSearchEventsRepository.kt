package com.example.team_23_kotlin.data.search

import com.google.firebase.firestore.FirebaseFirestore

class FirestoreSearchEventsRepository(
    private val db: FirebaseFirestore
) {
    fun getTopCategoriesForUser(
        userId: String,
        limit: Int = 3,
        onResult: (List<String>) -> Unit
    ) {
        db.collection("product_search_events")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { result ->
                val counts = mutableMapOf<String, Int>()
                for (doc in result) {
                    val cat = doc.getString("selectedCategory")?.lowercase() ?: continue
                    counts[cat] = counts.getOrDefault(cat, 0) + 1
                }
                val topCats = counts.entries
                    .sortedByDescending { it.value }
                    .take(limit)
                    .map { it.key }
                onResult(topCats)
            }
            .addOnFailureListener {
                onResult(emptyList())
            }
    }
}