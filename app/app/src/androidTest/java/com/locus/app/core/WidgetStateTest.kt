package com.locus.app.core

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.locus.app.core.data.local.LocusDatabase
import com.locus.app.widget.RecordingWidget
import com.locus.app.widget.StreakWidget
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalDate

/** 小组件验证辅助（仅测试代码）：把今天改成未打卡并刷新组件 */
@RunWith(AndroidJUnit4::class)
class WidgetStateTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun clearTodayCheckIn() {
        val db = LocusDatabase.build(context)
        db.openHelper.writableDatabase.execSQL(
            "DELETE FROM daily_check_ins WHERE date = ?",
            arrayOf<Any?>(LocalDate.now().toEpochDay()),
        )
        db.close()
        runBlocking { StreakWidget().updateAll(context) }
    }

    @Test
    fun refreshWidgetsOnly() {
        runBlocking {
            StreakWidget().updateAll(context)
            RecordingWidget().updateAll(context)
        }
    }
}
