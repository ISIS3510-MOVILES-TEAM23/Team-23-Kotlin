package com.example.team_23_kotlin.data.purchases

import java.util.Date

data class PurchaseEntity(
    val id: String = "",
    val postId: String = "",
    val title: String = "",
    val description: String = "",
    val price: Double = 0.0,
    val postImages: List<String> = emptyList(),

    val sellerId: String = "",
    val sellerName: String = "",
    val sellerImage: String? = null,

    val buyerId: String = "",
    val buyerName: String = "",

    val status: String = "pending", // pending | completed
    val createdAt: Date = Date(),
    val isDownloaded: Boolean = false
)
