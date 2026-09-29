package com.locus.app.core

import android.app.NotificationManager
import androidx.core.app.NotificationManagerCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.WorkManager
import com.locus.app.notification.EveningReminderScheduler
import com.locus.app.notification.NotificationChannels
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDateTime
import java.time.Duration
import java.time.LocalTime

/** 通知系统接线验证（仅测试代码） */
@RunWith(AndroidJUnit4::class)
class NotificationSetupTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun channels_areRegisteredWithExpectedImportance() {
        val manager = NotificationManagerCompat.from(context)

        val reminder = manager.getNotificationChannel(NotificationChannels.REMINDER)
        assertNotNull("晚间提醒渠道应存在", reminder)
        assertEquals(NotificationManager.IMPORTANCE_DEFAULT, reminder!!.importance)

        val ongoing = manager.getNotificationChannel(NotificationChannels.ONGOING)
        assertNotNull("记录中渠道应存在", ongoing)
        assertEquals(NotificationManager.IMPORTANCE_LOW, ongoing!!.importance)

        val sos = manager.getNotificationChannel(NotificationChannels.SOS)
        assertNotNull("急救渠道应存在", sos)
        assertEquals(NotificationManager.IMPORTANCE_MIN, sos!!.importance)
    }

    @Test
    fun reminder_workIsScheduled_andDelayMatchesTargetTime() = runBlocking {
        EveningReminderScheduler.schedule(context)
        val infos = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("locus_evening_reminder")
            .get()
        assertTrue("晚间提醒任务应已入队", infos.isNotEmpty())

        // 延迟算法：目标时刻在明天时，延迟 = 距明天该时刻的时长
        val delay = EveningReminderScheduler.delayUntilNext("21:30")
        val now = LocalDateTime.now()
        val target = if (LocalTime.now().isBefore(LocalTime.of(21, 30))) {
            now.toLocalDate().atTime(21, 30)
        } else {
            now.toLocalDate().plusDays(1).atTime(21, 30)
        }
        val expected = Duration.between(now, target).toMillis()
        assertTrue("延迟应接近计算值（差 < 5s）", kotlin.math.abs(delay - expected) < 5_000)

        // 漂移计算：刚过目标时刻时漂移最小
        assertTrue("漂移应为环形最小距离", EveningReminderScheduler.driftMillis("21:30") <= 12 * 60 * 60 * 1000L)
    }
}
