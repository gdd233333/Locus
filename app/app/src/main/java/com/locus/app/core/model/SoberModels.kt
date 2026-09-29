package com.locus.app.core.model

import java.time.Instant
import java.time.LocalDate

data class StreakRecord(
    val id: Long = 0,
    val startDate: LocalDate,
    val endDate: LocalDate? = null,
    val isActive: Boolean = true,
)

enum class UrgeIntensity { MILD, MODERATE, STRONG }

data class UrgeEvent(
    val id: Long = 0,
    val timestamp: Instant,
    val intensity: UrgeIntensity,
    val durationMinutes: Int? = null,
    val triggerNote: String? = null,
    val resolved: Boolean = false,
    val resolutionMethod: String? = null,
)

data class DailyCheckIn(
    val id: Long = 0,
    val date: LocalDate,
    val mood: Int,
    val note: String? = null,
    val createdAt: Instant,
)
