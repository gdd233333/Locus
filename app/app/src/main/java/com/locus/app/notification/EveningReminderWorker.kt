package com.locus.app.notification

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.locus.app.LocusApplication
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlin.math.abs
import kotlin.math.min

/**
 * 晚间守护提醒：按 eveningReminderTime 计算到目标时刻的 initialDelay，之后 24h 周期。
 * 今日已打卡则当天不发；与目标时刻漂移超过 30 分钟时自愈重排。
 */
class EveningReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? LocusApplication ?: return Result.success()
        val container = app.container

        val enabled = container.settingsRepository.eveningReminderEnabled.first()
        if (!enabled) return Result.success()

        val checkedIn = container.streakRepository.getTodayCheckIn().first() != null
        if (!checkedIn) {
            LocusNotifications.showReminder(applicationContext)
        }

        val time = container.settingsRepository.eveningReminderTime.first()
        if (EveningReminderScheduler.driftMillis(time) > DRIFT_TOLERANCE_MILLIS) {
            EveningReminderScheduler.schedule(applicationContext)
        }
        return Result.success()
    }

    private companion object {
        const val DRIFT_TOLERANCE_MILLIS = 30 * 60 * 1000L
    }
}

object EveningReminderScheduler {

    private const val WORK_NAME = "locus_evening_reminder"
    private const val DEFAULT_HOUR = 21
    private const val DEFAULT_MINUTE = 30

    /** 读设置后重排（设置变更、开机、漂移自愈都走这里） */
    suspend fun schedule(context: Context) {
        val app = context.applicationContext as? LocusApplication ?: return
        val time = app.container.settingsRepository.eveningReminderTime.first()
        val request = PeriodicWorkRequestBuilder<EveningReminderWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntilNext(time), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            request,
        )
    }

    /** 距下一个目标时刻的毫秒数（今天的时间点已过 → 顺延到明天） */
    fun delayUntilNext(time: String): Long {
        val minutes = parseMinutes(time)
        val now = LocalDateTime.now()
        var target = now.toLocalDate().atTime(minutes / 60, minutes % 60)
        if (!target.isAfter(now)) target = target.plusDays(1)
        return Duration.between(now, target).toMillis()
    }

    /** 当前时刻与目标时刻的最小环形偏差（毫秒），用于漂移自愈 */
    fun driftMillis(time: String): Long {
        val minutes = parseMinutes(time)
        val now = LocalDateTime.now()
        val nowMinutes = now.hour * 60 + now.minute
        val diff = abs(nowMinutes - minutes)
        return min(diff, 24 * 60 - diff).toLong() * 60_000L
    }

    private fun parseMinutes(time: String): Int {
        val parts = time.split(":")
        val hour = parts.getOrNull(0)?.toIntOrNull() ?: DEFAULT_HOUR
        val minute = parts.getOrNull(1)?.toIntOrNull() ?: DEFAULT_MINUTE
        return hour.coerceIn(0, 23) * 60 + minute.coerceIn(0, 59)
    }
}
