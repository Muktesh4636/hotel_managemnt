package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.restaurant.management.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY name")
    fun observeStaff(): Flow<List<StaffEntity>>

    @Query("SELECT IFNULL(SUM(salaryCents), 0) FROM staff")
    suspend fun sumMonthlySalaries(): Int

    @Insert
    suspend fun insertAll(items: List<StaffEntity>)

    @Insert
    suspend fun insert(staff: StaffEntity): Long

    @Update
    suspend fun update(staff: StaffEntity)

    @Delete
    suspend fun delete(staff: StaffEntity)

    @Query("DELETE FROM staff")
    suspend fun deleteAll()
}
