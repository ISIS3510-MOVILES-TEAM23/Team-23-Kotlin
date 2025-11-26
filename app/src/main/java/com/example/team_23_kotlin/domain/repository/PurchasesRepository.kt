package com.example.team_23_kotlin.domain.repository

import com.example.team_23_kotlin.data.purchases.PurchaseEntity

interface PurchasesRepository {
    suspend fun getPurchases(): List<PurchaseEntity>
    suspend fun savePurchasesToLocal(purchases: List<PurchaseEntity>)
    suspend fun getLocalPurchases(): List<PurchaseEntity>

    suspend fun markReceiptGenerated(purchaseId: String)
    suspend fun isReceiptGenerated(purchaseId: String): Boolean
    suspend fun getAllGeneratedReceipts(): List<String>

    fun invalidateCache()
}
