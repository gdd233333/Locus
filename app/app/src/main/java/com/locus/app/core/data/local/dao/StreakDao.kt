package com.locus.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.locus.app.core.data.local.entity.StreakRecordEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StreakDao {

    @Query("SELECT * FROM streak_records WHERE is_active = 1 ORDER BY start_date DESC LIMIT 1")
    fun observeActive(): Flow<StreakRecordEntity?>

    @Query("SELECT * FROM streak_records WHERE is_active = 1 ORDER BY start_date DESC LIMIT 1")
    suspend fun getActive(): StreakRecordEntity?

    @Query("SELECT * FROM streak_records")
    fun observeAll(): Flow<List<StreakRecordEntity>>

    @Insert
    suspend fun insert(record: StreakRecordEntity): Long

    @Update
    suspend fun update(record: StreakRecordEntity)
}
