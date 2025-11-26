package com.example.team_23_kotlin.data.local

data class FeedbackDraft(
    val purchaseId: String,
    val sellerId: String,
    val rating: Int,
    val comment: String,
    val photoUris: List<String>, // URIs locales como strings
    val timestamp: Long = System.currentTimeMillis()
)
