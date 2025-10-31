package com.example.team_23_kotlin.data.posts

import android.net.Uri
import android.util.Log
import com.example.team_23_kotlin.data.local.PostsCacheStorage
import com.example.team_23_kotlin.data.local.PostsMemoryCache
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.ktx.storageMetadata
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class FirestorePostsRepository(
    private val db: FirebaseFirestore,
    private val cache: PostsCacheStorage? = null,
    private val memoryCache: PostsMemoryCache? = null,
    private val storage: FirebaseStorage = FirebaseStorage.getInstance(),
    private val auth: FirebaseAuth = FirebaseAuth.getInstance(),
    private val isOnline: (() -> Boolean)? = null
) : PostsRepository {

    // -----------------------------------------------------------
    // Obtiene posts activos
    // -----------------------------------------------------------
    override suspend fun getActivePosts(limit: Int): List<PostEntity> {
        if (isOnline?.invoke() == false) {
            memoryCache?.getList(CACHE_KEY_ACTIVE)?.take(limit)?.takeIf { it.isNotEmpty() }?.let { return it }
            return cache?.loadPosts(CACHE_KEY_ACTIVE)?.take(limit)
                ?: cache?.loadPosts(CACHE_KEY_NEW)?.take(limit)
                ?: emptyList()
        }

        return try {
            val qs = db.collection("posts")
                .whereEqualTo("status", "active")
                .limit(limit.toLong())
                .get(Source.SERVER)
                .await()

            val posts = qs.documents.map { snap -> mapToEntity(snap) }
            cache?.savePosts(CACHE_KEY_ACTIVE, posts)
            memoryCache?.putList(CACHE_KEY_ACTIVE, posts)
            posts
        } catch (e: Exception) {
            memoryCache?.getList(CACHE_KEY_ACTIVE)?.take(limit)?.takeIf { it.isNotEmpty() }
                ?: cache?.loadPosts(CACHE_KEY_ACTIVE)?.take(limit)?.takeIf { it.isNotEmpty() }
                ?: throw e
        }
    }

    // -----------------------------------------------------------
    // Obtiene posts recientes (últimos 5 días)
    // -----------------------------------------------------------
    override suspend fun getNewPosts(limit: Int): List<PostEntity> {
        val fiveDaysAgo = Timestamp(
            Date(System.currentTimeMillis() - 5 * 24 * 60 * 60 * 1000L)
        )

        if (isOnline?.invoke() == false) {
            memoryCache?.getList(CACHE_KEY_NEW)?.take(limit)?.takeIf { it.isNotEmpty() }?.let { return it }
            return cache?.loadPosts(CACHE_KEY_NEW)?.take(limit) ?: emptyList()
        }

        return try {
            val qs = db.collection("posts")
                .whereGreaterThanOrEqualTo("created_at", fiveDaysAgo)
                .limit(limit.toLong())
                .get(Source.SERVER)
                .await()

            val posts = qs.documents
                .map { snap -> mapToEntity(snap) }
                .sortedByDescending { it.createdAt }
            cache?.savePosts(CACHE_KEY_NEW, posts)
            memoryCache?.putList(CACHE_KEY_NEW, posts)
            posts
        } catch (e: Exception) {
            memoryCache?.getList(CACHE_KEY_NEW)?.take(limit)?.takeIf { it.isNotEmpty() }
                ?: cache?.loadPosts(CACHE_KEY_NEW)?.take(limit)?.takeIf { it.isNotEmpty() }
                ?: throw e
        }
    }

    // -----------------------------------------------------------
    // Mapeo genérico de snapshot a entidad
    // -----------------------------------------------------------
    private fun mapToEntity(snap: DocumentSnapshot): PostEntity {
        val data = snap.data ?: emptyMap<String, Any?>()
        val categoryName = data["category_name"] as? String ?: "Unknown"

        return PostEntity(
            id = snap.id,
            title = data["title"] as? String ?: "",
            description = data["description"] as? String ?: "",
            price = (data["price"] as? Number)?.toLong() ?: 0L,
            images = (data["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            userId = data["user_id"] as? String ?: "",
            status = data["status"] as? String ?: "",
            createdAt = (data["created_at"] as? Timestamp)?.toDate(),
            categoryName = categoryName,
            pickupName = data["pickup_point_name"] as? String ?: "",
            pickupCoords = data["pickup_coordinates"] as? String ?: "",

            )
    }

    // -----------------------------------------------------------
    // Obtiene post por ID
    // -----------------------------------------------------------
    override suspend fun getPostById(id: String): PostEntity {
        if (isOnline?.invoke() == false) {
            memoryCache?.getDetail(id)?.let { return it }
            return cache?.loadPostDetail(id) ?: error("Post not available offline")
        }

        return try {
            val snap = db.collection("posts").document(id).get(Source.SERVER).await()
            if (!snap.exists()) error("Post not found")
            val data = snap.data ?: emptyMap<String, Any?>()

            val categoryName = data["category_name"] as? String ?: "Unknown"

            val post = PostEntity(
                id = snap.id,
                title = data["title"] as? String ?: "",
                description = data["description"] as? String ?: "",
                price = (data["price"] as? Number)?.toLong() ?: 0L,
                images = (data["images"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                userId = data["user_id"] as? String ?: "",
                status = data["status"] as? String ?: "",
                createdAt = (data["created_at"] as? Timestamp)?.toDate(),
                categoryName = categoryName,
                pickupName = data["pickup_point_name"] as? String ?: "",
                pickupCoords = data["pickup_coordinates"] as? String ?: "",

                )
            cache?.savePostDetail(post)
            memoryCache?.putDetail(post)
            post
        } catch (e: Exception) {
            memoryCache?.getDetail(id) ?: cache?.loadPostDetail(id) ?: throw e
        }
    }

    // -----------------------------------------------------------
    // Busca posts por texto
    // -----------------------------------------------------------
    override suspend fun searchPosts(query: String, limit: Int): List<PostEntity> {
        if (query.isBlank()) return emptyList()

        if (isOnline?.invoke() == false) {
            val searchKey = searchKey(query)
            val cachedSources = listOfNotNull(
                memoryCache?.getList(searchKey),
                memoryCache?.getList(CACHE_KEY_ACTIVE),
                memoryCache?.getList(CACHE_KEY_NEW),
                cache?.loadPosts(searchKey),
                cache?.loadPosts(CACHE_KEY_ACTIVE),
                cache?.loadPosts(CACHE_KEY_NEW)
            ).flatten()

            if (cachedSources.isEmpty()) return emptyList()

            val lowerQuery = query.lowercase()
            return cachedSources
                .filter { post ->
                    post.title.lowercase().contains(lowerQuery) ||
                            post.description.lowercase().contains(lowerQuery)
                }
                .distinctBy { it.id }
                .take(limit)
        }

        return try {
            val qs = db.collection("posts")
                .whereEqualTo("status", "active")
                .get(Source.SERVER)
                .await()

            val lowerQuery = query.lowercase()

            val results = qs.documents
                .map { snap -> mapToEntity(snap) }
                .filter { post ->
                    post.title.lowercase().contains(lowerQuery) ||
                            post.description.lowercase().contains(lowerQuery)
                }
                .take(limit)

            val searchKey = searchKey(query)
            memoryCache?.putList(searchKey, results)
            cache?.savePosts(searchKey, results)
            results
        } catch (e: Exception) {
            val cachedSources = listOfNotNull(
                memoryCache?.getList(searchKey(query)),
                memoryCache?.getList(CACHE_KEY_ACTIVE),
                memoryCache?.getList(CACHE_KEY_NEW),
                cache?.loadPosts(searchKey(query)),
                cache?.loadPosts(CACHE_KEY_ACTIVE),
                cache?.loadPosts(CACHE_KEY_NEW)
            ).flatten()

            if (cachedSources.isEmpty()) throw e

            val lowerQuery = query.lowercase()
            cachedSources
                .filter { post ->
                    post.title.lowercase().contains(lowerQuery) ||
                            post.description.lowercase().contains(lowerQuery)
                }
                .distinctBy { it.id }
                .take(limit)
        }
    }

    // -----------------------------------------------------------
    // Obtiene nombre del usuario
    // -----------------------------------------------------------
    override suspend fun getUserNameById(userId: String): String {
        if (isOnline?.invoke() == false) {
            return memoryCache?.getUserName(userId)
                ?: cache?.loadUserName(userId)
                ?: "Usuario desconocido"
        }

        return try {
            if (userId.isBlank()) return "Usuario desconocido"
            val doc = db.collection("users").document(userId).get(Source.SERVER).await()
            val name = doc.getString("name") ?: "Usuario desconocido"
            cache?.saveUserName(userId, name)
            memoryCache?.putUserName(userId, name)
            name
        } catch (e: Exception) {
            Log.e("FirestorePostsRepository", "⚠️ Error al obtener nombre de usuario", e)
            memoryCache?.getUserName(userId)
                ?: cache?.loadUserName(userId)
                ?: "Usuario desconocido"
        }
    }

    // -----------------------------------------------------------
    // Obtiene posts por categoría
    // -----------------------------------------------------------
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

                Log.d("RECS", "Posts encontrados: ${posts.size}")
                onResult(posts)
            }
            .addOnFailureListener {
                Log.e("RECS", "Error buscando posts", it)
                onResult(emptyList())
            }
    }

    /** Cargar categorías desde Firestore **/
    suspend fun getCategories(): List<CategoryEntity> {
        val snap = db.collection("categories").get().await()
        return snap.documents.mapNotNull { doc ->
            val name = doc.getString("name") ?: return@mapNotNull null
            CategoryEntity(id = doc.id, name = name)
        }
    }

    /** Subir imagen a Firebase Storage **/
    private suspend fun uploadImage(uid: String, uri: Uri): String? {
        return try {
            val productId = UUID.randomUUID().toString()
            val fileName = "img_${System.currentTimeMillis()}.jpg"

            val ref = storage.reference
                .child("public/products/$uid/$productId/$fileName")

            val metadata = storageMetadata {
                contentType = "image/jpeg"
                setCustomMetadata("ownerUid", uid)
            }

            ref.putFile(uri, metadata).await()
            ref.downloadUrl.await().toString()
        } catch (e: Exception) {
            Log.e("FirestorePostsRepository", "Error subiendo imagen: ${e.message}")
            null
        }
    }

    /** Crear nuevo post **/
    suspend fun createPost(post: Post, uris: List<Uri>): Boolean {
        val uid = auth.currentUser?.uid ?: "anonymous"
        if (auth.currentUser == null) auth.signInAnonymously().await()

        // Subir imágenes
        val urls = mutableListOf<String>()
        for (uri in uris) {
            uploadImage(uid, uri)?.let { urls.add(it) }
        }

        val newDoc = db.collection("posts").document()
        val data = mapOf(
            "_id" to newDoc.id,
            "title" to post.title,
            "description" to post.description,
            "price" to post.price,
            "images" to urls,
            "category_id" to post.category,
            "category_name" to post.category_name,
            "status" to "active",
            "created_at" to Timestamp.now(),
            "user_id" to uid,
            "pickup_point_name" to post.pickup_point_name,
            "pickup_coordinates" to post.pickup_coordinates
        )

        newDoc.set(data).await()
        return true
    }
}


// -----------------------------------------------------------
// Entidad auxiliar para categorías
// -----------------------------------------------------------
data class CategoryEntity(
    val id: String,
    val name: String
)

private const val CACHE_KEY_ACTIVE = "cache_active_posts"
private const val CACHE_KEY_NEW = "cache_new_posts"
private fun searchKey(query: String) = "search_${query.lowercase()}"
