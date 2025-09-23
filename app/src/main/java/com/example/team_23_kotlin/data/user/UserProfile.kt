package com.example.team_23_kotlin.data.user

import com.google.firebase.Timestamp
import com.google.firebase.firestore.ServerTimestamp

data class UserProfile(
    @ServerTimestamp val created_at: Timestamp? = null,
    val email: String = "",
    val is_verified: Boolean = false,
    val name: String = "",
)