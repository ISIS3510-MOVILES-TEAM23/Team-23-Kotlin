package com.example.team_23_kotlin.presentation.categories

sealed class CategoriesEvent {
    data class QueryChanged(val value: String) : CategoriesEvent()
    object SubmitSearch : CategoriesEvent()
    data class CategoryClicked(val category: String) : CategoriesEvent()
}
