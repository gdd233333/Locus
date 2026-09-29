package com.locus.app.core.data.fake

import com.locus.app.core.data.StreakRepository
import com.locus.app.core.model.DailyCheckIn
import com.locus.app.core.model.UrgeEvent
import com.locus.app.core.model.UrgeIntensity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 参考实现（内存假数据），不再被 ViewModel 引用。
 * 接口签名即业务契约，Room 实现见 core/data/room/RoomStreakRepository。
 */
class FakeStreakRepository : StreakRepository {

    private var streakStartDate: LocalDate? = LocalDate.now().minusDays(22) // 今天起算 = 23 天

    private val urgeEvents = mutableListOf(
        UrgeEvent(
            id = 1,
            timestamp = Instant.now().minus(80, ChronoUnit.MINUTES),
            intensity = UrgeIntensity.MODERATE,
            durationMinutes = 12,
            resolved = true,
            resolutionMethod = "冲浪练习",
        )
    )

    private var checkIn: DailyCheckIn? = null

    override fun getCurrentStreakDays(): Flow<Int> = flowOf(
        streakStartDate?.let { (ChronoUnit.DAYS.between(it, LocalDate.now()) + 1).toInt() } ?: 0
    )

    override fun isPersonalBest(): Flow<Boolean> = flowOf(true)

    override fun getTodayUrgeCount(): Flow<Int> = flowOf(
        urgeEvents.count {
            it.timestamp.atZone(ZoneId.systemDefault()).toLocalDate() == LocalDate.now()
        }
    )

    override fun getLatestUrge(): Flow<UrgeEvent?> = flowOf(urgeEvents.maxByOrNull { it.timestamp })

    override fun getRecentUrgeIntensities(): Flow<List<Float>> = flowOf(
        listOf(0.2f, 0.5f, 0.8f, 0.4f, 0.35f, 0.6f, 0.25f)
    )

    override fun getTodayCheckIn(): Flow<DailyCheckIn?> = flowOf(checkIn)

    override suspend fun checkIn(mood: Int, note: String?) {
        checkIn = DailyCheckIn(
            id = 1,
            date = LocalDate.now(),
            mood = mood,
            note = note,
            createdAt = Instant.now(),
        )
        if (streakStartDate == null) streakStartDate = LocalDate.now()
    }

    override suspend fun logUrge(intensity: UrgeIntensity) {
        urgeEvents += UrgeEvent(
            id = (urgeEvents.maxOfOrNull { it.id } ?: 0L) + 1,
            timestamp = Instant.now(),
            intensity = intensity,
        )
    }

    override suspend fun resolveLatestUrge(durationMinutes: Int, method: String) {
        val latest = urgeEvents.filter { !it.resolved }.maxByOrNull { it.timestamp } ?: return
        val index = urgeEvents.indexOf(latest)
        urgeEvents[index] = latest.copy(
            resolved = true,
            durationMinutes = durationMinutes,
            resolutionMethod = method,
        )
    }
}
