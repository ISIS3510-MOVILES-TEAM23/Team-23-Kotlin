package com.example.team_23_kotlin.data.local

import android.content.Context
import android.util.Log
import com.example.team_23_kotlin.data.posts.PostEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

private const val PREFS_NAME = "posts_cache_storage"

class PostsCacheStorage(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun savePosts(key: String, posts: List<PostEntity>) {
        try {
            val jsonArray = JSONArray()
            posts.forEach { post ->
                jsonArray.put(post.toJson())
                savePostDetailInternal(post)
            }
            prefs.edit()
                .putString(key, jsonArray.toString())
                .putLong("${key}_updated_at", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando posts en cache", e)
        }
    }

    fun loadPosts(key: String): List<PostEntity>? {
        return try {
            val raw = prefs.getString(key, null) ?: return null
            val array = JSONArray(raw)
            val list = mutableListOf<PostEntity>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(obj.toPostEntity())
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo posts cacheados", e)
            null
        }
    }

    fun savePostDetail(post: PostEntity) {
        try {
            savePostDetailInternal(post)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando detalle de post", e)
        }
    }

    fun loadPostDetail(id: String): PostEntity? {
        return try {
            val raw = prefs.getString(detailKey(id), null) ?: return null
            val obj = JSONObject(raw)
            obj.toPostEntity()
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo detalle cacheado", e)
            null
        }
    }

    fun saveUserName(userId: String, name: String) {
        try {
            saveUserNameInternal(userId, name)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando nombre de usuario", e)
        }
    }

    fun loadUserName(userId: String): String? {
        return try {
            prefs.getString(userKey(userId), null)
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo nombre de usuario cacheado", e)
            null
        }
    }

    fun lastUpdated(key: String): Long? {
        return if (prefs.contains("${key}_updated_at")) {
            prefs.getLong("${key}_updated_at", 0L)
        } else {
            null
        }
    }

    private fun PostEntity.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("title", title)
            put("description", description)
            put("price", price)
            put("images", JSONArray().apply { images.forEach { put(it) } })
            put("userId", userId)
            put("status", status)
            if (createdAt != null) put("createdAt", createdAt.time) else put("createdAt", JSONObject.NULL)
            put("categoryName", categoryName)
            put("pickupName", pickupName)
            put("pickupCoords", pickupCoords)
        }
    }

    private fun JSONObject.toPostEntity(): PostEntity {
        val imagesArray = optJSONArray("images") ?: JSONArray()
        val imagesList = mutableListOf<String>()
        for (i in 0 until imagesArray.length()) {
            imagesList.add(imagesArray.optString(i))
        }

        val createdAtValue = if (isNull("createdAt")) null else optLong("createdAt")

        return PostEntity(
            id = optString("id"),
            title = optString("title"),
            description = optString("description"),
            price = optLong("price", 0L),
            images = imagesList,
            userId = optString("userId"),
            status = optString("status"),
            createdAt = createdAtValue?.let { Date(it) },
            categoryName = optString("categoryName", "Unknown"),
            pickupName = optString("pickupName"),
            pickupCoords = optString("pickupCoords")
        )
    }

    private fun savePostDetailInternal(post: PostEntity) {
        prefs.edit()
            .putString(detailKey(post.id), post.toJson().toString())
            .putLong("${detailKey(post.id)}_updated_at", System.currentTimeMillis())
            .apply()
    }

    private fun saveUserNameInternal(userId: String, name: String) {
        prefs.edit()
            .putString(userKey(userId), name)
            .putLong("${userKey(userId)}_updated_at", System.currentTimeMillis())
            .apply()
    }

    private fun detailKey(id: String) = "detail_$id"

    private fun userKey(id: String) = "user_$id"

    companion object {
        private const val TAG = "PostsCacheStorage"
    }
}


