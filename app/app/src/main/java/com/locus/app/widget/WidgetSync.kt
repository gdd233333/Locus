package com.locus.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import com.locus.app.LocusApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * App 进程存活期间的同步器：跟随 time_logs 的进行中记录变化，
 * 立即刷新 RecordingWidget 并维护分钟级刷新链。
 * （Room Flow 不能在 Glance 里直接 collect，这里是唯一的长驻观察点）
 */
object WidgetSync {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(context: Context) {
        val app = context.applicationContext as? LocusApplication ?: return
        scope.launch {
            runCatching {
                WidgetRefresh.refreshAll(app)
                app.container.timeLogRepository.getActiveLog().collect { active ->
                    RecordingWidget().updateAll(app)
                    if (active != null) {
                        WidgetRefresh.scheduleTick(app)
                    } else {
                        WidgetRefresh.cancelTick(app)
                    }
                }
            }
        }
    }
}
