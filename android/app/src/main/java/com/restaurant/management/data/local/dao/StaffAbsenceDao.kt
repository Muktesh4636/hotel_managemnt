package com.restaurant.management.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import com.restaurant.management.data.local.entity.StaffAbsenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StaffAbsenceDao {
    @Query(
        "SELECT * FROM staff_absences ORDER BY dayStartEpochMillis DESC LIMIT 3000",
    )
    fun observeAll(): Flow<List<StaffAbsenceEntity>>

    @Insert
    suspend fun insert(row: StaffAbsenceEntity): Long

    @Delete
    suspend fun delete(row: StaffAbsenceEntity)

    @Query(
        "SELECT COUNT(*) FROM staff_absences WHERE staffId = :staffId AND dayStartEpochMillis = :dayStart",
    )
    suspend fun countForDay(
        staffId: Long,
        dayStart: Long,
    ): Int

    @Query("DELETE FROM staff_absences WHERE staffId = :staffId")
    suspend fun deleteAllForStaff(staffId: Long)

    @Query("DELETE FROM staff_absences")
    suspend fun deleteAll()
}
