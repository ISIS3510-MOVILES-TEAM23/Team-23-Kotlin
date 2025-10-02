package com.example.team_23_kotlin.data.posts

import java.util.Date

interface PostsRepository {
    suspend fun getActivePosts(limit: Int = 10): List<PostEntity>
    suspend fun getPostById(id: String): PostEntity

    suspend fun getNewPosts(limit: Int = 10): List<PostEntity>
    suspend fun searchPosts(query: String, limit: Int = 20): List<PostEntity>

}

data class PostEntity(
    val id: String = "",
    val title: String = "",
    val description: String = "",
    val price: Long = 0,
    val images: List<String> = emptyList(),
    val userRef: String = "",
    val status: String = "",
    val createdAt: Date? = null,
    val categoryName: String = "Unknown"
) {
    constructor() : this(
        id = "",
        title = "",
        description = "",
        price = 0,
        images = emptyList(),
        userRef = "",
        status = "",
        createdAt = null,
        categoryName = "Unknown"
    )
}