package com.example.team_23_kotlin.data.posts

import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestorePostsRepository(
    private val db: FirebaseFirestore
) : PostsRepository {

    override suspend fun getActivePosts(limit: Int): List<PostEntity> {
        val qs = db.collection("posts")
            .whereEqualTo("status", "active")
            .limit(limit.toLong())
            .get()
            .await()

        return qs.documents.map { snap ->
            val data = snap.data ?: emptyMap<String, Any?>()
            PostEntity(
                id = snap.id,
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
    }

    override suspend fun getNewPosts(limit: Int): List<PostEntity> {
        val fiveDaysAgo = Timestamp(
            Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L)
        )

        val qs = db.collection("posts")
            .whereGreaterThanOrEqualTo("created_at", fiveDaysAgo)
            .limit(limit.toLong())
            .get()
            .await()

        return qs.documents
            .map { snap -> mapToEntity(snap) }
            .sortedByDescending { it.createdAt }
    }


    private fun mapToEntity(snap: DocumentSnapshot): PostEntity {
        val data = snap.data ?: emptyMap<String, Any?>()
        return PostEntity(
            id = snap.id,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            price = (data["price"] as? Number)?.toLong() ?: 0L,
            images = (data["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            userRef = data["user_ref"] as? String ?: "",
            status = data["status"] as? String ?: "",
            createdAt = (data["created_at"] as? Timestamp)?.toDate()
        )
    }


    override suspend fun getPostById(id: String): PostEntity {
        val snap = db.collection("posts").document(id).get().await()
        if (!snap.exists()) error("Post not found")
        val data = snap.data ?: emptyMap<String, Any?>()

        val categoryRef = data["category_id"] as? DocumentReference
        val categoryName = categoryRef?.get()?.await()?.getString("name") ?: "Unknown"

        return PostEntity(
            id = snap.id,
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
            status = data["status"] as? String ?: "",
            categoryName = categoryName
        )
    }
}
