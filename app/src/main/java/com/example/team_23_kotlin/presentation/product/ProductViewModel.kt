package com.example.team_23_kotlin.presentation.product

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.posts.PostsRepository
import com.example.team_23_kotlin.domain.repository.AnalyticsRepository
import kotlinx.coroutines.launch

class ProductViewModel(
    private val repo: PostsRepository,
    private val analytics: AnalyticsRepository
) : ViewModel() {

    private val _state = kotlinx.coroutines.flow.MutableStateFlow(ProductState())
    val state: kotlinx.coroutines.flow.StateFlow<ProductState> = _state

    fun onEvent(event: ProductEvent) {
        when (event) {
            is ProductEvent.LoadProduct -> load(event.productId)
        }
    }

    private fun load(productId: String) {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            try {
                val entity = repo.getPostById(productId)

                val ui = ProductUiModel(
                    id = entity.id,
                    title = entity.title,
                    description = entity.description,
                    price = "$${entity.price}",               // formateo simple
                    imageUrl = entity.images.firstOrNull().orEmpty(),
                    sellerName = entity.userRef.substringAfterLast("/").ifBlank { "Seller" },
                    sellerRating = 4.5f                       // placeholder (aún no hay rating)
                )

                println("📦 CategoryName being logged: ${entity.categoryName}")

                analytics.logProductClick(
                    postId = entity.id,
                    category = entity.categoryName,
                    source = "product_screen"
                )

                _state.value = ProductState(product = ui, isLoading = false)
            } catch (e: Exception) {
                _state.value = _state.value.copy(
                    isLoading = false,
                    error = e.message ?: "Error loading product"
                )
            }
        }
    }
}
