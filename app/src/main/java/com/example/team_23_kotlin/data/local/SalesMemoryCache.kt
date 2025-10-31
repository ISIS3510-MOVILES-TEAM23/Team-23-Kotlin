package com.example.team_23_kotlin.data.local

import android.util.LruCache
import com.example.team_23_kotlin.data.sales.SaleEntity

class SalesMemoryCache(
    salesMaxEntries: Int = 10,
    detailsMaxEntries: Int = 32
) {

    private val salesCache = object : LruCache<String, List<SaleEntity>>(salesMaxEntries) {
        override fun sizeOf(key: String, value: List<SaleEntity>): Int = value.size.coerceAtLeast(1)
    }

    private val detailsCache = object : LruCache<String, SaleEntity>(detailsMaxEntries) {
        override fun sizeOf(key: String, value: SaleEntity): Int = 1
    }

    fun putSales(userId: String, sales: List<SaleEntity>) {
        salesCache.put(userId, sales)
        sales.forEach { putDetail(it) }
    }

    fun getSales(userId: String): List<SaleEntity>? = salesCache.get(userId)

    fun putDetail(sale: SaleEntity) {
        detailsCache.put(sale.id, sale)
    }

    fun getDetail(id: String): SaleEntity? = detailsCache.get(id)

    fun clearAll() {
        salesCache.evictAll()
        detailsCache.evictAll()
    }

    fun clearForUser(userId: String) {
        salesCache.remove(userId)
    }
}

object SharedSalesMemoryCache {
    val instance: SalesMemoryCache = SalesMemoryCache()
}

