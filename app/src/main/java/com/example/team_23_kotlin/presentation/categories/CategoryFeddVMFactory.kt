// presentation/categories/CategoryFeedVMFactory.kt
package com.example.team_23_kotlin.presentation.categories

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.google.firebase.firestore.FirebaseFirestore

class CategoryFeedVMFactory(
    private val db: FirebaseFirestore,
    private val categoryId: String
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        @Suppress("UNCHECKED_CAST")
        return CategoryFeedViewModel(db, categoryId) as T
    }
}
