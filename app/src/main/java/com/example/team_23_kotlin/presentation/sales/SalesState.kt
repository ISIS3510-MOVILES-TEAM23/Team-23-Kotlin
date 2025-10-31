package com.example.team_23_kotlin.presentation.sales

import com.example.team_23_kotlin.data.sales.SaleEntity

/**
 * Estado de la pantalla de ventas
 */
data class SalesState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val allSales: List<SaleEntity> = emptyList(),
    val currentTab: SalesTab = SalesTab.ALL,
    // Estadísticas
    val totalSold: Int = 0,
    val totalShipped: Int = 0,
    val totalPending: Int = 0
) {
    /**
     * Filtra las ventas según la pestaña activa
     */
    val filteredSales: List<SaleEntity>
        get() = when (currentTab) {
            SalesTab.ALL -> allSales
            SalesTab.PENDING -> allSales.filter { it.status == "pending" }
            SalesTab.COMPLETED -> allSales.filter { it.status == "completed" }
        }
}

/**
 * Pestañas disponibles en la pantalla de ventas
 */
enum class SalesTab {
    ALL,
    PENDING,
    COMPLETED
}

