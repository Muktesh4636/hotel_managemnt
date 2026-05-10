package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.restaurant.management.data.local.entity.TableEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableDao {
    @Query("SELECT * FROM tables ORDER BY section, label")
    fun observeTables(): Flow<List<TableEntity>>

    @Query("SELECT * FROM tables WHERE id = :id")
    suspend fun getById(id: Long): TableEntity?

    @Query("SELECT COUNT(*) FROM tables")
    suspend fun count(): Int

    @Query(
        "SELECT COUNT(*) FROM tables WHERE status = :status",
    )
    fun observeCountWithStatus(status: String): Flow<Int>

    @Insert
    suspend fun insertAll(items: List<TableEntity>)

    @Update
    suspend fun update(table: TableEntity)
}
