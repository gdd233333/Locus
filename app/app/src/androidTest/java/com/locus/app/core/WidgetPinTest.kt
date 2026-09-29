package com.locus.app.core

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.util.Log
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.locus.app.MainActivity
import com.locus.app.widget.RecordingWidgetReceiver
import com.locus.app.widget.StreakWidgetReceiver
import org.junit.Test
import org.junit.runner.RunWith

/** 把两个小组件钉到桌面（验证用，仅测试代码）。系统会弹确认框，需要手动/adb 点「添加」。 */
@RunWith(AndroidJUnit4::class)
class WidgetPinTest {

    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun pinStreakWidget() = pin(StreakWidgetReceiver::class.java, "streak")

    @Test
    fun pinRecordingWidget() = pin(RecordingWidgetReceiver::class.java, "recording")

    private fun pin(receiver: Class<*>, tag: String) {
        ActivityScenario.launch(MainActivity::class.java).use {
            val manager = AppWidgetManager.getInstance(context)
            val provider = ComponentName(context, receiver)
            val requested = manager.requestPinAppWidget(provider, null, null)
            Log.i("WidgetPin", "$tag pin requested = $requested")
            Thread.sleep(4000)
        }
    }
}
