package com.example.team_23_kotlin.data.purchases

import java.util.Date

data class FeedbackEntity(
    val id: String = "",
    val purchaseId: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val rating: Int = 0, // 1-5 estrellas
    val comment: String = "",
    val photos: List<String> = emptyList(), // URLs de Firebase Storage
    val createdAt: Date = Date()
)
