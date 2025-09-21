package com.example.team_23_kotlin.presentation.seller

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class SellerViewModel(sellerId: String) : ViewModel() {

    private val _state = MutableStateFlow(SellerState())
    val state: StateFlow<SellerState> = _state

    init {
        onEvent(SellerEvent.LoadSeller(sellerId))
    }

    fun onEvent(event: SellerEvent) {
        when (event) {
            is SellerEvent.LoadSeller -> loadSeller(event.sellerId)
        }
    }

    private fun loadSeller(sellerId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true)
            try {
                delay(1000) // Simulación de carga

                val seller = SellerUiModel(
                    id = sellerId,
                    name = "Camila Torres",
                    role = "Estudiante de Ingeniería Industrial",
                    rating = 4.7f,
                    profileImageUrl = "https://randomuser.me/api/portraits/women/5.jpg",
                    isInCampus = true,
                    products = listOf(
                        ProductItem("1", "Calculadora Científica", "$80.000", "https://picsum.photos/200?1"),
                        ProductItem("2", "Libro de Física", "$50.000", "https://picsum.photos/200?2"),
                        ProductItem("3", "Sudadera Uniandes", "$60.000", "https://picsum.photos/200?3")
                    )
                )


                _state.value = SellerState(seller = seller, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(error = "No se pudo cargar el perfil", isLoading = false)
            }
        }
    }
}
