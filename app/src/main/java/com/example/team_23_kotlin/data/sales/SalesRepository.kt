package com.example.team_23_kotlin.data.sales

import com.google.firebase.Timestamp
import java.util.Date

/**
 * Interface para el repositorio de ventas
 */
interface SalesRepository {
    /**
     * Obtiene todas las ventas donde el usuario es el vendedor
     * @param userId ID del usuario vendedor
     * @param limit Límite de resultados
     * @return Lista de ventas
     */
    suspend fun getSalesBySeller(userId: String, limit: Int = 50): List<SaleEntity>
    
    /**
     * Obtiene una venta por su ID
     * @param saleId ID de la venta
     * @return Entidad de venta
     */
    suspend fun getSaleById(saleId: String): SaleEntity?
}

/**
 * Entidad que representa una venta
 */
data class SaleEntity(
    val id: String = "",
    val buyerId: String = "",
    val sellerId: String = "",
    val postId: String = "",
    val price: Long = 0,
    val status: String = "", // "pending", "shipped", "completed"
    val createdAt: Date? = null,
    val updatedAt: Date? = null,
    // Información adicional del post
    val postTitle: String = "",
    val postImages: List<String> = emptyList(),
    // Información adicional del comprador
    val buyerName: String = "",
    val buyerEmail: String = ""
)

