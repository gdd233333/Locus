package com.locus.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "daily_check_ins")
data class DailyCheckInEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val mood: Int,
    val note: String? = null,
    @ColumnInfo(name = "created_at") val createdAt: Instant,
)
