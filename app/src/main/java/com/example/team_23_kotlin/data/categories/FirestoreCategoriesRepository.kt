package com.example.team_23_kotlin.data.categories

import android.content.Context
import androidx.collection.LruCache
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import com.google.gson.Gson

// DataStore (un único archivo de prefs para cachear la lista)
private val Context.categoriesDataStore by preferencesDataStore("categories_cache")

data class CategoryEntity(
    val id: String = "",
    val name: String = "",
    val icon: String = "" // nombre del drawable, p. ej. "ic_books"
)

class FirestoreCategoriesRepository(
    private val context: Context,
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val gson: Gson = Gson()
) {
    // LRU muy simple: cachea la lista en memoria durante la sesión
    private val memory = LruCache<String, List<CategoryEntity>>(1)
    private val KEY_JSON = stringPreferencesKey("categories_json")

    /** Carga categorías con ECn propia:
     *  1) Memoria (LRU)
     *  2) Disco (DataStore JSON)
     *  3) Red (Firestore) y luego guarda en memoria+disco
     */
    suspend fun getCategories(forceRefresh: Boolean = false): List<CategoryEntity> {
        if (!forceRefresh) {
            memory.get("list")?.let { return it }

            val prefs = context.categoriesDataStore.data.first()
            val json = prefs[KEY_JSON]
            if (!json.isNullOrBlank()) {
                val list = gson.fromJson(json, Array<CategoryEntity>::class.java)?.toList().orEmpty()
                if (list.isNotEmpty()) {
                    memory.put("list", list)
                    return list
                }
            }
        }

        // Red
        val remote = fetchRemote()
        if (remote.isNotEmpty()) {
            memory.put("list", remote)
            val json = gson.toJson(remote)
            context.categoriesDataStore.edit { it[KEY_JSON] = json }
        }
        return remote
    }

    private suspend fun fetchRemote(): List<CategoryEntity> = try {
        val snap = db.collection("categories").get().await()
        snap.documents.map { d ->
            CategoryEntity(
                id = d.id,
                name = d.getString("name") ?: "",
                icon = d.getString("icon") ?: "ic_books" // drawable por defecto
            )
        }
    } catch (_: Exception) {
        emptyList()
    }
}
