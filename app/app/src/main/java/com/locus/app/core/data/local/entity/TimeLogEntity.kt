package com.locus.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

@Entity(tableName = "time_logs")
data class TimeLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "activity_name") val activityName: String,
    @ColumnInfo(name = "start_time") val startTime: Instant,
    @ColumnInfo(name = "end_time") val endTime: Instant? = null,
    val date: LocalDate,
)
