package com.example.team_23_kotlin.data.local

import android.content.Context
import android.util.Log
import com.example.team_23_kotlin.data.sales.SaleEntity
import org.json.JSONArray
import org.json.JSONObject
import java.util.Date

private const val PREFS_NAME = "sales_cache_storage"
private const val KEY_SALES = "cache_sales"

class SalesCacheStorage(context: Context) {

    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun saveSales(userId: String, sales: List<SaleEntity>) {
        try {
            val jsonArray = JSONArray()
            sales.forEach { sale ->
                jsonArray.put(sale.toJson())
                saveSaleDetailInternal(sale)
            }
            prefs.edit()
                .putString("${KEY_SALES}_$userId", jsonArray.toString())
                .putLong("${KEY_SALES}_${userId}_updated_at", System.currentTimeMillis())
                .apply()
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando ventas en cache", e)
        }
    }

    fun loadSales(userId: String): List<SaleEntity>? {
        return try {
            val raw = prefs.getString("${KEY_SALES}_$userId", null) ?: return null
            val array = JSONArray(raw)
            val list = mutableListOf<SaleEntity>()
            for (i in 0 until array.length()) {
                val obj = array.optJSONObject(i) ?: continue
                list.add(obj.toSaleEntity())
            }
            list
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo ventas cacheadas", e)
            null
        }
    }

    fun saveSaleDetail(sale: SaleEntity) {
        try {
            saveSaleDetailInternal(sale)
        } catch (e: Exception) {
            Log.e(TAG, "Error guardando detalle de venta", e)
        }
    }

    fun loadSaleDetail(id: String): SaleEntity? {
        return try {
            val raw = prefs.getString(detailKey(id), null) ?: return null
            val obj = JSONObject(raw)
            obj.toSaleEntity()
        } catch (e: Exception) {
            Log.e(TAG, "Error leyendo detalle cacheado", e)
            null
        }
    }

    fun lastUpdated(userId: String): Long? {
        return if (prefs.contains("${KEY_SALES}_${userId}_updated_at")) {
            prefs.getLong("${KEY_SALES}_${userId}_updated_at", 0L)
        } else {
            null
        }
    }

    private fun SaleEntity.toJson(): JSONObject {
        return JSONObject().apply {
            put("id", id)
            put("buyerId", buyerId)
            put("sellerId", sellerId)
            put("postId", postId)
            put("price", price)
            put("status", status)
            if (createdAt != null) put("createdAt", createdAt.time) else put("createdAt", JSONObject.NULL)
            if (updatedAt != null) put("updatedAt", updatedAt.time) else put("updatedAt", JSONObject.NULL)
            put("postTitle", postTitle)
            put("postImages", JSONArray().apply { postImages.forEach { put(it) } })
            put("buyerName", buyerName)
            put("buyerEmail", buyerEmail)
        }
    }

    private fun JSONObject.toSaleEntity(): SaleEntity {
        val imagesArray = optJSONArray("postImages") ?: JSONArray()
        val imagesList = mutableListOf<String>()
        for (i in 0 until imagesArray.length()) {
            imagesList.add(imagesArray.optString(i))
        }

        val createdAtValue = if (isNull("createdAt")) null else optLong("createdAt")
        val updatedAtValue = if (isNull("updatedAt")) null else optLong("updatedAt")

        return SaleEntity(
            id = optString("id"),
            buyerId = optString("buyerId"),
            sellerId = optString("sellerId"),
            postId = optString("postId"),
            price = optLong("price", 0L),
            status = optString("status", "pending"),
            createdAt = createdAtValue?.let { Date(it) },
            updatedAt = updatedAtValue?.let { Date(it) },
            postTitle = optString("postTitle", ""),
            postImages = imagesList,
            buyerName = optString("buyerName", ""),
            buyerEmail = optString("buyerEmail", "")
        )
    }

    private fun saveSaleDetailInternal(sale: SaleEntity) {
        prefs.edit()
            .putString(detailKey(sale.id), sale.toJson().toString())
            .putLong("${detailKey(sale.id)}_updated_at", System.currentTimeMillis())
            .apply()
    }

    private fun detailKey(id: String) = "sale_detail_$id"

    companion object {
        private const val TAG = "SalesCacheStorage"
    }
}

