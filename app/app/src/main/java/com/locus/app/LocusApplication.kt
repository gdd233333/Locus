package com.locus.app

import android.app.Application
import com.locus.app.notification.NotificationChannels
import com.locus.app.notification.NotificationSync
import com.locus.app.widget.WidgetRefresh
import com.locus.app.widget.WidgetSync

class LocusApplication : Application() {

    val container: AppContainer by lazy { AppContainer(this) }

    override fun onCreate() {
        super.onCreate()
        // 通知：渠道注册 + 晚间提醒调度 + 记录中常驻通知
        NotificationChannels.create(this)
        NotificationSync.start(this)
        // 小组件：15 分钟周期兜底刷新 + 跟随进行中记录做分钟级刷新
        WidgetRefresh.schedulePeriodic(this)
        WidgetSync.start(this)
    }
}
