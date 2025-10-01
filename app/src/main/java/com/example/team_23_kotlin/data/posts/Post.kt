    // data/posts/Post.kt
    package com.example.team_23_kotlin.data.posts

    import com.google.firebase.Timestamp
    import com.google.firebase.firestore.DocumentReference

    data class Post(
        val id: String = "",
        val title: String = "",
        val description: String = "",
        val price: Long = 0,
        val images: List<String> = emptyList(),
        val status: String = "",
        val createdAt: Timestamp? = null,
        val category: DocumentReference? = null,
        val userRef: DocumentReference? = null
    )

