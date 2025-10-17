package com.example.team_23_kotlin.data.posts

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.tasks.await
import java.util.Date

class FirestorePostsRepository(
    private val db: FirebaseFirestore
) : PostsRepository {

    // -----------------------------------------------------------
    // 🔹 Obtiene posts activos
    // -----------------------------------------------------------
    override suspend fun getActivePosts(limit: Int): List<PostEntity> {
        val qs = db.collection("posts")
            .whereEqualTo("status", "active")
            .limit(limit.toLong())
            .get()
            .await()

        return qs.documents.map { snap ->
            val data = snap.data ?: emptyMap<String, Any?>()
            val categoryRef = data["category_id"] as? DocumentReference
            val categoryName = categoryRef?.get()?.await()?.getString("name") ?: "Unknown"

            PostEntity(
                id = snap.id,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                price = (data["price"] as? Number)?.toLong() ?: 0L,
                images = (data["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                userId = data["user_id"] as? String ?: "",     // ✅ campo correcto
                status = data["status"] as? String ?: "",
                createdAt = (data["created_at"] as? Timestamp)?.toDate(),
                categoryName = categoryName
            )
        }
    }

    // -----------------------------------------------------------
    // 🔹 Obtiene posts recientes (últimos 5 días)
    // -----------------------------------------------------------
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

    // -----------------------------------------------------------
    // 🔹 Mapeo genérico de snapshot a entidad
    // -----------------------------------------------------------
    private fun mapToEntity(snap: DocumentSnapshot): PostEntity {
        val data = snap.data ?: emptyMap<String, Any?>()
        val categoryRef = data["category_id"] as? DocumentReference
        val categoryName = try {
            categoryRef?.get()?.result?.getString("name") ?: "Unknown"
        } catch (e: Exception) {
            "Unknown"
        }

        return PostEntity(
            id = snap.id,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            price = (data["price"] as? Number)?.toLong() ?: 0L,
            images = (data["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            userId = data["user_id"] as? String ?: "",         // ✅ reemplazado userRef → userId
            status = data["status"] as? String ?: "",
            createdAt = (data["created_at"] as? Timestamp)?.toDate(),
            categoryName = categoryName
        )
    }

    // -----------------------------------------------------------
    // 🔹 Obtiene post por ID (ya lo tienes bien)
    // -----------------------------------------------------------
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
            price = (data["price"] as? Number)?.toLong() ?: 0L,
            images = (data["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            userId = data["user_id"] as? String ?: "",
            status = data["status"] as? String ?: "",
            createdAt = (data["created_at"] as? Timestamp)?.toDate(),
            categoryName = categoryName
        )
    }

    // -----------------------------------------------------------
    // 🔹 Busca posts por texto
    // -----------------------------------------------------------
    override suspend fun searchPosts(query: String, limit: Int): List<PostEntity> {
        if (query.isBlank()) return emptyList()

        val qs = db.collection("posts")
            .whereEqualTo("status", "active")
            .get()
            .await()

        val lowerQuery = query.lowercase()

        return qs.documents
            .map { snap -> mapToEntity(snap) }
            .filter { post ->
                post.title.lowercase().contains(lowerQuery) ||
                        post.description.lowercase().contains(lowerQuery)
            }
            .take(limit)
    }

    // -----------------------------------------------------------
    // 🔹 Obtiene nombre del usuario
    // -----------------------------------------------------------
    override suspend fun getUserNameById(userId: String): String {
        return try {
            if (userId.isBlank()) return "Usuario desconocido"
            val doc = db.collection("users").document(userId).get().await()
            doc.getString("name") ?: "Usuario desconocido"
        } catch (e: Exception) {
            Log.e("FirestorePostsRepository", "⚠️ Error al obtener nombre de usuario", e)
            "Usuario desconocido"
        }
    }


    fun getPostsByCategories(
        categories: List<String>,
        onResult: (List<PostEntity>) -> Unit
    ) {
        if (categories.isEmpty()) {
            onResult(emptyList())
            return
        }

        val normalized = categories.map { it.lowercase() }

        db.collection("posts")
            .whereIn("category_name", normalized.take(10))
            .get()
            .addOnSuccessListener { result ->
                val posts = result.documents.mapNotNull { doc ->
                    doc.toObject(PostEntity::class.java)?.copy(id = doc.id)
                }

                android.util.Log.d("RECS", "Posts encontrados: ${posts.size}")
                onResult(posts)
            }
            .addOnFailureListener {
                android.util.Log.e("RECS", "Error buscando posts", it)
                onResult(emptyList())
            }
    }
}
