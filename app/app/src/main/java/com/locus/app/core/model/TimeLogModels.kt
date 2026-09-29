package com.locus.app.core.model

import java.time.Instant
import java.time.LocalDate

data class TimeLog(
    val id: Long = 0,
    val activityName: String,
    val startTime: Instant,
    val endTime: Instant?,        // null = 进行中
    val date: LocalDate,          // 归属日期（跨天归入开始日）
) {
    val isActive: Boolean get() = endTime == null

    /** 时长秒数；进行中时以 now 计算 */
    fun durationSeconds(now: Instant = Instant.now()): Long =
        java.time.Duration.between(startTime, endTime ?: now).seconds
}

data class FrequentActivity(
    val name: String,
    val useCount: Int,
)
