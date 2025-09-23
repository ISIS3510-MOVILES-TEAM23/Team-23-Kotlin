package com.example.team_23_kotlin.data.posts

interface PostsRepository {
    suspend fun getActivePosts(limit: Int = 20): List<PostEntity>
    suspend fun getPostById(id: String): PostEntity
}

data class PostEntity(
    val id: String,
    val title: String,
    val description: String,
    val price: Long,
    val images: List<String>,
    val userRef: String,
    val status: String
)
