package com.example.team_23_kotlin.data.local

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extensión para acceder al DataStore
val Context.userDataStore by preferencesDataStore("user_prefs")

class UserPreferences(private val context: Context) {

    companion object {
        val NAME = stringPreferencesKey("name")
        val PHONE = stringPreferencesKey("phone")
        val CONTACT_PREFS = stringSetPreferencesKey("contact_prefs")
    }

    suspend fun saveProfile(name: String, phone: String, prefs: List<String>) {
        context.userDataStore.edit { settings ->
            settings[NAME] = name
            settings[PHONE] = phone
            settings[CONTACT_PREFS] = prefs.toSet()
        }
    }

    val userProfile: Flow<UserProfile> = context.userDataStore.data.map { prefs ->
        UserProfile(
            name = prefs[NAME] ?: "",
            phone = prefs[PHONE] ?: "",
            contactPrefs = prefs[CONTACT_PREFS]?.toList() ?: emptyList()
        )
    }
}

data class UserProfile(
    val name: String,
    val phone: String,
    val contactPrefs: List<String>
)
