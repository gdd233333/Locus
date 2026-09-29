package com.locus.app.notification

import android.content.Context
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import com.locus.app.R

/** 通知渠道：Application.onCreate 时注册（幂等，重复注册只更新描述） */
object NotificationChannels {

    const val REMINDER = "locus_reminder"
    const val ONGOING = "locus_ongoing"
    const val SOS = "locus_sos"

    fun create(context: Context) {
        val manager = NotificationManagerCompat.from(context)

        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(REMINDER, NotificationManagerCompat.IMPORTANCE_DEFAULT)
                .setName(context.getString(R.string.channel_reminder_name))
                .setDescription(context.getString(R.string.channel_reminder_desc))
                .build()
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(ONGOING, NotificationManagerCompat.IMPORTANCE_LOW)
                .setName(context.getString(R.string.channel_ongoing_name))
                .setDescription(context.getString(R.string.channel_ongoing_desc))
                .setShowBadge(false)
                .build()
        )
        manager.createNotificationChannel(
            NotificationChannelCompat.Builder(SOS, NotificationManagerCompat.IMPORTANCE_MIN)
                .setName(context.getString(R.string.channel_sos_name))
                .setDescription(context.getString(R.string.channel_sos_desc))
                .setShowBadge(false)
                .build()
        )
    }
}
