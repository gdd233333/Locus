package com.locus.app.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.locus.app.MainActivity
import com.locus.app.R

/** 通知构建与发送（文案统一走 strings.xml） */
object LocusNotifications {

    const val ID_REMINDER = 9001
    const val ID_ONGOING = 9002

    private const val DEEP_LINK_SOBER = "locus://sober"
    private const val DEEP_LINK_TIMELOG = "locus://timelog"

    /** 晚间守护提醒（点击进守护页 + 直接打卡 action） */
    fun showReminder(context: Context) {
        val notification = NotificationCompat.Builder(context, NotificationChannels.REMINDER)
            .setSmallIcon(R.drawable.ic_notification_locus)
            .setContentTitle(context.getString(R.string.notif_reminder_title))
            .setContentText(context.getString(R.string.notif_reminder_text))
            .setContentIntent(deepLinkPendingIntent(context, DEEP_LINK_SOBER))
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    context.getString(R.string.notif_action_check_in),
                    checkInPendingIntent(context),
                ).build()
            )
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        safeNotify(context, ID_REMINDER, notification)
    }

    /**
     * 记录中常驻通知：计时交给系统 chronometer 渲染（setWhen + setUsesChronometer），
     * 应用侧不需要任何周期性刷新。
     */
    fun showOngoing(context: Context, activityName: String, startTimeMillis: Long, startedAtText: String) {
        val notification = NotificationCompat.Builder(context, NotificationChannels.ONGOING)
            .setSmallIcon(R.drawable.ic_notification_locus)
            .setContentTitle(activityName)
            .setContentText(
                context.getString(R.string.notif_ongoing_started_at, startedAtText)
            )
            .setUsesChronometer(true)
            .setWhen(startTimeMillis)
            .setShowWhen(true)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setContentIntent(deepLinkPendingIntent(context, DEEP_LINK_TIMELOG))
            .addAction(
                NotificationCompat.Action.Builder(
                    0,
                    context.getString(R.string.notif_action_end),
                    endRecordingPendingIntent(context),
                ).build()
            )
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        safeNotify(context, ID_ONGOING, notification)
    }

    fun cancelOngoing(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_ONGOING)
    }

    fun cancelReminder(context: Context) {
        NotificationManagerCompat.from(context).cancel(ID_REMINDER)
    }

    /** 未授权 / 渠道被关闭时静默跳过，不抛异常 */
    private fun safeNotify(context: Context, id: Int, notification: android.app.Notification) {
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return
        runCatching { manager.notify(id, notification) }
    }

    private fun deepLinkIntent(context: Context, uri: String): Intent =
        Intent(Intent.ACTION_VIEW, Uri.parse(uri), context, MainActivity::class.java)

    private fun deepLinkPendingIntent(context: Context, uri: String): PendingIntent =
        PendingIntent.getActivity(
            context,
            uri.hashCode(),
            deepLinkIntent(context, uri),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun checkInPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, NotificationCheckInReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    private fun endRecordingPendingIntent(context: Context): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            2,
            Intent(context, NotificationEndRecordingReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
}
