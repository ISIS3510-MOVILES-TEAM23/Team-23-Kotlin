package com.example.team_23_kotlin.data.local.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReceiptDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(receipt: ReceiptEntity)

    @Query("SELECT * FROM receipts WHERE purchaseId = :id LIMIT 1")
    suspend fun getReceipt(id: String): ReceiptEntity?

    @Query("SELECT purchaseId FROM receipts")
    suspend fun getAll(): List<String>
}
