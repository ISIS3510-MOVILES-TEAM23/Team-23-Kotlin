package com.example.team_23_kotlin.data.local.room

import javax.inject.Inject

class LocalChatRepository @Inject constructor(
    private val dao: ChatDao
) {
    suspend fun saveChats(chats: List<ChatEntity>) = dao.insertChats(chats)
    suspend fun getChats(): List<ChatEntity> = dao.getAllChats()
}
