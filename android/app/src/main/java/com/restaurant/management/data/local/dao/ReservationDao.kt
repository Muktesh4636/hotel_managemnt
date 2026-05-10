package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.restaurant.management.data.local.entity.ReservationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReservationDao {
    @Query("SELECT * FROM reservations ORDER BY atEpochMillis ASC LIMIT 200")
    fun observeReservations(): Flow<List<ReservationEntity>>

    @Insert
    suspend fun insert(r: ReservationEntity): Long

    @Update
    suspend fun update(r: ReservationEntity)

    @Delete
    suspend fun delete(r: ReservationEntity)
}
