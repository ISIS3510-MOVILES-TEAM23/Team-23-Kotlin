package com.example.team_23_kotlin.presentation.post

import android.net.Uri

sealed class PostEvent {
    data class TitleChanged(val value: String): PostEvent()
    data class DescriptionChanged(val value: String): PostEvent()
    data class PriceChanged(val value: String): PostEvent()

    data class CategorySelected(val id: String, val name: String): PostEvent()
    object ReloadCategories : PostEvent()

    // Fotos
    object AddPhotosClick: PostEvent()
    data class PhotoAdded(val uri: Uri): PostEvent()
    data class PhotoRemovedAt(val index: Int): PostEvent()

    object SubmitClicked: PostEvent()
}
