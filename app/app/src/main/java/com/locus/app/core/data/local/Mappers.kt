package com.locus.app.core.data.local

import com.locus.app.core.data.local.entity.ActivityEntity
import com.locus.app.core.data.local.entity.DailyCheckInEntity
import com.locus.app.core.data.local.entity.StreakRecordEntity
import com.locus.app.core.data.local.entity.TimeLogEntity
import com.locus.app.core.data.local.entity.UrgeEventEntity
import com.locus.app.core.model.Activity
import com.locus.app.core.model.DailyCheckIn
import com.locus.app.core.model.StreakRecord
import com.locus.app.core.model.TimeLog
import com.locus.app.core.model.UrgeEvent

fun StreakRecordEntity.toModel(): StreakRecord = StreakRecord(
    id = id,
    startDate = startDate,
    endDate = endDate,
    isActive = isActive,
)

fun UrgeEventEntity.toModel(): UrgeEvent = UrgeEvent(
    id = id,
    timestamp = timestamp,
    intensity = intensity,
    durationMinutes = durationMinutes,
    triggerNote = triggerNote,
    resolved = resolved,
    resolutionMethod = resolutionMethod,
)

fun DailyCheckInEntity.toModel(): DailyCheckIn = DailyCheckIn(
    id = id,
    date = date,
    mood = mood,
    note = note,
    createdAt = createdAt,
)

fun ActivityEntity.toModel(): Activity = Activity(
    id = id,
    title = title,
    description = description,
    durationMinutes = durationMinutes,
    category = category,
    tags = tags,
    isBuiltIn = isBuiltIn,
)

fun TimeLogEntity.toModel(): TimeLog = TimeLog(
    id = id,
    activityName = activityName,
    startTime = startTime,
    endTime = endTime,
    date = date,
)
