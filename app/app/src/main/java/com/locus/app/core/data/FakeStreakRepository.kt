package com.locus.app.core.data

import com.locus.app.core.model.DailyCheckIn
import com.locus.app.core.model.UrgeEvent
import com.locus.app.core.model.UrgeIntensity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * 假数据仓库：Stage 3 阶段驱动 UI，Stage 6 之后替换为 Room 实现。
 * 接口签名即业务契约，替换实现时签名不得改变。
 */
class FakeStreakRepository {

    fun getCurrentStreakDays(): Flow<Int> = flowOf(23)

    fun isPersonalBest(): Flow<Boolean> = flowOf(true)

    fun getTodayUrgeCount(): Flow<Int> = flowOf(1)

    fun getLatestUrge(): Flow<UrgeEvent?> = flowOf(
        UrgeEvent(
            id = 1,
            timestamp = Instant.now().minus(80, ChronoUnit.MINUTES),
            intensity = UrgeIntensity.MODERATE,
            durationMinutes = 12,
            resolved = true,
            resolutionMethod = "冲浪练习",
        )
    )

    /** 近 7 日冲动强度，0.0~1.0 归一化，用于波浪线 */
    fun getRecentUrgeIntensities(): Flow<List<Float>> = flowOf(
        listOf(0.2f, 0.5f, 0.8f, 0.4f, 0.35f, 0.6f, 0.25f)
    )

    fun getTodayCheckIn(): Flow<DailyCheckIn?> = flowOf(null)
}
