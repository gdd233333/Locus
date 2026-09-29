package com.locus.app.core.data

import com.locus.app.core.model.FrequentActivity
import com.locus.app.core.model.TimeLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 内存假仓库：预置今天 3 条已完成 + 1 条进行中（45 分钟前开始的"刷手机"），
 * 以及昨天 3 条，方便验证日期切换。
 * 状态保存在 MutableStateFlow 中，start/end/delete 会真实更新 UI（仅本次运行内有效）。
 */
class FakeTimeLogRepository {

    private val zone: ZoneId = ZoneId.systemDefault()
    private var nextId = 100L

    /** 今天某时刻的 Instant，minuteOffset 为相对该时刻的分钟偏移 */
    private fun todayAt(hour: Int, minute: Int, minuteOffset: Long = 0): Instant =
        LocalDate.now().atTime(LocalTime.of(hour, minute)).atZone(zone)
            .toInstant().plus(minuteOffset, ChronoUnit.MINUTES)

    private val initialLogs: List<TimeLog> = run {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        listOf(
            // 今天已完成的 3 条（与设计稿一致）
            TimeLog(1, "晚餐", todayAt(19, 0), todayAt(19, 45), today),
            TimeLog(2, "图书馆自习", todayAt(19, 45), todayAt(21, 30), today),
            TimeLog(3, "走路回宿舍", todayAt(21, 30), todayAt(22, 3), today),
            // 今天进行中：45 分钟前开始
            TimeLog(4, "刷手机", Instant.now().minus(45, ChronoUnit.MINUTES), null, today),
            // 昨天的 3 条（用于验证日期切换）
            TimeLog(5, "睡懒觉", yesterday.atTime(9, 30).atZone(zone).toInstant(),
                yesterday.atTime(11, 0).atZone(zone).toInstant(), yesterday),
            TimeLog(6, "打游戏的下午", yesterday.atTime(14, 0).atZone(zone).toInstant(),
                yesterday.atTime(17, 20).atZone(zone).toInstant(), yesterday),
            TimeLog(7, "夜跑", yesterday.atTime(20, 0).atZone(zone).toInstant(),
                yesterday.atTime(20, 40).atZone(zone).toInstant(), yesterday),
        )
    }

    private val logs = MutableStateFlow(initialLogs)

    fun getActiveLog(): Flow<TimeLog?> = logs.map { list -> list.firstOrNull { it.isActive } }

    fun getLogsForDate(date: LocalDate): Flow<List<TimeLog>> =
        logs.map { list -> list.filter { it.date == date }.sortedBy { it.startTime } }

    suspend fun startLog(activityName: String): TimeLog {
        // 防御：若已有进行中的记录，先把它结束掉
        logs.value.firstOrNull { it.isActive }?.let { endLog(it.id) }
        val now = Instant.now()
        val log = TimeLog(
            id = nextId++,
            activityName = activityName.trim().ifBlank { "未命名" },
            startTime = now,
            endTime = null,
            date = LocalDate.now(),
        )
        logs.value = logs.value + log
        return log
    }

    suspend fun endLog(logId: Long) {
        val now = Instant.now()
        logs.value = logs.value.map { if (it.id == logId && it.isActive) it.copy(endTime = now) else it }
    }

    suspend fun deleteLog(logId: Long) {
        logs.value = logs.value.filterNot { it.id == logId }
    }

    fun getFrequentActivities(): List<FrequentActivity> = listOf(
        FrequentActivity("刷手机", 12),
        FrequentActivity("学习", 8),
        FrequentActivity("吃饭", 7),
        FrequentActivity("睡觉", 6),
        FrequentActivity("运动", 4),
        FrequentActivity("发呆", 3),
    )
}
