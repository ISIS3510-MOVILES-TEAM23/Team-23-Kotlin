package com.example.team_23_kotlin.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first
import org.json.JSONArray

// Local persistence for wishlist post IDs using DataStore Preferences.
// We keep a JSON array of strings to remain compatible across devices.
private val Context.wishlistDataStore by preferencesDataStore("wishlist_store")

class WishlistStorage(private val appContext: Context) {
	private val keyJson = stringPreferencesKey("ids_json")

	val idsFlow: Flow<Set<String>> = appContext.wishlistDataStore.data.map { prefs ->
		val raw = prefs[keyJson].orEmpty()
		if (raw.isBlank()) emptySet()
		else runCatching {
			val arr = JSONArray(raw)
			buildSet(arr.length()) { for (i in 0 until arr.length()) add(arr.optString(i)) }
		}.getOrElse { emptySet() }
	}

	suspend fun getIds(): Set<String> {
		return idsFlow.first()
	}

	suspend fun add(id: String) {
		update { set -> set + id }
	}

	suspend fun remove(id: String) {
		update { set -> set - id }
	}

	suspend fun toggle(id: String): Boolean {
		var added = false
		update { set ->
			added = !set.contains(id)
			if (added) set + id else set - id
		}
		return added
	}

	private suspend fun update(transform: (Set<String>) -> Set<String>) {
		appContext.wishlistDataStore.edit { prefs ->
			val current = prefs[keyJson].orEmpty()
			val currentSet = if (current.isBlank()) emptySet() else try {
				val arr = JSONArray(current)
				buildSet(arr.length()) { for (i in 0 until arr.length()) add(arr.optString(i)) }
			} catch (_: Exception) {
				emptySet()
			}
			val updated = transform(currentSet)
			val arr = JSONArray().apply { updated.forEach { put(it) } }
			prefs[keyJson] = arr.toString()
		}
	}
}


