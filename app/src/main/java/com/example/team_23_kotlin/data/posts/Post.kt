    // data/posts/Post.kt
    package com.example.team_23_kotlin.data.posts

    import com.google.firebase.Timestamp
    import com.google.firebase.firestore.DocumentReference

    data class Post(
        val title: String = "",
        val description: String = "",
        val price: Long = 0,
        val category: DocumentReference? = null,
        val category_name: String = "", // ✅ nombre de la categoría
        val pickup_point_name: String = "", // ✅ nombre del punto
        val pickup_coordinates: String = "", // ✅ coordenadas
        val images: List<String> = emptyList(),
        val user_id: String = "",
        val created_at: String = "",
        val status: String = "active"
    )

