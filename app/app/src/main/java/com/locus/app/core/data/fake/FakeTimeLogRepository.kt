package com.locus.app.core.data.fake

import com.locus.app.core.data.TimeLogRepository
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
 * 参考实现（内存假数据）：预置今天 3 条已完成 + 1 条进行中（45 分钟前开始的"刷手机"），
 * 以及昨天 3 条。Room 实现见 core/data/room/RoomTimeLogRepository（不再预置历史）。
 */
class FakeTimeLogRepository : TimeLogRepository {

    private val zone: ZoneId = ZoneId.systemDefault()
    private var nextId = 100L

    private fun todayAt(hour: Int, minute: Int, minuteOffset: Long = 0): Instant =
        LocalDate.now().atTime(LocalTime.of(hour, minute)).atZone(zone)
            .toInstant().plus(minuteOffset, ChronoUnit.MINUTES)

    private val initialLogs: List<TimeLog> = run {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        listOf(
            TimeLog(1, "晚餐", todayAt(19, 0), todayAt(19, 45), today),
            TimeLog(2, "图书馆自习", todayAt(19, 45), todayAt(21, 30), today),
            TimeLog(3, "走路回宿舍", todayAt(21, 30), todayAt(22, 3), today),
            TimeLog(4, "刷手机", Instant.now().minus(45, ChronoUnit.MINUTES), null, today),
            TimeLog(5, "睡懒觉", yesterday.atTime(9, 30).atZone(zone).toInstant(),
                yesterday.atTime(11, 0).atZone(zone).toInstant(), yesterday),
            TimeLog(6, "打游戏的下午", yesterday.atTime(14, 0).atZone(zone).toInstant(),
                yesterday.atTime(17, 20).atZone(zone).toInstant(), yesterday),
            TimeLog(7, "夜跑", yesterday.atTime(20, 0).atZone(zone).toInstant(),
                yesterday.atTime(20, 40).atZone(zone).toInstant(), yesterday),
        )
    }

    private val logs = MutableStateFlow(initialLogs)

    override fun getActiveLog(): Flow<TimeLog?> = logs.map { list -> list.firstOrNull { it.isActive } }

    override fun getLogsForDate(date: LocalDate): Flow<List<TimeLog>> =
        logs.map { list -> list.filter { it.date == date }.sortedBy { it.startTime } }

    override suspend fun startLog(activityName: String): TimeLog {
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

    override suspend fun endLog(logId: Long) {
        val now = Instant.now()
        logs.value = logs.value.map { if (it.id == logId && it.isActive) it.copy(endTime = now) else it }
    }

    override suspend fun deleteLog(logId: Long) {
        logs.value = logs.value.filterNot { it.id == logId }
    }

    override fun getFrequentActivities(): Flow<List<FrequentActivity>> = logs.map { list ->
        list.groupingBy { it.activityName }.eachCount()
            .entries
            .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
            .take(6)
            .map { FrequentActivity(it.key, it.value) }
    }
}
