package com.example.team_23_kotlin.data.search

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await

class FirestoreSearchEventsRepository(
    private val db: FirebaseFirestore
) {
    // ❌ OLD: Callback-based (mantener para compatibilidad)
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
    
    // ✅ NEW: Suspend function (optimizado)
    suspend fun getTopCategoriesForUserSuspend(
        userId: String,
        limit: Int = 3
    ): List<String> {
        return try {
            val result = db.collection("product_search_events")
                .whereEqualTo("userId", userId)
                .get()
                .await()
            
            val counts = mutableMapOf<String, Int>()
            for (doc in result) {
                val cat = doc.getString("selectedCategory")?.lowercase() ?: continue
                counts[cat] = counts.getOrDefault(cat, 0) + 1
            }
            
            counts.entries
                .sortedByDescending { it.value }
                .take(limit)
                .map { it.key }
        } catch (e: Exception) {
            emptyList()
        }
    }
}