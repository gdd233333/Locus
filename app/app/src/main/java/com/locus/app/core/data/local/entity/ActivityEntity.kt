package com.locus.app.core.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.locus.app.core.model.ActivityCategory

@Entity(tableName = "activities")
data class ActivityEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String,
    @ColumnInfo(name = "duration_minutes") val durationMinutes: Int,
    val category: ActivityCategory,
    val tags: List<String> = emptyList(),
    @ColumnInfo(name = "is_built_in") val isBuiltIn: Boolean = true,
)
