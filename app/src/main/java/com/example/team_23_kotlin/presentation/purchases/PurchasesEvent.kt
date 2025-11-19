package com.example.team_23_kotlin.presentation.purchases

import com.example.team_23_kotlin.data.purchases.PurchaseEntity

sealed class PurchasesEvent {
    object OnRefresh : PurchasesEvent()
    data class OnTabChange(val tab: PurchasesTab) : PurchasesEvent()

    data class OnDownloadReceipt(val purchase: PurchaseEntity) : PurchasesEvent()


}
