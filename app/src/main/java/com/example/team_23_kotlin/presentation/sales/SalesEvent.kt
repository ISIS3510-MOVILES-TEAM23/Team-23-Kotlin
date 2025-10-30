package com.example.team_23_kotlin.presentation.sales

/**
 * Eventos que puede disparar el usuario en la pantalla de ventas
 */
sealed class SalesEvent {
    /**
     * Cambiar la pestaña activa
     */
    data class OnTabChange(val tab: SalesTab) : SalesEvent()
    
    /**
     * Marcar una venta como enviada
     */
    data class OnMarkAsShipped(val saleId: String) : SalesEvent()
    
    /**
     * Refrescar la lista de ventas
     */
    object OnRefresh : SalesEvent()
    
    /**
     * Click en una venta para ver detalles
     */
    data class OnSaleClick(val saleId: String) : SalesEvent()
}

