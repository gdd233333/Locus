package com.locus.app.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.updateAll
import com.locus.app.LocusApplication
import kotlinx.coroutines.flow.first

/** 小组件「打卡」：写入今日打卡后立即刷新组件 */
class CheckInAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext
        val container = (appContext as LocusApplication).container
        // 小组件没有心情输入，用中性值 3；同日重复打卡只会更新原记录
        container.streakRepository.checkIn(mood = DEFAULT_MOOD, note = null)
        StreakWidget().updateAll(appContext)
        WidgetRefresh.scheduleImmediate(appContext)
    }

    private companion object {
        const val DEFAULT_MOOD = 3
    }
}

/** 小组件「结束」：结束进行中的记录后立即刷新组件 */
class EndRecordingAction : ActionCallback {

    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val appContext = context.applicationContext
        val container = (appContext as LocusApplication).container
        container.timeLogRepository.getActiveLog().first()?.let { active ->
            container.timeLogRepository.endLog(active.id)
        }
        WidgetRefresh.cancelTick(appContext)
        RecordingWidget().updateAll(appContext)
        WidgetRefresh.scheduleImmediate(appContext)
    }
}
