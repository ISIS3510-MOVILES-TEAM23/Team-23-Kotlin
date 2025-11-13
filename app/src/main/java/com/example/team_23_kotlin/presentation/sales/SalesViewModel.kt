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

    private val _state = MutableStateFlow(SalesState())
    val state: StateFlow<SalesState> = _state.asStateFlow()

    init {
        loadSales()
    }

    fun onEvent(event: SalesEvent) {
        when (event) {
            is SalesEvent.OnTabChange -> {
                _state.update { it.copy(currentTab = event.tab) }
            }
            is SalesEvent.OnMarkAsShipped -> {
                markAsCompleted(event.saleId)
            }
            is SalesEvent.OnRefresh -> loadSales()
            is SalesEvent.OnSaleClick -> Log.d("SalesVM", "Venta clic: ${event.saleId}")
        }
    }

    private fun loadSales() {
        viewModelScope.launch {
            try {
                _state.update { it.copy(isLoading = true, error = null) }

                val userId = auth.currentUser?.uid
                if (userId == null) {
                    _state.update { it.copy(isLoading = false, error = "Usuario no autenticado") }
                    return@launch
                }

                val sales = salesRepository.getSalesBySeller(userId)

                val totalSold = sales.size
                val totalCompleted = sales.count { it.status == "completed" }
                val totalPending = sales.count { it.status == "pending" }

                _state.update {
                    it.copy(
                        isLoading = false,
                        allSales = sales,
                        totalSold = totalSold,
                        totalCompleted = totalCompleted,
                        totalPending = totalPending,
                        error = null
                    )
                }

            } catch (e: Exception) {
                _state.update {
                    it.copy(isLoading = false, error = "Error: ${e.message}")
                }
            }
        }
    }

    private fun markAsCompleted(saleId: String) {
        viewModelScope.launch {
            try {
                firestore.collection("sales")
                    .document(saleId)
                    .update(
                        mapOf(
                            "status" to "completed",
                            "updated_at" to com.google.firebase.Timestamp.now()
                        )
                    )
                    .await()

                _state.update { current ->
                    val updatedSales = current.allSales.map { sale ->
                        if (sale.id == saleId) sale.copy(status = "completed")
                        else sale
                    }

                    current.copy(
                        allSales = updatedSales,
                        totalCompleted = updatedSales.count { it.status == "completed" },
                        totalPending = updatedSales.count { it.status == "pending" }
                    )
                }

            } catch (e: Exception) {
                _state.update { it.copy(error = "Error al actualizar: ${e.message}") }
            }
        }
    }
}
