package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.restaurant.management.data.local.entity.StaffEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffDao {
    @Query("SELECT * FROM staff ORDER BY name")
    fun observeStaff(): Flow<List<StaffEntity>>

    @Insert
    suspend fun insertAll(items: List<StaffEntity>)

    @Update
    suspend fun update(staff: StaffEntity)
}
