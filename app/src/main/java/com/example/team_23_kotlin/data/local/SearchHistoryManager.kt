package com.example.team_23_kotlin.data.local

import android.content.Context
import android.content.SharedPreferences

class SearchHistoryManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("search_history_prefs", Context.MODE_PRIVATE)

    private val KEY_HISTORY = "recent_searches"

    fun saveQuery(query: String) {
        if (query.isBlank()) return
        val existing = getHistory().toMutableList()
        existing.remove(query) // evita duplicados
        existing.add(0, query) // agrega al inicio
        val limited = existing.take(5) // guarda solo las últimas 5
        prefs.edit().putStringSet(KEY_HISTORY, limited.toSet()).apply()
    }

    fun getHistory(): List<String> {
        return prefs.getStringSet(KEY_HISTORY, emptySet())?.toList() ?: emptyList()
    }

    fun clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply()
    }
}
