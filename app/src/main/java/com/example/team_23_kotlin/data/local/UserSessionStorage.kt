package com.example.team_23_kotlin.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class StoredUser(
    val uid: String,
    val email: String?,
    val displayName: String?,
    val photoUrl: String?,
    val isEmailVerified: Boolean
)

@Singleton
class UserSessionStorage @Inject constructor(
    @ApplicationContext context: Context
) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun save(user: StoredUser) {
        prefs.edit()
            .putString(KEY_UID, user.uid)
            .putString(KEY_EMAIL, user.email)
            .putString(KEY_DISPLAY_NAME, user.displayName)
            .putString(KEY_PHOTO_URL, user.photoUrl)
            .putBoolean(KEY_IS_EMAIL_VERIFIED, user.isEmailVerified)
            .apply()
        Log.d(TAG, "Sesión guardada para uid=${user.uid}, email=${user.email}")
    }

    fun get(): StoredUser? {
        val uid = prefs.getString(KEY_UID, null) ?: return null
        Log.d(TAG, "Recuperando sesión almacenada para uid=$uid")
        return StoredUser(
            uid = uid,
            email = prefs.getString(KEY_EMAIL, null),
            displayName = prefs.getString(KEY_DISPLAY_NAME, null),
            photoUrl = prefs.getString(KEY_PHOTO_URL, null),
            isEmailVerified = prefs.getBoolean(KEY_IS_EMAIL_VERIFIED, false)
        )
    }

    fun clear() {
        prefs.edit().clear().apply()
        Log.d(TAG, "Sesión local eliminada")
    }

    private companion object {
        const val TAG = "UserSessionStorage"
        const val PREFS_NAME = "user_session_prefs"
        const val KEY_UID = "uid"
        const val KEY_EMAIL = "email"
        const val KEY_DISPLAY_NAME = "display_name"
        const val KEY_PHOTO_URL = "photo_url"
        const val KEY_IS_EMAIL_VERIFIED = "is_email_verified"
    }
}

