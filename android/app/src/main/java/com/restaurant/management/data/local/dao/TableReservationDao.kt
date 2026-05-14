package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.restaurant.management.data.local.entity.TableReservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TableReservationDao {
    @Query("SELECT * FROM table_reservations ORDER BY startEpochMillis ASC, id ASC LIMIT 500")
    fun observeAll(): Flow<List<TableReservationEntity>>

    @Query("DELETE FROM table_reservations")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(rows: List<TableReservationEntity>)
}
