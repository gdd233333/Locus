package com.locus.app.core.data.room

import com.locus.app.core.data.TimeLogRepository
import com.locus.app.core.data.local.dao.TimeLogDao
import com.locus.app.core.data.local.entity.TimeLogEntity
import com.locus.app.core.data.local.toModel
import com.locus.app.core.model.FrequentActivity
import com.locus.app.core.model.TimeLog
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.Clock
import java.time.LocalDate

class RoomTimeLogRepository(
    private val timeLogDao: TimeLogDao,
    private val clock: Clock = Clock.systemDefaultZone(),
) : TimeLogRepository {

    override fun getActiveLog(): Flow<TimeLog?> =
        timeLogDao.observeActive().map { it?.toModel() }

    override fun getLogsForDate(date: LocalDate): Flow<List<TimeLog>> =
        timeLogDao.observeForDate(date).map { list -> list.map { it.toModel() } }

    override suspend fun startLog(activityName: String): TimeLog {
        // 防御：若已有进行中的记录，先把它结束掉
        timeLogDao.getActive()?.let { endLog(it.id) }
        val now = clock.instant()
        val entity = TimeLogEntity(
            activityName = activityName.trim().ifBlank { "未命名" },
            startTime = now,
            endTime = null,
            // 归属日期 = 开始时间所在日期（跨天归入开始日）
            date = now.atZone(clock.zone).toLocalDate(),
        )
        val id = timeLogDao.insert(entity)
        return entity.copy(id = id).toModel()
    }

    override suspend fun endLog(logId: Long) {
        val entity = timeLogDao.getById(logId) ?: return
        if (entity.endTime == null) {
            timeLogDao.update(entity.copy(endTime = clock.instant()))
        }
    }

    override suspend fun deleteLog(logId: Long) {
        timeLogDao.deleteById(logId)
    }

    override fun getFrequentActivities(): Flow<List<FrequentActivity>> =
        timeLogDao.observeFrequent(FREQUENT_LIMIT).map { rows ->
            rows.map { FrequentActivity(it.name, it.useCount) }
        }

    private companion object {
        const val FREQUENT_LIMIT = 6
    }
}
