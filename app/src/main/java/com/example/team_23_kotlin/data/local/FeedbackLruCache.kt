package com.example.team_23_kotlin.data.local

import android.util.LruCache
import com.example.team_23_kotlin.data.purchases.FeedbackEntity

/**
 * LRU Cache singleton para almacenar feedbacks en memoria
 * Máximo 20 feedbacks para optimizar consultas frecuentes
 */
object FeedbackLruCache {
    private const val MAX_SIZE = 20
    private val cache = LruCache<String, FeedbackEntity>(MAX_SIZE)

    /**
     * Guarda un feedback en cache usando purchaseId como key
     */
    fun put(purchaseId: String, feedback: FeedbackEntity) {
        cache.put(purchaseId, feedback)
    }

    /**
     * Obtiene un feedback desde cache
     * @return FeedbackEntity si existe en cache, null si no
     */
    fun get(purchaseId: String): FeedbackEntity? {
        return cache.get(purchaseId)
    }

    /**
     * Elimina un feedback específico del cache
     */
    fun remove(purchaseId: String) {
        cache.remove(purchaseId)
    }

    /**
     * Limpia todo el cache
     */
    fun clear() {
        cache.evictAll()
    }

    /**
     * Obtiene el tamaño actual del cache
     */
    fun size(): Int {
        return cache.size()
    }

    /**
     * Verifica si existe un feedback en cache
     */
    fun contains(purchaseId: String): Boolean {
        return cache.get(purchaseId) != null
    }
}
