package com.locus.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.locus.app.core.data.local.entity.DailyCheckInEntity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface CheckInDao {

    @Query("SELECT * FROM daily_check_ins WHERE date = :date LIMIT 1")
    fun observeOn(date: LocalDate): Flow<DailyCheckInEntity?>

    @Query("SELECT * FROM daily_check_ins WHERE date = :date LIMIT 1")
    suspend fun getOn(date: LocalDate): DailyCheckInEntity?

    @Query("SELECT date FROM daily_check_ins WHERE date >= :from AND date <= :to ORDER BY date ASC")
    fun observeDatesBetween(from: LocalDate, to: LocalDate): Flow<List<LocalDate>>

    @Insert
    suspend fun insert(checkIn: DailyCheckInEntity): Long

    @Update
    suspend fun update(checkIn: DailyCheckInEntity)
}
