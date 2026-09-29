package com.locus.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.locus.app.core.model.UrgeIntensity
import java.time.Instant

@Entity(tableName = "urge_events")
data class UrgeEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val timestamp: Instant,
    val intensity: UrgeIntensity,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int? = null,
    @ColumnInfo(name = "trigger_note") val triggerNote: String? = null,
    val resolved: Boolean = false,
    @ColumnInfo(name = "resolution_method") val resolutionMethod: String? = null,
)
