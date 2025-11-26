package com.example.team_23_kotlin.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FeedbackDraftStorage @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val prefs: SharedPreferences = 
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    
    private val gson = Gson()

    companion object {
        private const val TAG = "FeedbackDraftStorage"
        private const val PREFS_NAME = "feedback_drafts"
        private const val KEY_PREFIX = "draft_"
    }

    /**
     * Guarda un borrador de feedback localmente
     */
    fun saveDraft(draft: FeedbackDraft) {
        try {
            val key = KEY_PREFIX + draft.purchaseId
            val jsonString = gson.toJson(draft)
            
            prefs.edit()
                .putString(key, jsonString)
                .apply()
            
            Log.d(TAG, "✅ Draft saved for purchase ${draft.purchaseId}")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error saving draft", e)
        }
    }

    /**
     * Carga un borrador de feedback desde local storage
     */
    fun loadDraft(purchaseId: String): FeedbackDraft? {
        return try {
            val key = KEY_PREFIX + purchaseId
            val jsonString = prefs.getString(key, null) ?: return null
            
            val draft = gson.fromJson(jsonString, FeedbackDraft::class.java)
            Log.d(TAG, "✅ Draft loaded for purchase $purchaseId")
            draft
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error loading draft for $purchaseId", e)
            null
        }
    }

    /**
     * Elimina un borrador específico
     */
    fun clearDraft(purchaseId: String) {
        try {
            val key = KEY_PREFIX + purchaseId
            prefs.edit()
                .remove(key)
                .apply()
            
            Log.d(TAG, "✅ Draft cleared for purchase $purchaseId")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing draft", e)
        }
    }

    /**
     * Obtiene todos los borradores guardados
     */
    fun getAllDrafts(): List<FeedbackDraft> {
        return try {
            prefs.all
                .filter { it.key.startsWith(KEY_PREFIX) }
                .mapNotNull { entry ->
                    try {
                        gson.fromJson(entry.value as String, FeedbackDraft::class.java)
                    } catch (e: Exception) {
                        Log.w(TAG, "⚠️ Skipping corrupted draft: ${entry.key}")
                        null
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error getting all drafts", e)
            emptyList()
        }
    }

    /**
     * Elimina todos los borradores
     */
    fun clearAllDrafts() {
        try {
            val editor = prefs.edit()
            prefs.all.keys
                .filter { it.startsWith(KEY_PREFIX) }
                .forEach { editor.remove(it) }
            editor.apply()
            
            Log.d(TAG, "✅ All drafts cleared")
        } catch (e: Exception) {
            Log.e(TAG, "❌ Error clearing all drafts", e)
        }
    }

    /**
     * Verifica si existe un borrador para una compra
     */
    fun hasDraft(purchaseId: String): Boolean {
        val key = KEY_PREFIX + purchaseId
        return prefs.contains(key)
    }
}
