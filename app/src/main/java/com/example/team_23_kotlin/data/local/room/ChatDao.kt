package com.example.team_23_kotlin.data.local.room

import androidx.room.*

@Dao
interface ChatDao {

    @Query("SELECT * FROM chats ORDER BY lastTime DESC")
    suspend fun getAllChats(): List<ChatEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ChatEntity>)

    @Query("DELETE FROM chats")
    suspend fun clearChats()
}
