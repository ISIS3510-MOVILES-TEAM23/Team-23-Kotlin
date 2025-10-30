package com.example.team_23_kotlin.data.local

import android.util.LruCache
import com.example.team_23_kotlin.data.posts.PostEntity

class PostsMemoryCache(
    postsMaxEntries: Int = 6,
    detailsMaxEntries: Int = 32,
    userNamesMaxEntries: Int = 32
) {

    private val listsCache = object : LruCache<String, List<PostEntity>>(postsMaxEntries) {
        override fun sizeOf(key: String, value: List<PostEntity>): Int = value.size.coerceAtLeast(1)
    }

    private val detailsCache = object : LruCache<String, PostEntity>(detailsMaxEntries) {
        override fun sizeOf(key: String, value: PostEntity): Int = 1
    }

    private val userNamesCache = object : LruCache<String, String>(userNamesMaxEntries) {
        override fun sizeOf(key: String, value: String): Int = 1
    }

    fun putList(key: String, posts: List<PostEntity>) {
        listsCache.put(key, posts)
        posts.forEach { putDetail(it) }
    }

    fun getList(key: String): List<PostEntity>? = listsCache.get(key)

    fun putDetail(post: PostEntity) {
        detailsCache.put(post.id, post)
    }

    fun getDetail(id: String): PostEntity? = detailsCache.get(id)

    fun putUserName(userId: String, name: String) {
        userNamesCache.put(userId, name)
    }

    fun getUserName(userId: String): String? = userNamesCache.get(userId)

    fun clearAll() {
        listsCache.evictAll()
        detailsCache.evictAll()
        userNamesCache.evictAll()
    }
}

object SharedPostsMemoryCache {
    val instance: PostsMemoryCache = PostsMemoryCache()
}


