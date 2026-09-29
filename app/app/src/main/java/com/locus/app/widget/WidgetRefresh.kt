package com.locus.app.widget

import android.content.Context
import androidx.glance.appwidget.updateAll
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.locus.app.LocusApplication
import kotlinx.coroutines.flow.first
import java.util.concurrent.TimeUnit

/**
 * 小组件刷新调度：
 * - 15 分钟周期任务（系统下限）兜底刷新两个组件
 * - 有进行中记录时，用 1 分钟一次性任务自续期，驱动计时显示
 * - 写操作（打卡/结束）后由 ActionCallback 立即 update，不依赖 worker
 */
object WidgetRefresh {

    private const val PERIODIC_WORK = "locus_widget_periodic"
    private const val TICK_WORK = "locus_widget_recording_tick"
    private const val IMMEDIATE_WORK = "locus_widget_immediate"

    fun schedulePeriodic(context: Context) {
        val request = PeriodicWorkRequestBuilder<WidgetRefreshWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(PERIODIC_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    /**
     * 按钮动作后的兜底刷新：Glance 的 ActionCallback 里直接 updateAll 在
     * 短生命周期进程状态下可能不生效，这里再排一个立即执行的任务双保险。
     */
    fun scheduleImmediate(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>().build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(IMMEDIATE_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleTick(context: Context) {
        val request = OneTimeWorkRequestBuilder<WidgetRefreshWorker>()
            .setInitialDelay(1, TimeUnit.MINUTES)
            .build()
        WorkManager.getInstance(context)
            .enqueueUniqueWork(TICK_WORK, ExistingWorkPolicy.REPLACE, request)
    }

    fun cancelTick(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(TICK_WORK)
    }

    suspend fun refreshAll(context: Context) {
        StreakWidget().updateAll(context)
        RecordingWidget().updateAll(context)
    }
}

class WidgetRefreshWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val context = applicationContext
        WidgetRefresh.refreshAll(context)

        // 有进行中记录 → 安排下一次分钟级刷新；结束则停链
        val app = context as? LocusApplication ?: return Result.success()
        val active = runCatching {
            app.container.timeLogRepository.getActiveLog().first()
        }.getOrNull()
        if (active != null) {
            WidgetRefresh.scheduleTick(context)
        } else {
            WidgetRefresh.cancelTick(context)
        }
        return Result.success()
    }
}
