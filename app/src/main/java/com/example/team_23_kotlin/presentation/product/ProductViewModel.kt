package com.example.team_23_kotlin.presentation.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class ProductViewModel(productId: String) : ViewModel() {

    private val _state = MutableStateFlow(ProductState())
    val state: StateFlow<ProductState> = _state

    init {
        onEvent(ProductEvent.LoadProduct(productId))
    }

    fun onEvent(event: ProductEvent) {
        when (event) {
            is ProductEvent.LoadProduct -> loadProduct(event.productId)
        }
    }

    private fun loadProduct(productId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                // Simulación de carga de datos
                delay(1000)

                val mock = ProductUiModel(
                    id = productId,
                    title = "Audífonos Sony WH-1000XM5",
                    description = "Cancelación activa de ruido, batería de 30 horas, y sonido Hi-Res.",
                    price = "$1.200.000",
                    imageUrl = "https://picsum.photos/400",
                    sellerName = "Laura Torres",
                    sellerRating = 4.6f
                )

                _state.value = ProductState(product = mock, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "Error al cargar el producto", isLoading = false)
            }
        }
    }
}
