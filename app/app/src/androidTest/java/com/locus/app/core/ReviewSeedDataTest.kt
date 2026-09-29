package com.locus.app.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.locus.app.core.data.local.LocusDatabase
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate
import java.time.ZoneId

/**
 * 复盘页演示数据（仅测试用，不随 app 发布）：
 * 时间记录铺满近 21 天，冲动铺满近 30 天（本周 7 天固定 [2,1,0,3,1,0,2]），
 * 打卡 4 天 + 活跃 streak。可重复执行（先清理同前缀数据）。
 */
@RunWith(AndroidJUnit4::class)
class ReviewSeedDataTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun seedReviewDemoData() {
        val db = LocusDatabase.build(context)
        val raw = db.openHelper.writableDatabase
        val today = LocalDate.now()
        val zone = ZoneId.systemDefault()

        fun at(date: LocalDate, hour: Int, minute: Int): Long =
            date.atTime(hour, minute).atZone(zone).toInstant().toEpochMilli()

        raw.beginTransaction()
        try {
            raw.execSQL("DELETE FROM time_logs WHERE activity_name LIKE '演示·%'")
            raw.execSQL("DELETE FROM urge_events")
            raw.execSQL("DELETE FROM daily_check_ins")
            raw.execSQL("DELETE FROM streak_records")

            val names = listOf("演示·自习", "演示·跑步", "演示·阅读")
            (0..20).forEach { k ->
                val date = today.minusDays(k.toLong())
                val segmentCount = (k % 3) + 1
                repeat(segmentCount) { s ->
                    val startHour = 9 + s * 3
                    val durationMillis = ((k % 5) + 1) * 20 * 60_000L
                    raw.execSQL(
                        "INSERT INTO time_logs (activity_name, start_time, end_time, date) VALUES (?, ?, ?, ?)",
                        arrayOf<Any?>(
                            names[s % names.size],
                            at(date, startHour, 0),
                            at(date, startHour, 0) + durationMillis,
                            date.toEpochDay(),
                        ),
                    )
                }
            }

            val weekCounts = listOf(2, 1, 0, 3, 1, 0, 2)
            (0..29).forEach { k ->
                val date = today.minusDays(k.toLong())
                val count = if (k < 7) weekCounts[k] else (k % 4)
                repeat(count) { i ->
                    val resolved = i == 0
                    raw.execSQL(
                        "INSERT INTO urge_events (timestamp, intensity, duration_minutes, trigger_note, resolved, resolution_method) " +
                            "VALUES (?, ?, ?, NULL, ?, ?)",
                        arrayOf<Any?>(
                            at(date, 21, 30),
                            if (i % 2 == 0) "MODERATE" else "MILD",
                            if (resolved) 12 else null,
                            if (resolved) 1 else 0,
                            if (resolved) "冲浪练习" else null,
                        ),
                    )
                }
            }

            listOf(0L, 1L, 3L, 5L).forEach { k ->
                val date = today.minusDays(k)
                raw.execSQL(
                    "INSERT INTO daily_check_ins (date, mood, note, created_at) VALUES (?, 4, NULL, ?)",
                    arrayOf<Any?>(date.toEpochDay(), at(date, 22, 0)),
                )
            }
            raw.execSQL(
                "INSERT INTO streak_records (start_date, end_date, is_active) VALUES (?, NULL, 1)",
                arrayOf<Any?>(today.minusDays(5).toEpochDay()),
            )
            raw.setTransactionSuccessful()
        } finally {
            raw.endTransaction()
        }
        db.close()

        // 回读校验 + 诊断（打印真实库路径，防止写进别的包目录）
        val verify = LocusDatabase.build(context)
        val rc = verify.openHelper.readableDatabase
        fun count(sql: String): Int = rc.query(sql).use { it.moveToFirst(); it.getInt(0) }
        val logs = count("SELECT COUNT(*) FROM time_logs")
        val urges = count("SELECT COUNT(*) FROM urge_events")
        val checkIns = count("SELECT COUNT(*) FROM daily_check_ins")
        verify.close()
        android.util.Log.i(
            "ReviewSeed",
            "pkg=${context.packageName} db=${context.getDatabasePath("locus.db")} logs=$logs urges=$urges checkIns=$checkIns",
        )
        org.junit.Assert.assertTrue(
            "seed 未落库: logs=$logs urges=$urges checkIns=$checkIns",
            logs > 30 && urges > 30 && checkIns == 4,
        )
    }
}
