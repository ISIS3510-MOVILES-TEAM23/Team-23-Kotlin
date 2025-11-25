package com.example.team_23_kotlin.domain.repository

import android.net.Uri
import com.example.team_23_kotlin.data.purchases.FeedbackEntity

interface FeedbackRepository {
    suspend fun submitFeedback(feedback: FeedbackEntity, photoUris: List<Uri>): Boolean
    suspend fun updateFeedback(feedbackId: String, feedback: FeedbackEntity, photoUris: List<Uri>): Boolean
    suspend fun getFeedbackForPurchase(purchaseId: String): FeedbackEntity?
    suspend fun hasFeedback(purchaseId: String): Boolean
}
