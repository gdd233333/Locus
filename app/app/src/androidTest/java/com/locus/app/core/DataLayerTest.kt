package com.locus.app.core

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.locus.app.LocusApplication
import com.locus.app.core.data.local.LocusDatabase
import com.locus.app.core.data.room.RoomActivityRepository
import com.locus.app.core.data.room.RoomStreakRepository
import com.locus.app.core.data.room.RoomTimeLogRepository
import com.locus.app.core.data.settings.SettingsRepository
import com.locus.app.core.model.ActivityCategory
import com.locus.app.core.model.UrgeIntensity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@RunWith(AndroidJUnit4::class)
class DataLayerTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var db: LocusDatabase
    private lateinit var streakRepository: RoomStreakRepository
    private lateinit var timeLogRepository: RoomTimeLogRepository
    private lateinit var clock: MutableClock

    @Before
    fun setUp() {
        clock = MutableClock(Instant.parse("2026-09-29T13:00:00Z"), ZoneId.of("Asia/Shanghai"))
        db = Room.inMemoryDatabaseBuilder(context, LocusDatabase::class.java)
            .addCallback(LocusDatabase.buildCallback())
            .build()
        streakRepository = RoomStreakRepository(db.streakDao(), db.urgeDao(), db.checkInDao(), clock)
        timeLogRepository = RoomTimeLogRepository(db.timeLogDao(), clock)
    }

    @After
    fun tearDown() {
        db.close()
    }

    // ---- 任务 1：预置数据 ----

    @Test
    fun builtInActivities_seeded_withExactCounts() = runBlocking {
        assertEquals(20, db.activityDao().count())

        val expectedCounts = mapOf(
            ActivityCategory.EMERGENCY to 5,
            ActivityCategory.QUICK to 2,
            ActivityCategory.MEDIUM to 0,
            ActivityCategory.OUTDOOR to 2,
            ActivityCategory.CREATIVE to 3,
            ActivityCategory.INPUT to 2,
            ActivityCategory.ENVIRONMENT to 3,
            ActivityCategory.EXPRESSION to 3,
        )
        expectedCounts.forEach { (category, expected) ->
            assertEquals("$category 条数", expected, db.activityDao().getByCategory(category.name).size)
        }

        // tags（List<String> 转换器）往返
        val emergency = db.activityDao().getByCategory(ActivityCategory.EMERGENCY.name)
        assertTrue(emergency.all { it.tags.isNotEmpty() })
        assertEquals(
            listOf("冲动急救"),
            emergency.first { it.title == "做 20 个俯卧撑" }.tags,
        )
    }

    @Test
    fun realDatabase_isSeededAndServesRandomActivities() {
        val real = LocusDatabase.build(context)
        try {
            assertEquals(20, runBlocking { real.activityDao().count() })
            val repository = RoomActivityRepository(real.activityDao())
            val random = repository.getRandomActivities(3, ActivityCategory.EMERGENCY)
            assertEquals(3, random.size)
            assertTrue(random.all { it.category == ActivityCategory.EMERGENCY })
            assertEquals(20, runBlocking { repository.getActivities(null).first() }.size)
        } finally {
            real.close()
        }
    }

    // ---- 任务 2：streak / 打卡规则 ----

    @Test
    fun streak_startsAtZero_checkInCreatesStreak_andSameDayOnlyUpdates() = runBlocking {
        assertEquals(0, streakRepository.getCurrentStreakDays().first())
        assertFalse(streakRepository.isPersonalBest().first())
        assertNull(streakRepository.getTodayCheckIn().first())

        streakRepository.checkIn(3, "第一天")
        assertEquals(1, streakRepository.getCurrentStreakDays().first())
        assertTrue(streakRepository.isPersonalBest().first())
        assertEquals(3, streakRepository.getTodayCheckIn().first()!!.mood)

        // 同日重复打卡：只更新原记录，不产生新行
        streakRepository.checkIn(5, "改一下")
        assertEquals(5, streakRepository.getTodayCheckIn().first()!!.mood)
        assertEquals("同日打卡应只有 1 条记录", 1, countRows("daily_check_ins"))
        assertEquals("应只新建 1 个 streak", 1, countRows("streak_records"))
    }

    @Test
    fun streak_acrossTwoDays_incrementsByOne() = runBlocking {
        streakRepository.checkIn(3, "第一天")
        assertEquals(1, streakRepository.getCurrentStreakDays().first())

        clock.advanceDays(1)
        assertEquals("跨天未打卡，天数按公式仍连续计算", 2, streakRepository.getCurrentStreakDays().first())

        streakRepository.checkIn(4, "第二天")
        assertEquals(2, streakRepository.getCurrentStreakDays().first())
        assertTrue(streakRepository.isPersonalBest().first())
        assertEquals(2, countRows("daily_check_ins"))
        assertEquals(1, countRows("streak_records"))
    }

    @Test
    fun personalBest_comparesAgainstLongestHistory() = runBlocking {
        // 历史：一条 10 天前开始、5 天前结束的 streak（共 6 天）
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO streak_records (start_date, end_date, is_active) VALUES (?, ?, 0)",
            arrayOf<Any?>(today().minusDays(10).toEpochDay(), today().minusDays(5).toEpochDay()),
        )
        // 当前 streak 从 3 天前开始 = 4 天 < 6 天，不算个人最佳
        db.openHelper.writableDatabase.execSQL(
            "INSERT INTO streak_records (start_date, end_date, is_active) VALUES (?, NULL, 1)",
            arrayOf<Any?>(today().minusDays(3).toEpochDay()),
        )
        assertEquals(4, streakRepository.getCurrentStreakDays().first())
        assertFalse(streakRepository.isPersonalBest().first())

        // 再等两天到第 6 天 → 追平历史最佳
        clock.advanceDays(2)
        assertEquals(6, streakRepository.getCurrentStreakDays().first())
        assertTrue(streakRepository.isPersonalBest().first())
    }

    // ---- 任务 2：冲动记录与近 7 日强度 ----

    @Test
    fun urgeWave_usesStrongestPerDay_andFillsGapsWithFloor() = runBlocking {
        streakRepository.logUrge(UrgeIntensity.MILD)
        clock.advanceMinutes(1)
        streakRepository.logUrge(UrgeIntensity.STRONG)   // 今天两次 → 取最强 1.0

        clock.advanceDays(-3)
        streakRepository.logUrge(UrgeIntensity.MILD)     // 3 天前 0.33
        clock.advanceDays(2)
        streakRepository.logUrge(UrgeIntensity.MODERATE) // 1 天前 0.66
        clock.advanceDays(1)                             // 回到今天

        val wave = streakRepository.getRecentUrgeIntensities().first()
        assertEquals(7, wave.size)
        assertEquals(0.05f, wave[0], 0.001f)  // 6 天前无冲动
        assertEquals(0.05f, wave[1], 0.001f)
        assertEquals(0.05f, wave[2], 0.001f)
        assertEquals(0.33f, wave[3], 0.001f)  // 3 天前
        assertEquals(0.05f, wave[4], 0.001f)
        assertEquals(0.66f, wave[5], 0.001f)  // 1 天前
        assertEquals(1.0f, wave[6], 0.001f)   // 今天取最强

        assertEquals(2, streakRepository.getTodayUrgeCount().first())
        assertEquals(UrgeIntensity.STRONG, streakRepository.getLatestUrge().first()!!.intensity)

        streakRepository.resolveLatestUrge(12, "冲浪练习")
        val latest = streakRepository.getLatestUrge().first()!!
        assertTrue(latest.resolved)
        assertEquals(12, latest.durationMinutes)
        assertEquals("冲浪练习", latest.resolutionMethod)
    }

    // ---- 任务 2：时间记录 ----

    @Test
    fun timeLog_startEndDelete_andDefensiveEnd() = runBlocking {
        assertNull(timeLogRepository.getActiveLog().first())

        val first = timeLogRepository.startLog("刷手机")
        assertEquals("刷手机", first.activityName)
        assertEquals(today(), first.date)
        assertNotNull(timeLogRepository.getActiveLog().first())

        // 防御：开始新记录前旧记录被自动结束
        val second = timeLogRepository.startLog("学习")
        assertEquals(second.id, timeLogRepository.getActiveLog().first()!!.id)
        val afterSecond = timeLogRepository.getLogsForDate(today()).first()
        assertEquals(2, afterSecond.size)
        assertNotNull(afterSecond.first { it.id == first.id }.endTime)

        timeLogRepository.endLog(second.id)
        assertNull(timeLogRepository.getActiveLog().first())
        assertNotNull(
            timeLogRepository.getLogsForDate(today()).first().first { it.id == second.id }.endTime
        )

        // 空名防御
        val third = timeLogRepository.startLog("   ")
        assertEquals("未命名", third.activityName)

        // 常用活动聚合（GROUP BY activity_name）
        val frequent = timeLogRepository.getFrequentActivities().first()
        assertTrue(frequent.any { it.name == "刷手机" })
        assertTrue(frequent.any { it.name == "学习" })

        timeLogRepository.deleteLog(third.id)
        assertEquals(2, timeLogRepository.getLogsForDate(today()).first().size)
    }

    @Test
    fun timeLog_crossMidnight_belongsToStartDate() = runBlocking {
        val log = timeLogRepository.startLog("跨天任务")
        val startDate = today()
        clock.advanceDays(1)
        timeLogRepository.endLog(log.id)

        assertNull(timeLogRepository.getActiveLog().first())
        assertTrue(timeLogRepository.getLogsForDate(startDate).first().any { it.id == log.id })
        assertTrue(timeLogRepository.getLogsForDate(today()).first().isEmpty())
    }

    // ---- 任务 4：DataStore 设置层 ----

    @Test
    fun settings_freshStore_hasDefaults_andRoundTrips() = runBlocking {
        val file = File(context.cacheDir, "test_settings_${System.currentTimeMillis()}.preferences_pb")
        val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        val settings = SettingsRepository(
            PreferenceDataStoreFactory.create(scope = scope, produceFile = { file })
        )

        assertEquals("K", settings.userInitial.first())
        assertTrue(settings.eveningReminderEnabled.first())
        assertEquals("21:30", settings.eveningReminderTime.first())
        assertEquals(1, settings.seedVersion.first())

        settings.setUserInitial("L")
        settings.setEveningReminderEnabled(false)
        settings.setEveningReminderTime("22:15")
        settings.setSeedVersion(2)

        assertEquals("L", settings.userInitial.first())
        assertFalse(settings.eveningReminderEnabled.first())
        assertEquals("22:15", settings.eveningReminderTime.first())
        assertEquals(2, settings.seedVersion.first())
        assertTrue("DataStore 文件应已落盘", file.exists())
        scope.cancel()
    }

    @Test
    fun settings_realAppRepository_roundTrips() = runBlocking {
        val app = context.applicationContext as LocusApplication
        val settings = app.container.settingsRepository

        val original = settings.userInitial.first()
        settings.setUserInitial("测试")
        assertEquals("测试", settings.userInitial.first())
        settings.setUserInitial(original) // 还原，避免影响后续 UI 验证
        assertEquals(original, settings.userInitial.first())

        assertTrue(
            "真实 DataStore 文件应存在",
            File(context.filesDir, "datastore/settings.preferences_pb").exists(),
        )
    }

    // ---- helpers ----

    private fun today(): LocalDate = LocalDate.now(clock)

    private fun countRows(table: String): Int =
        db.openHelper.readableDatabase
            .query("SELECT COUNT(*) FROM $table")
            .use { cursor ->
                cursor.moveToFirst()
                cursor.getInt(0)
            }

    private class MutableClock(
        private var current: Instant,
        private val zone: ZoneId,
    ) : Clock() {
        override fun getZone(): ZoneId = zone

        override fun withZone(zone: ZoneId): Clock = MutableClock(current, zone)

        override fun instant(): Instant = current

        fun advanceDays(days: Long) {
            current = current.plus(days, ChronoUnit.DAYS)
        }

        fun advanceMinutes(minutes: Long) {
            current = current.plus(minutes, ChronoUnit.MINUTES)
        }
    }
}
