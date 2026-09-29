package com.locus.app.notification

import android.content.Context
import com.locus.app.LocusApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * App 进程存活期间的同步器：
 * 1) 晚间提醒跟随设置（开关/时间）变化重排
 * 2) 记录中常驻通知跟随 time_logs 的活跃记录出现/消失（计时由系统 chronometer 渲染）
 */
object NotificationSync {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    fun start(context: Context) {
        val app = context.applicationContext as? LocusApplication ?: return

        scope.launch {
            runCatching {
                EveningReminderScheduler.schedule(app)
                combine(
                    app.container.settingsRepository.eveningReminderEnabled,
                    app.container.settingsRepository.eveningReminderTime,
                ) { enabled, time -> enabled to time }
                    .distinctUntilChanged()
                    .drop(1)
                    .collect { EveningReminderScheduler.schedule(app) }
            }
        }

        scope.launch {
            runCatching {
                app.container.timeLogRepository.getActiveLog().collect { active ->
                    if (active == null) {
                        LocusNotifications.cancelOngoing(app)
                    } else {
                        val startedAt = active.startTime
                            .atZone(ZoneId.systemDefault())
                            .format(timeFormatter)
                        LocusNotifications.showOngoing(
                            context = app,
                            activityName = active.activityName,
                            startTimeMillis = active.startTime.toEpochMilli(),
                            startedAtText = startedAt,
                        )
                    }
                }
            }
        }
    }
}
