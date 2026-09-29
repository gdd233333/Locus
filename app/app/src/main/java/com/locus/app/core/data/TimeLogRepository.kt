package com.locus.app.core.data

import com.locus.app.core.model.FrequentActivity
import com.locus.app.core.model.TimeLog
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TimeLogRepository {

    fun getActiveLog(): Flow<TimeLog?>

    fun getLogsForDate(date: LocalDate): Flow<List<TimeLog>>

    suspend fun startLog(activityName: String): TimeLog

    suspend fun endLog(logId: Long)

    suspend fun deleteLog(logId: Long)

    /** 常用活动：由 time_logs 按 activity_name 聚合，COUNT 降序取前 N 条 */
    fun getFrequentActivities(): Flow<List<FrequentActivity>>
}
