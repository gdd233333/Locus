package com.locus.app.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.locus.app.LocusApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/** 通知「直接打卡」：不打开 app 直接写打卡，然后取消当天提醒 */
class NotificationCheckInReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (appContext as? LocusApplication)?.container
                container?.streakRepository?.checkIn(mood = DEFAULT_MOOD, note = null)
                LocusNotifications.cancelReminder(appContext)
            } finally {
                pending.finish()
            }
        }
    }

    private companion object {
        const val DEFAULT_MOOD = 3
    }
}

/** 通知「结束记录」：结束进行中的记录并撤掉常驻通知 */
class NotificationEndRecordingReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val container = (appContext as? LocusApplication)?.container
                container?.timeLogRepository?.getActiveLog()?.first()?.let { active ->
                    container.timeLogRepository.endLog(active.id)
                }
                LocusNotifications.cancelOngoing(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}

/** 开机 / 应用更新后恢复晚间提醒调度 */
class NotificationBootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != Intent.ACTION_MY_PACKAGE_REPLACED
        ) {
            return
        }
        val pending = goAsync()
        val appContext = context.applicationContext
        CoroutineScope(Dispatchers.Default).launch {
            try {
                EveningReminderScheduler.schedule(appContext)
            } finally {
                pending.finish()
            }
        }
    }
}
