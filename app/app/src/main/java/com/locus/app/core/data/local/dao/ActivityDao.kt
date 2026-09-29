package com.locus.app.core.data.local.dao

import androidx.room.Dao
import androidx.room.Query
import com.locus.app.core.data.local.entity.ActivityEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ActivityDao {

    @Query("SELECT * FROM activities ORDER BY id ASC")
    fun observeAll(): Flow<List<ActivityEntity>>

    @Query("SELECT * FROM activities WHERE :category IS NULL OR category = :category ORDER BY id ASC")
    suspend fun getByCategory(category: String?): List<ActivityEntity>

    @Query("SELECT COUNT(*) FROM activities")
    suspend fun count(): Int
}
