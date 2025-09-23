package com.example.team_23_kotlin.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.domain.repository.AnalyticsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val analytics: AnalyticsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CategoriesState())
    val state = _state.asStateFlow()

    fun onEvent(event: CategoriesEvent) {
        when (event) {
            is CategoriesEvent.QueryChanged -> {
                _state.value = _state.value.copy(query = event.value)
            }
            CategoriesEvent.SubmitSearch -> {
                val q = _state.value.query.trim().ifEmpty { null }
                // Log del evento de búsqueda desde la barra
                analytics.logProductSearch(
                    query = q,
                    selectedCategory = null,     // si además aplicas filtro, pásalo aquí
                    source = "search_bar"
                )
                // aquí disparas tu búsqueda real si aplica (use case / repo)
            }
            is CategoriesEvent.CategoryClicked -> {
                // Log del tap en categoría
                analytics.logProductSearch(
                    query = null,
                    selectedCategory = event.category,
                    source = "category_chip"
                )
                // navegar / filtrar productos por categoría
            }
        }
    }
}
