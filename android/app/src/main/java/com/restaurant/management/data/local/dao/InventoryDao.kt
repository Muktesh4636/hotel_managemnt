package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.restaurant.management.data.local.entity.InventoryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory ORDER BY name")
    fun observeInventory(): Flow<List<InventoryEntity>>

    @Insert
    suspend fun insert(item: InventoryEntity): Long

    @Insert
    suspend fun insertAll(items: List<InventoryEntity>)

    @Update
    suspend fun update(item: InventoryEntity)

    @Delete
    suspend fun delete(item: InventoryEntity)
}
