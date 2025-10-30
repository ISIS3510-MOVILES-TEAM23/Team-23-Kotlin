package com.example.team_23_kotlin.data.sales

import android.util.Log
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source
import kotlinx.coroutines.tasks.await

/**
 * Implementación del repositorio de ventas usando Firestore
 */
class FirestoreSalesRepository(
    private val db: FirebaseFirestore
) : SalesRepository {

    companion object {
        private const val TAG = "SalesRepository"
        private const val COLLECTION_SALES = "sales"
        private const val COLLECTION_POSTS = "posts"
        private const val COLLECTION_USERS = "users"
    }

    override suspend fun getSalesBySeller(userId: String, limit: Int): List<SaleEntity> {
        return try {
            Log.d(TAG, "Obteniendo ventas para el vendedor: $userId")
            
            // Obtener todas las ventas donde el usuario es el vendedor
            val salesSnapshot = db.collection(COLLECTION_SALES)
                .whereEqualTo("seller_ref", db.document("users/$userId"))
                .orderBy("created_at", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get(Source.SERVER)
                .await()

            val sales = salesSnapshot.documents.mapNotNull { doc ->
                mapToSaleEntity(doc)
            }

            Log.d(TAG, "Se encontraron ${sales.size} ventas")
            sales
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener ventas", e)
            emptyList()
        }
    }

    override suspend fun getSaleById(saleId: String): SaleEntity? {
        return try {
            val doc = db.collection(COLLECTION_SALES)
                .document(saleId)
                .get(Source.SERVER)
                .await()
            
            mapToSaleEntity(doc)
        } catch (e: Exception) {
            Log.e(TAG, "Error al obtener venta por ID: $saleId", e)
            null
        }
    }

    /**
     * Mapea un documento de Firestore a una entidad de venta
     */
    private suspend fun mapToSaleEntity(doc: DocumentSnapshot): SaleEntity? {
        return try {
            val data = doc.data ?: return null

            // Extraer IDs de las referencias
            val buyerRef = data["buyer_ref"] as? com.google.firebase.firestore.DocumentReference
            val sellerRef = data["seller_ref"] as? com.google.firebase.firestore.DocumentReference
            val postRef = data["post_ref"] as? com.google.firebase.firestore.DocumentReference

            val buyerId = buyerRef?.id ?: ""
            val sellerId = sellerRef?.id ?: ""
            val postId = postRef?.id ?: ""

            // Obtener información del post
            val postData = if (postRef != null) {
                try {
                    postRef.get().await().data
                } catch (e: Exception) {
                    Log.e(TAG, "Error al obtener datos del post: $postId", e)
                    null
                }
            } else null

            val postTitle = postData?.get("title") as? String ?: "Sin título"
            val postImages = (postData?.get("images") as? List<*>)?.mapNotNull { it as? String } ?: emptyList()

            // Obtener información del comprador
            val buyerData = if (buyerRef != null) {
                try {
                    buyerRef.get().await().data
                } catch (e: Exception) {
                    Log.e(TAG, "Error al obtener datos del comprador: $buyerId", e)
                    null
                }
            } else null

            val buyerName = buyerData?.get("name") as? String ?: "Usuario desconocido"
            val buyerEmail = buyerData?.get("email") as? String ?: ""

            SaleEntity(
                id = doc.id,
                buyerId = buyerId,
                sellerId = sellerId,
                postId = postId,
                price = (data["price"] as? Number)?.toLong() ?: 0L,
                status = data["status"] as? String ?: "pending",
                createdAt = (data["created_at"] as? Timestamp)?.toDate(),
                updatedAt = (data["updated_at"] as? Timestamp)?.toDate(),
                postTitle = postTitle,
                postImages = postImages,
                buyerName = buyerName,
                buyerEmail = buyerEmail
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error al mapear documento de venta", e)
            null
        }
    }
}

