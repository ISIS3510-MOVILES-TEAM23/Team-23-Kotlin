package com.example.team_23_kotlin.data.local.room

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "receipts")
data class ReceiptEntity(
    @PrimaryKey val purchaseId: String,
    val timestamp: Long
)
