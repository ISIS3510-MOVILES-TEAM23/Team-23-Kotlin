package com.example.team_23_kotlin.presentation.feedback

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.team_23_kotlin.core.network.hasInternetConnection
import com.example.team_23_kotlin.data.local.FeedbackDraft
import com.example.team_23_kotlin.data.local.FeedbackDraftStorage
import com.example.team_23_kotlin.data.purchases.FeedbackEntity
import com.example.team_23_kotlin.domain.repository.FeedbackRepository
import com.example.team_23_kotlin.domain.repository.PurchasesRepository
import com.google.firebase.auth.FirebaseAuth
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import javax.inject.Inject

@OptIn(FlowPreview::class)
@HiltViewModel
class FeedbackViewModel @Inject constructor(
    private val feedbackRepository: FeedbackRepository,
    private val purchasesRepository: PurchasesRepository,
    private val auth: FirebaseAuth,
    private val draftStorage: FeedbackDraftStorage,
    @ApplicationContext private val context: Context,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _state = MutableStateFlow(FeedbackState())
    val state: StateFlow<FeedbackState> = _state
    
    // Trigger para auto-save
    private val autoSaveTrigger = MutableStateFlow(0L)

    companion object {
        private const val TAG = "FeedbackViewModel"
        private const val AUTO_SAVE_DEBOUNCE_MS = 2000L
    }

    init {
        val purchaseId = savedStateHandle.get<String>("purchaseId") ?: ""
        val sellerId = savedStateHandle.get<String>("sellerId") ?: ""
        _state.value = _state.value.copy(
            purchaseId = purchaseId,
            sellerId = sellerId,
            isOnline = context.hasInternetConnection()
        )
        
        // Setup auto-save con debounce
        setupAutoSave()
        
        // Cargar feedback existente o borrador
        loadExistingFeedbackOrDraft(purchaseId)
        
        // Intentar sincronizar borrador si hay conexión
        syncDraftIfNeeded()
    }

    private fun setupAutoSave() {
        viewModelScope.launch {
            autoSaveTrigger
                .debounce(AUTO_SAVE_DEBOUNCE_MS)
                .collect {
                    if (it > 0) { // Ignorar valor inicial
                        saveDraftLocally()
                    }
                }
        }
    }

    private fun loadExistingFeedbackOrDraft(purchaseId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                _state.value = _state.value.copy(isLoading = true)
                
                // 1. Intentar cargar feedback existente
                val existingFeedback = feedbackRepository.getFeedbackForPurchase(purchaseId)
                
                if (existingFeedback != null) {
                    // Feedback existe - modo edición
                    val photoTokens = existingFeedback.photos.map { url -> url }
                    
                    withContext(Dispatchers.Main) {
                        _state.value = _state.value.copy(
                            rating = existingFeedback.rating,
                            comment = existingFeedback.comment,
                            photoTokens = photoTokens,
                            existingFeedbackId = existingFeedback.id,
                            isEditMode = true,
                            isLoading = false
                        )
                        Log.d(TAG, "✅ Loaded existing feedback: ${existingFeedback.id}")
                    }
                } else {
                    // 2. No hay feedback - intentar cargar borrador
                    val draft = draftStorage.loadDraft(purchaseId)
                    
                    withContext(Dispatchers.Main) {
                        if (draft != null) {
                            _state.value = _state.value.copy(
                                rating = draft.rating,
                                comment = draft.comment,
                                photoTokens = draft.photoUris.map { "uri:$it" },
                                hasDraft = true,
                                draftTimestamp = draft.timestamp,
                                isEditMode = false,
                                isLoading = false
                            )
                            Log.d(TAG, "✅ Loaded draft for purchase $purchaseId")
                        } else {
                            _state.value = _state.value.copy(
                                isEditMode = false,
                                isLoading = false
                            )
                            Log.d(TAG, "ℹ️ No existing feedback or draft found")
                        }
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(
                        isLoading = false,
                        errorMessage = "Error al cargar feedback: ${e.message}"
                    )
                }
                Log.e(TAG, "❌ Error loading feedback", e)
            }
        }
    }

    fun onEvent(event: FeedbackEvent) {
        when (event) {
            is FeedbackEvent.RatingChanged -> {
                _state.value = _state.value.copy(rating = event.rating)
                triggerAutoSave()
            }
            is FeedbackEvent.CommentChanged -> {
                _state.value = _state.value.copy(comment = event.comment)
                triggerAutoSave()
            }
            is FeedbackEvent.PhotoAdded -> {
                val list = _state.value.photoTokens.toMutableList().apply {
                    add("uri:${event.uri}")
                }
                _state.value = _state.value.copy(photoTokens = list)
                triggerAutoSave()
            }
            is FeedbackEvent.PhotoRemovedAt -> {
                val list = _state.value.photoTokens.toMutableList().apply {
                    if (event.index in indices) removeAt(event.index)
                }
                _state.value = _state.value.copy(photoTokens = list)
                triggerAutoSave()
            }
            FeedbackEvent.SubmitClicked -> submitFeedback()
        }
    }

    private fun triggerAutoSave() {
        autoSaveTrigger.value = System.currentTimeMillis()
    }

    private fun saveDraftLocally() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val s = _state.value
                
                // No guardar si ya existe feedback (modo edición)
                if (s.isEditMode) return@launch
                
                // No guardar si está vacío
                if (s.rating == 0 && s.comment.isBlank() && s.photoTokens.isEmpty()) {
                    return@launch
                }
                
                val draft = FeedbackDraft(
                    purchaseId = s.purchaseId,
                    sellerId = s.sellerId,
                    rating = s.rating,
                    comment = s.comment,
                    photoUris = s.photoTokens
                        .filter { it.startsWith("uri:") }
                        .map { it.removePrefix("uri:") }
                )
                
                draftStorage.saveDraft(draft)
                
                withContext(Dispatchers.Main) {
                    _state.value = s.copy(
                        hasDraft = true,
                        draftTimestamp = draft.timestamp
                    )
                }
                
                Log.d(TAG, "✅ Draft saved locally")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error saving draft", e)
            }
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
                // Verificar conectividad
                val isOnline = context.hasInternetConnection()
                _state.value = s.copy(isOnline = isOnline)
                
                if (!isOnline) {
                    saveDraftLocally()
                    _state.value = s.copy(
                        errorMessage = "📦 Sin conexión — feedback guardado localmente. Se sincronizará automáticamente."
                    )
                    return@launch
                }
                
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
                    photos = existingPhotoUrls // El repositorio agregará las nuevas
                )

                // Enviar o actualizar feedback
                val success = withContext(Dispatchers.IO) {
                    if (s.isEditMode && s.existingFeedbackId != null) {
                        feedbackRepository.updateFeedback(s.existingFeedbackId, feedback, newUris)
                    } else {
                        feedbackRepository.submitFeedback(feedback, newUris)
                    }
                }

                if (success) {
                    // Limpiar borrador
                    draftStorage.clearDraft(s.purchaseId)
                    
                    // Invalidar cache de purchases para refrescar lista
                    withContext(Dispatchers.IO) {
                        try {
                            purchasesRepository.invalidateCache()
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not invalidate purchases cache", e)
                        }
                    }
                    
                    val message = if (s.isEditMode) "✅ Feedback actualizado correctamente" else "✅ Feedback enviado correctamente"
                    _state.value = s.copy(
                        isSaving = false,
                        uploadProgress = 1f,
                        submitSuccess = true,
                        hasDraft = false,
                        errorMessage = message
                    )
                    Log.d(TAG, "✅ Feedback ${if (s.isEditMode) "updated" else "submitted"} successfully")
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
                Log.e(TAG, "❌ Error submitting feedback", e)
            }
        }
    }

    private fun syncDraftIfNeeded() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                if (!context.hasInternetConnection()) return@launch
                
                val draft = draftStorage.loadDraft(_state.value.purchaseId) ?: return@launch
                
                // Solo sincronizar si no existe feedback ya
                val existingFeedback = feedbackRepository.getFeedbackForPurchase(draft.purchaseId)
                if (existingFeedback != null) {
                    // Ya existe feedback, eliminar borrador
                    draftStorage.clearDraft(draft.purchaseId)
                    return@launch
                }
                
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(isSyncing = true)
                }
                
                // Intentar sincronizar
                Log.d(TAG, "🔄 Attempting to sync draft for purchase ${draft.purchaseId}")
                
                // TODO: Implementar sincronización automática
                // Por ahora solo notificamos al usuario que hay un borrador
                
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(isSyncing = false)
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "❌ Error syncing draft", e)
                withContext(Dispatchers.Main) {
                    _state.value = _state.value.copy(isSyncing = false)
                }
            }
        }
    }
}
