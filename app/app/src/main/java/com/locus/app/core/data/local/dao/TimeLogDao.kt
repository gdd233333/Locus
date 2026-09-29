package com.locus.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.locus.app.core.data.local.entity.TimeLogEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

/** time_logs 聚合投影：常用活动（非独立表） */
data class FrequentActivityRow(
    val name: String,
    val useCount: Int,
)

@Dao
interface TimeLogDao {

    @Query("SELECT * FROM time_logs WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    fun observeActive(): Flow<TimeLogEntity?>

    @Query("SELECT * FROM time_logs WHERE end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    suspend fun getActive(): TimeLogEntity?

    @Query("SELECT * FROM time_logs WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): TimeLogEntity?

    @Query("SELECT * FROM time_logs WHERE date = :date ORDER BY start_time ASC")
    fun observeForDate(date: LocalDate): Flow<List<TimeLogEntity>>

    @Insert
    suspend fun insert(log: TimeLogEntity): Long

    @Update
    suspend fun update(log: TimeLogEntity)

    @Query("DELETE FROM time_logs WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query(
        "SELECT activity_name AS name, COUNT(*) AS useCount FROM time_logs " +
            "GROUP BY activity_name ORDER BY useCount DESC, name ASC LIMIT :limit"
    )
    fun observeFrequent(limit: Int): Flow<List<FrequentActivityRow>>
}
