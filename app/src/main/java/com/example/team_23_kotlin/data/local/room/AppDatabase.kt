package com.example.team_23_kotlin.data.local.room

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [ChatEntity::class, ReceiptEntity::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun chatDao(): ChatDao

    abstract fun receiptDao(): ReceiptDao
}
