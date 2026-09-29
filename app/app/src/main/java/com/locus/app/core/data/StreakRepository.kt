package com.locus.app.core.data

import com.locus.app.core.model.DailyCheckIn
import com.locus.app.core.model.UrgeEvent
import com.locus.app.core.model.UrgeIntensity
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.YearMonth

interface StreakRepository {

    fun getCurrentStreakDays(): Flow<Int>

    fun isPersonalBest(): Flow<Boolean>

    fun getTodayUrgeCount(): Flow<Int>

    fun getLatestUrge(): Flow<UrgeEvent?>

    /** 近 7 日归一化冲动强度，0.0~1.0，从 6 天前到今天 */
    fun getRecentUrgeIntensities(): Flow<List<Float>>

    fun getTodayCheckIn(): Flow<DailyCheckIn?>

    /** 打卡：同日重复打卡只更新原记录；无活跃 streak 时新建 */
    suspend fun checkIn(mood: Int, note: String?)

    /** 记录一次冲动 */
    suspend fun logUrge(intensity: UrgeIntensity)

    /** 标记最近一条未平复的冲动为已平复 */
    suspend fun resolveLatestUrge(durationMinutes: Int, method: String)

    /** 日期区间内每天冲动次数（按本地日期分桶，缺数据补 0），复盘曲线用 */
    fun getDailyUrgeCounts(from: LocalDate, to: LocalDate): Flow<List<Pair<LocalDate, Int>>>

    /** 日期区间内成功平复的冲动次数 */
    fun getResolvedUrgeCount(from: LocalDate, to: LocalDate): Flow<Int>

    /** 指定月份内所有打卡日期，复盘日历用 */
    fun getCheckInDatesInMonth(month: YearMonth): Flow<Set<LocalDate>>
}
