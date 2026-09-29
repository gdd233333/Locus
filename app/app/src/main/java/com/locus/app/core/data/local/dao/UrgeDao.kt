package com.locus.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.locus.app.core.data.local.entity.UrgeEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface UrgeDao {

    @Query("SELECT * FROM urge_events WHERE timestamp >= :start AND timestamp < :end ORDER BY timestamp ASC")
    fun observeBetween(start: Instant, end: Instant): Flow<List<UrgeEventEntity>>

    @Query("SELECT * FROM urge_events ORDER BY timestamp DESC, id DESC LIMIT 1")
    fun observeLatest(): Flow<UrgeEventEntity?>

    @Query("SELECT * FROM urge_events WHERE resolved = 0 ORDER BY timestamp DESC, id DESC LIMIT 1")
    suspend fun getLatestUnresolved(): UrgeEventEntity?

    @Query("SELECT COUNT(*) FROM urge_events WHERE timestamp >= :start AND timestamp < :end AND resolved = 1")
    fun observeResolvedCountBetween(start: Instant, end: Instant): Flow<Int>

    @Insert
    suspend fun insert(event: UrgeEventEntity): Long

    @Update
    suspend fun update(event: UrgeEventEntity)
}
