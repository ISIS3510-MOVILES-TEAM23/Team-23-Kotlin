package com.example.team_23_kotlin.presentation.sales

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.sales.SalesRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

/**
 * ViewModel para la pantalla de ventas
 */
@HiltViewModel
class SalesViewModel @Inject constructor(
    private val salesRepository: SalesRepository,
    private val auth: FirebaseAuth,
    private val firestore: FirebaseFirestore
) : ViewModel() {

    companion object {
        private const val TAG = "SalesViewModel"
    }

    private val _state = MutableStateFlow(SalesState())
    val state: StateFlow<SalesState> = _state.asStateFlow()

    init {
        loadSales()
    }

    /**
     * Maneja los eventos de la UI
     */
    fun onEvent(event: SalesEvent) {
        when (event) {
            is SalesEvent.OnTabChange -> {
                _state.update { it.copy(currentTab = event.tab) }
            }
            is SalesEvent.OnMarkAsShipped -> {
                markAsShipped(event.saleId)
            }
            is SalesEvent.OnRefresh -> {
                loadSales()
            }
            is SalesEvent.OnSaleClick -> {
                // Aquí se puede navegar a la pantalla de detalles si se necesita
                Log.d(TAG, "Click en venta: ${event.saleId}")
            }
        }
    }

    /**
     * Carga las ventas del usuario logueado
     */
    private fun loadSales() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _state.update { 
                        it.copy(
                            isLoading = false, 
                            error = "Usuario no autenticado"
                        ) 
                    }
                    return@launch
                }

                val sales = salesRepository.getSalesBySeller(userId)
                
                // Calcular estadísticas
                val totalSold = sales.size
                val totalShipped = sales.count { it.status == "shipped" }
                val totalPending = sales.count { it.status == "pending" }

                _state.update {
                    it.copy(
                        isLoading = false,
                        allSales = sales,
                        totalSold = totalSold,
                        totalShipped = totalShipped,
                        totalPending = totalPending,
                        error = null
                    )
                }

                Log.d(TAG, "Ventas cargadas exitosamente: $totalSold")
            } catch (e: Exception) {
                Log.e(TAG, "Error al cargar ventas", e)
                _state.update {
                    it.copy(
                        isLoading = false,
                        error = "Error al cargar las ventas: ${e.message}"
                    )
                }
            }
        }
    }

    /**
     * Marca una venta como enviada
     */
    private fun markAsShipped(saleId: String) {
        viewModelScope.launch {
            try {
                // Actualizar el estado en Firestore
                firestore.collection("sales")
                    .document(saleId)
                    .update(
                        mapOf(
                            "status" to "shipped",
                            "updated_at" to com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()

                // Actualizar el estado local
                _state.update { currentState ->
                    val updatedSales = currentState.allSales.map { sale ->
                        if (sale.id == saleId) {
                            sale.copy(status = "shipped")
                        } else {
                            sale
                        }
                    }

                    // Recalcular estadísticas
                    val totalShipped = updatedSales.count { it.status == "shipped" }
                    val totalPending = updatedSales.count { it.status == "pending" }

                    currentState.copy(
                        allSales = updatedSales,
                        totalShipped = totalShipped,
                        totalPending = totalPending
                    )
                }

                Log.d(TAG, "Venta marcada como enviada: $saleId")
            } catch (e: Exception) {
                Log.e(TAG, "Error al marcar como enviada", e)
                _state.update {
                    it.copy(error = "Error al actualizar la venta: ${e.message}")
                }
            }
        }
    }
}

