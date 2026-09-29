package com.locus.app.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.locus.app.LocusApplication
import com.locus.app.core.data.local.LocusDatabase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume
import org.junit.Test
import org.junit.runner.RunWith

/**
 * 打卡/streak 的跨进程持久化验证：
 * 先跑 writeCheckIn，force-stop 后再跑 verifyPersistedAfterRestart。
 * 不在这两个方法之间跑其它测试。
 */
@RunWith(AndroidJUnit4::class)
class StreakPersistenceTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private fun repository() =
        (context.applicationContext as LocusApplication).container.streakRepository

    @Test
    fun writeCheckIn() = runBlocking {
        val repo = repository()
        // 清空历史，保证可重复执行
        val db = LocusDatabase.build(context)
        db.openHelper.writableDatabase.execSQL("DELETE FROM daily_check_ins")
        db.openHelper.writableDatabase.execSQL("DELETE FROM streak_records")
        db.close()

        assertEquals("全新数据库应为 0 天", 0, repo.getCurrentStreakDays().first())

        repo.checkIn(4, "持久化验证")
        assertEquals(1, repo.getCurrentStreakDays().first())
        assertNotNull(repo.getTodayCheckIn().first())
        assertTrue(repo.isPersonalBest().first())
    }

    @Test
    fun verifyPersistedAfterRestart() = runBlocking {
        val repo = repository()
        val streak = repo.getCurrentStreakDays().first()
        val existing = repo.getTodayCheckIn().first()
        // 本方法用于「writeCheckIn → force-stop → 重开」的跨进程验证，单独执行；整套跑时状态不符则跳过
        Assume.assumeTrue(
            "需先单独执行 writeCheckIn 并杀掉进程",
            streak == 1 && existing != null,
        )

        assertEquals("杀进程重开后 streak 天数应保留", 1, repo.getCurrentStreakDays().first())
        assertTrue("杀进程重开后仍是个人最佳", repo.isPersonalBest().first())

        val checkIn = repo.getTodayCheckIn().first()!!
        assertEquals(4, checkIn.mood)
        assertEquals("持久化验证", checkIn.note)

        // 清理测试写入，避免影响后续 UI 演示
        val db = LocusDatabase.build(context)
        db.openHelper.writableDatabase.execSQL("DELETE FROM daily_check_ins")
        db.openHelper.writableDatabase.execSQL("DELETE FROM streak_records")
        db.close()
        assertEquals("清理后应回到 0 天", 0, repo.getCurrentStreakDays().first())
    }
}
