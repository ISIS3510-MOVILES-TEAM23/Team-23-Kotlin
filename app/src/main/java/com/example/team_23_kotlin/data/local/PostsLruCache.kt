package com.example.team_23_kotlin.data.local

import android.util.LruCache
import com.example.team_23_kotlin.data.posts.PostEntity

/**
 * LRU Cache para posts recientes
 * Optimización: Evita re-fetch de posts ya cargados
 * Tamaño: 50 posts máximo (~2-3 MB en memoria)
 */
object PostsLruCache {
    private const val MAX_SIZE = 50
    private val cache = LruCache<String, PostEntity>(MAX_SIZE)
    
    /**
     * Guarda un post en cache
     */
    fun put(postId: String, post: PostEntity) {
        cache.put(postId, post)
    }
    
    /**
     * Obtiene un post desde cache
     */
    fun get(postId: String): PostEntity? {
        return cache.get(postId)
    }
    
    /**
     * Obtiene todos los posts cacheados
     */
    fun getAll(): List<PostEntity> {
        val snapshot = cache.snapshot()
        return snapshot.values.toList()
    }
    
    /**
     * Obtiene los N posts más recientes
     */
    fun getRecent(limit: Int): List<PostEntity> {
        return getAll().take(limit)
    }
    
    /**
     * Elimina un post del cache
     */
    fun remove(postId: String) {
        cache.remove(postId)
    }
    
    /**
     * Limpia todo el cache
     */
    fun clear() {
        cache.evictAll()
    }
    
    /**
     * Verifica si un post existe en cache
     */
    fun contains(postId: String): Boolean {
        return cache.get(postId) != null
    }
    
    /**
     * Obtiene el tamaño actual del cache
     */
    fun size(): Int {
        return cache.size()
    }
}
