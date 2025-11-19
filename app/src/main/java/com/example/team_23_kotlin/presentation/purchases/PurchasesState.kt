package com.example.team_23_kotlin.presentation.purchases

import com.example.team_23_kotlin.data.purchases.PurchaseEntity

data class PurchasesState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val purchases: List<PurchaseEntity> = emptyList(),
    val filteredPurchases: List<PurchaseEntity> = emptyList(),
    val currentTab: PurchasesTab = PurchasesTab.ALL,

    val totalPurchased: Int = 0,
    val totalCompleted: Int = 0,
    val totalPending: Int = 0
)

enum class PurchasesTab { ALL, PENDING, COMPLETED }
