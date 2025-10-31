package com.example.team_23_kotlin.presentation.categories

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.categories.FirestoreCategoriesRepository
import com.example.team_23_kotlin.domain.repository.AnalyticsRepository
import com.google.firebase.firestore.FirebaseFirestore
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class CategoriesViewModel @Inject constructor(
    private val analytics: AnalyticsRepository,
    @ApplicationContext context: Context
) : ViewModel() {

    private val repo = FirestoreCategoriesRepository(context, FirebaseFirestore.getInstance())

    private val _state = MutableStateFlow(CategoriesState())
    val state = _state.asStateFlow()

    init { load() }

    fun load(force: Boolean = false) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val list = repo.getCategories(forceRefresh = force)
                _state.value = _state.value.copy(isLoading = false, categories = list)
            } catch (e: Exception) {
                _state.value = _state.value.copy(isLoading = false, error = e.message)
            }
        }
    }

    fun onEvent(event: CategoriesEvent) {
        when (event) {
            is CategoriesEvent.QueryChanged -> {
                _state.value = _state.value.copy(query = event.value)
            }
            CategoriesEvent.SubmitSearch -> {
                val q = _state.value.query.trim().ifEmpty { null }
                analytics.logProductSearch(query = q, selectedCategory = null, source = "search_bar")
            }
            is CategoriesEvent.CategoryClicked -> {
                analytics.logProductSearch(query = null, selectedCategory = event.category, source = "category_chip")
            }
        }
    }
}
