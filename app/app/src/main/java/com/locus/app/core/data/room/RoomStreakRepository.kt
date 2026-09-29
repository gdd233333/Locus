package com.locus.app.core.data.room

import com.locus.app.core.data.StreakRepository
import com.locus.app.core.data.local.dao.CheckInDao
import com.locus.app.core.data.local.dao.StreakDao
import com.locus.app.core.data.local.dao.UrgeDao
import com.locus.app.core.data.local.entity.DailyCheckInEntity
import com.locus.app.core.data.local.entity.StreakRecordEntity
import com.locus.app.core.data.local.entity.UrgeEventEntity
import com.locus.app.core.data.local.toModel
import com.locus.app.core.model.DailyCheckIn
import com.locus.app.core.model.UrgeEvent
import com.locus.app.core.model.UrgeIntensity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

class RoomStreakRepository(
    private val streakDao: StreakDao,
    private val urgeDao: UrgeDao,
    private val checkInDao: CheckInDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) : StreakRepository {

    override fun getCurrentStreakDays(): Flow<Int> =
        streakDao.observeActive().map { active ->
            active?.let { daysSince(it.startDate) } ?: 0
        }

    override fun isPersonalBest(): Flow<Boolean> =
        combine(streakDao.observeActive(), streakDao.observeAll()) { active, all ->
            val current = active?.let { daysSince(it.startDate) } ?: 0
            val best = all.maxOfOrNull { streakLength(it) } ?: 0
            current > 0 && current >= best
        }

    override fun getTodayUrgeCount(): Flow<Int> =
        urgeDao.observeBetween(dayStart(today()), dayStart(today().plusDays(1)))
            .map { events -> events.size }

    override fun getLatestUrge(): Flow<UrgeEvent?> =
        urgeDao.observeLatest().map { it?.toModel() }

    override fun getRecentUrgeIntensities(): Flow<List<Float>> =
        urgeDao.observeBetween(dayStart(today().minusDays((DAYS - 1).toLong())), dayStart(today().plusDays(1)))
            .map { events ->
                val strongestPerDay = events
                    .groupBy { it.timestamp.atZone(clock.zone).toLocalDate() }
                    .mapValues { (_, dayEvents) -> dayEvents.maxOf { intensityWeight(it.intensity) } }
                // 从 6 天前到今天，缺数据的日子给 0.05 让波浪线不断底
                (DAYS - 1 downTo 0).map { offset ->
                    val date = today().minusDays(offset.toLong())
                    (strongestPerDay[date] ?: NO_URGE_WEIGHT).toFloat()
                }
            }

    override fun getTodayCheckIn(): Flow<DailyCheckIn?> =
        checkInDao.observeOn(today()).map { it?.toModel() }

    override suspend fun checkIn(mood: Int, note: String?) {
        val date = today()
        val existing = checkInDao.getOn(date)
        if (existing != null) {
            checkInDao.update(existing.copy(mood = mood, note = note))
        } else {
            checkInDao.insert(
                DailyCheckInEntity(date = date, mood = mood, note = note, createdAt = clock.instant())
            )
        }
        if (streakDao.getActive() == null) {
            streakDao.insert(StreakRecordEntity(startDate = date, endDate = null, isActive = true))
        }
    }

    override suspend fun logUrge(intensity: UrgeIntensity) {
        urgeDao.insert(UrgeEventEntity(timestamp = clock.instant(), intensity = intensity))
    }

    override suspend fun resolveLatestUrge(durationMinutes: Int, method: String) {
        val latest = urgeDao.getLatestUnresolved() ?: return
        urgeDao.update(
            latest.copy(resolved = true, durationMinutes = durationMinutes, resolutionMethod = method)
        )
    }

    override fun getDailyUrgeCounts(from: LocalDate, to: LocalDate): Flow<List<Pair<LocalDate, Int>>> =
        urgeDao.observeBetween(dayStart(from), dayStart(to.plusDays(1))).map { events ->
            val counts = events.groupingBy {
                it.timestamp.atZone(clock.zone).toLocalDate()
            }.eachCount()
            datesUntil(from, to).map { date -> date to (counts[date] ?: 0) }
        }

    override fun getResolvedUrgeCount(from: LocalDate, to: LocalDate): Flow<Int> =
        urgeDao.observeResolvedCountBetween(dayStart(from), dayStart(to.plusDays(1)))

    override fun getCheckInDatesInMonth(month: YearMonth): Flow<Set<LocalDate>> =
        checkInDao.observeDatesBetween(month.atDay(1), month.atEndOfMonth()).map { it.toSet() }

    private fun datesUntil(from: LocalDate, to: LocalDate): List<LocalDate> =
        generateSequence(from) { it.plusDays(1) }
            .takeWhile { !it.isAfter(to) }
            .toList()

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun dayStart(date: LocalDate) = date.atStartOfDay(clock.zone).toInstant()

    /** streak 天数 = 今天 - startDate + 1 */
    private fun daysSince(startDate: LocalDate): Int =
        (ChronoUnit.DAYS.between(startDate, today()) + 1).toInt().coerceAtLeast(0)

    private fun streakLength(record: StreakRecordEntity): Int =
        (ChronoUnit.DAYS.between(record.startDate, record.endDate ?: today()) + 1)
            .toInt().coerceAtLeast(0)

    private fun intensityWeight(intensity: UrgeIntensity): Double = when (intensity) {
        UrgeIntensity.MILD -> 0.33
        UrgeIntensity.MODERATE -> 0.66
        UrgeIntensity.STRONG -> 1.0
    }

    private companion object {
        const val DAYS = 7
        const val NO_URGE_WEIGHT = 0.05
    }
}
