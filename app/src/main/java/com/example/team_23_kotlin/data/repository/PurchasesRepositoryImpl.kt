package com.example.team_23_kotlin.data.repository

import android.os.Build
import android.util.Log
import com.example.team_23_kotlin.data.purchases.PurchaseEntity
import com.example.team_23_kotlin.domain.repository.PurchasesRepository
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import android.util.LruCache
import androidx.annotation.RequiresApi
import com.example.team_23_kotlin.data.local.room.ReceiptDao
import com.example.team_23_kotlin.data.local.room.ReceiptEntity
import com.google.firebase.auth.FirebaseAuth


class PurchasesRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val receiptDao: ReceiptDao

) : PurchasesRepository {
    private val purchasesLruCache = object : LruCache<String, PurchaseEntity>(10) {}


    companion object {
        private const val TAG = "PurchasesRepositoryImpl"
        private const val COLLECTION_SALES = "sales"
        private const val COLLECTION_USERS = "users"
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun getPurchases(): List<PurchaseEntity> {
        // 1. Si ya tenemos cache, devolverla inmediatamente
        if (purchasesLruCache.size() > 0) {
            val cachedList = purchasesLruCache.snapshot().values.toList()
            if (cachedList.isNotEmpty()) return cachedList
        }

        return try {
            val uid = FirebaseAuth.getInstance().uid ?: return emptyList()

            val snapshot = db.collection(COLLECTION_SALES)
                .whereEqualTo("buyer_ref", db.document("$COLLECTION_USERS/$uid"))
                .get(Source.SERVER)
                .await()

            val purchases = snapshot.documents.mapNotNull { doc ->
                mapToPurchaseEntity(doc)
            }

            // 🔥 GUARDAR EN CACHE LRU
            purchases.forEach { purchase ->
                purchasesLruCache.put(purchase.id, purchase)
            }

            purchases

        } catch (e: Exception) {

            // 🔥 Si falla Firestore: regresar lo que haya en cache
            val cached = mutableListOf<PurchaseEntity>()
            for (key in purchasesLruCache.snapshot().keys) {
                purchasesLruCache.get(key)?.let { cached.add(it) }
            }

            if (cached.isNotEmpty()) return cached

            emptyList()
        }
    }


    override suspend fun savePurchasesToLocal(purchases: List<PurchaseEntity>) {
        // Not implemented (you said no cache)
    }

    override suspend fun getLocalPurchases(): List<PurchaseEntity> {
        // Not implemented
        return emptyList()
    }

    // ========================
// 🔥 ROOM: RECIBOS
// ========================

    override suspend fun markReceiptGenerated(purchaseId: String) {
        receiptDao.insert(
            ReceiptEntity(
                purchaseId = purchaseId,
                timestamp = System.currentTimeMillis()
            )
        )
    }

    override suspend fun isReceiptGenerated(purchaseId: String): Boolean {
        return receiptDao.getReceipt(purchaseId) != null
    }

    override suspend fun getAllGeneratedReceipts(): List<String> {
        return receiptDao.getAll()
    }


    // 📌 Mapea documento → PurchaseEntity
    private suspend fun mapToPurchaseEntity(doc: DocumentSnapshot): PurchaseEntity? {
        return try {
            val data = doc.data ?: return null

            val buyerRef = data["buyer_ref"] as? com.google.firebase.firestore.DocumentReference
            val sellerRef = data["seller_ref"] as? com.google.firebase.firestore.DocumentReference
            val postRef   = data["post_ref"] as? com.google.firebase.firestore.DocumentReference

            val buyerId = buyerRef?.id ?: ""
            val sellerId = sellerRef?.id ?: ""
            val postId = postRef?.id ?: ""

            // 🟦 Obtener datos del post
            val postData = try { postRef?.get()?.await()?.data } catch (_: Exception) { null }
            val title = postData?.get("title") as? String ?: "No title"
            val images = (postData?.get("images") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()
            val price = (data["price"] as? Number)?.toDouble() ?: 0.0

            // 🟩 Obtener nombre del vendedor
            val sellerData = try { sellerRef?.get()?.await()?.data } catch (_: Exception) { null }
            val sellerName = sellerData?.get("name") as? String ?: "Unknown seller"

            // 🟨 Obtener nombre del comprador (opcional)
            val buyerData = try { buyerRef?.get()?.await()?.data } catch (_: Exception) { null }
            val buyerName = buyerData?.get("name") as? String ?: "Unknown buyer"

            val status = data["status"] as? String ?: "pending"
            val createdAt = (data["created_at"] as? Timestamp)?.toDate() ?: java.util.Date()
            val hasFeedback = data["hasFeedback"] as? Boolean ?: false

            // 🔥 Crear entidad final
            PurchaseEntity(
                id = doc.id,
                postId = postId,
                title = title,
                description = "",
                price = price,
                postImages = images,
                sellerId = sellerId,
                sellerName = sellerName,
                buyerId = buyerId,
                buyerName = buyerName,
                status = status,
                createdAt = createdAt,
                hasFeedback = hasFeedback
            )

        } catch (e: Exception) {
            Log.e(TAG, "Error mapping purchase document", e)
            null
        }
    }

    override fun invalidateCache() {
        purchasesLruCache.evictAll()
        Log.d(TAG, "✅ Purchases cache invalidated")
    }
}
