package com.example.team_23_kotlin.data.repository

import android.net.Uri
import android.util.Log
import com.example.team_23_kotlin.data.local.FeedbackLruCache
import com.example.team_23_kotlin.data.purchases.FeedbackEntity
import com.example.team_23_kotlin.domain.repository.FeedbackRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import java.util.UUID

class FeedbackRepositoryImpl @Inject constructor(
    private val db: FirebaseFirestore,
    private val storage: FirebaseStorage,
    private val auth: FirebaseAuth
) : FeedbackRepository {

    companion object {
        private const val TAG = "FeedbackRepository"
        private const val COLLECTION_FEEDBACKS = "feedbacks"
        private const val STORAGE_PATH = "feedbacks"
    }

    override suspend fun submitFeedback(feedback: FeedbackEntity, photoUris: List<Uri>): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false

            // 1. Subir fotos a Firebase Storage
            val uploadedPhotoUrls = photoUris.mapNotNull { uri ->
                uploadPhoto(userId, feedback.purchaseId, uri)
            }

            // 2. Crear documento de feedback en Firestore
            val feedbackData = hashMapOf(
                "purchaseId" to feedback.purchaseId,
                "buyerId" to feedback.buyerId,
                "sellerId" to feedback.sellerId,
                "rating" to feedback.rating,
                "comment" to feedback.comment,
                "photos" to uploadedPhotoUrls,
                "createdAt" to com.google.firebase.Timestamp.now()
            )

            db.collection(COLLECTION_FEEDBACKS)
                .add(feedbackData)
                .await()

            // 3. Actualizar la compra para marcar que tiene feedback
            try {
                db.collection("sales")
                    .document(feedback.purchaseId)
                    .update("hasFeedback", true)
                    .await()
                Log.d(TAG, "✅ Purchase marked as having feedback")
            } catch (e: Exception) {
                Log.w(TAG, "⚠️ Could not update purchase hasFeedback flag: ${e.message}")
            }

            // 4. Invalidar cache
            FeedbackLruCache.remove(feedback.purchaseId)

            Log.d(TAG, "✅ Feedback submitted successfully for purchase ${feedback.purchaseId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error submitting feedback", e)
            false
        }
    }

    override suspend fun updateFeedback(feedbackId: String, feedback: FeedbackEntity, photoUris: List<Uri>): Boolean {
        return try {
            val userId = auth.currentUser?.uid ?: return false

            // 1. Subir nuevas fotos a Firebase Storage
            val newPhotoUrls = photoUris.mapNotNull { uri ->
                uploadPhoto(userId, feedback.purchaseId, uri)
            }

            // 2. Combinar fotos existentes con nuevas
            val allPhotos = feedback.photos + newPhotoUrls

            // 3. Actualizar documento de feedback en Firestore
            val feedbackData = hashMapOf(
                "rating" to feedback.rating,
                "comment" to feedback.comment,
                "photos" to allPhotos,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )

            db.collection(COLLECTION_FEEDBACKS)
                .document(feedbackId)
                .update(feedbackData as Map<String, Any>)
                .await()

            // Invalidar cache
            FeedbackLruCache.remove(feedback.purchaseId)

            Log.d(TAG, "✅ Feedback updated successfully for purchase ${feedback.purchaseId}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error updating feedback", e)
            false
        }
    }

    override suspend fun getFeedbackForPurchase(purchaseId: String): FeedbackEntity? {
        return try {
            // 1. Intentar desde cache
            FeedbackLruCache.get(purchaseId)?.let {
                Log.d(TAG, "✅ Feedback loaded from cache for purchase $purchaseId")
                return it
            }

            // 2. Consultar Firestore
            val snapshot = db.collection(COLLECTION_FEEDBACKS)
                .whereEqualTo("purchaseId", purchaseId)
                .limit(1)
                .get()
                .await()

            if (snapshot.isEmpty) {
                null
            } else {
                val doc = snapshot.documents.first()
                val data = doc.data ?: return null

                val feedback = FeedbackEntity(
                    id = doc.id,
                    purchaseId = data["purchaseId"] as? String ?: "",
                    buyerId = data["buyerId"] as? String ?: "",
                    sellerId = data["sellerId"] as? String ?: "",
                    rating = (data["rating"] as? Long)?.toInt() ?: 0,
                    comment = data["comment"] as? String ?: "",
                    photos = (data["photos"] as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
                    createdAt = (data["createdAt"] as? com.google.firebase.Timestamp)?.toDate() ?: java.util.Date()
                )

                // 3. Guardar en cache
                FeedbackLruCache.put(purchaseId, feedback)
                Log.d(TAG, "✅ Feedback loaded from Firestore and cached for purchase $purchaseId")

                feedback
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting feedback for purchase $purchaseId", e)
            null
        }
    }

    override suspend fun hasFeedback(purchaseId: String): Boolean {
        return try {
            val snapshot = db.collection(COLLECTION_FEEDBACKS)
                .whereEqualTo("purchaseId", purchaseId)
                .limit(1)
                .get()
                .await()

            !snapshot.isEmpty
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error checking feedback for purchase $purchaseId", e)
            false
        }
    }

    private suspend fun uploadPhoto(userId: String, purchaseId: String, uri: Uri): String? {
        return try {
            val fileName = "${UUID.randomUUID()}.jpg"
            val ref = storage.reference
                .child("$STORAGE_PATH/$userId/$purchaseId/$fileName")

            ref.putFile(uri).await()
            val downloadUrl = ref.downloadUrl.await()
            downloadUrl.toString()
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error uploading photo", e)
            null
        }
    }
}
