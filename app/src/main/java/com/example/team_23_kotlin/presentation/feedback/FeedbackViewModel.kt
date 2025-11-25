package com.example.team_23_kotlin.presentation.feedback

import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.data.purchases.FeedbackEntity
import com.example.team_23_kotlin.domain.repository.FeedbackRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val auth: FirebaseAuth,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackState())
    val state: StateFlow<FeedbackState> = _state

    init {
        val purchaseId = savedStateHandle.get<String>("purchaseId") ?: ""
        val sellerId = savedStateHandle.get<String>("sellerId") ?: ""
        _state.value = _state.value.copy(
            purchaseId = purchaseId,
            sellerId = sellerId
        )
        
        // Cargar feedback existente si existe
        loadExistingFeedback(purchaseId)
    }

    private fun loadExistingFeedback(purchaseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.value = _state.value.copy(isLoading = true)
                
                val existingFeedback = feedbackRepository.getFeedbackForPurchase(purchaseId)
                
                withContext(Dispatchers.Main) {
                    if (existingFeedback != null) {
                        // Convertir URLs de Firebase a tokens para mostrar en UI
                        val photoTokens = existingFeedback.photos.map { url -> url }
                        
                        _state.value = _state.value.copy(
                            rating = existingFeedback.rating,
                            comment = existingFeedback.comment,
                            photoTokens = photoTokens,
                            existingFeedbackId = existingFeedback.id,
                            isEditMode = true,
                            isLoading = false
                        )
                        Log.d("FeedbackViewModel", "✅ Loaded existing feedback: ${existingFeedback.id}")
                    } else {
                        _state.value = _state.value.copy(
                            isEditMode = false,
                            isLoading = false
                        )
                        Log.d("FeedbackViewModel", "ℹ️ No existing feedback found")
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar feedback: ${e.message}"
                    )
                }
                Log.e("FeedbackViewModel", "❌ Error loading feedback", e)
            }
        }
    }

    fun onEvent(event: FeedbackEvent) {
        when (event) {
            is FeedbackEvent.RatingChanged -> {
                _state.value = _state.value.copy(rating = event.rating)
            }
            is FeedbackEvent.CommentChanged -> {
                _state.value = _state.value.copy(comment = event.comment)
            }
            is FeedbackEvent.PhotoAdded -> {
                val list = _state.value.photoTokens.toMutableList().apply {
                    add("uri:${event.uri}")
                }
                _state.value = _state.value.copy(photoTokens = list)
            }
            is FeedbackEvent.PhotoRemovedAt -> {
                val list = _state.value.photoTokens.toMutableList().apply {
                    if (event.index in indices) removeAt(event.index)
                }
                _state.value = _state.value.copy(photoTokens = list)
            }
            FeedbackEvent.SubmitClicked -> submitFeedback()
        }
    }

    private fun submitFeedback() {
        val s = _state.value

        // Validación
        if (s.rating == 0) {
            _state.value = s.copy(errorMessage = "Por favor selecciona una calificación")
            return
        }

        if (s.comment.isBlank()) {
            _state.value = s.copy(errorMessage = "Por favor escribe un comentario")
            return
        }

        viewModelScope.launch(Dispatchers.Main) {
            try {
                _state.value = s.copy(isSaving = true, errorMessage = null, uploadProgress = 0f)

                val userId = auth.currentUser?.uid ?: run {
                    _state.value = s.copy(
                        isSaving = false,
                        errorMessage = "Usuario no autenticado"
                    )
                    return@launch
                }

                // Separar URIs nuevas de URLs existentes
                val newUris: List<Uri> = s.photoTokens
                    .filter { it.startsWith("uri:") }
                    .map { Uri.parse(it.removePrefix("uri:")) }
                
                val existingPhotoUrls: List<String> = s.photoTokens
                    .filter { !it.startsWith("uri:") }

                // Crear entidad de feedback
                val feedback = FeedbackEntity(
                    id = s.existingFeedbackId ?: "",
                    purchaseId = s.purchaseId,
                    buyerId = userId,
                    sellerId = s.sellerId,
                    rating = s.rating,
                    comment = s.comment,
                    photos = existingPhotoUrls // URLs existentes
                )

                // Simular progreso de subida
                _state.value = _state.value.copy(uploadProgress = 0.5f)

                // Enviar o actualizar feedback
                val success = withContext(Dispatchers.IO) {
                    if (s.isEditMode && s.existingFeedbackId != null) {
                        feedbackRepository.updateFeedback(s.existingFeedbackId, feedback, newUris)
                    } else {
                        feedbackRepository.submitFeedback(feedback, newUris)
                    }
                }

                if (success) {
                    val message = if (s.isEditMode) "✅ Feedback actualizado correctamente" else "✅ Feedback enviado correctamente"
                    _state.value = s.copy(
                        isSaving = false,
                        uploadProgress = 1f,
                        submitSuccess = true,
                        errorMessage = message
                    )
                    Log.d("FeedbackViewModel", "✅ Feedback ${if (s.isEditMode) "updated" else "submitted"} successfully")
                } else {
                    _state.value = s.copy(
                        isSaving = false,
                        errorMessage = "❌ Error al ${if (s.isEditMode) "actualizar" else "enviar"} el feedback"
                    )
                }

            } catch (e: Exception) {
                _state.value = s.copy(
                    isSaving = false,
                    errorMessage = "❌ Error: ${e.message}"
                )
                Log.e("FeedbackViewModel", "❌ Error submitting feedback", e)
            }
        }
    }
}
