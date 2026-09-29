package com.locus.app

import android.content.Context
import com.locus.app.core.data.ActivityRepository
import com.locus.app.core.data.StreakRepository
import com.locus.app.core.data.TimeLogRepository
import com.locus.app.core.data.local.LocusDatabase
import com.locus.app.core.data.room.RoomActivityRepository
import com.locus.app.core.data.room.RoomStreakRepository
import com.locus.app.core.data.room.RoomTimeLogRepository
import com.locus.app.core.data.settings.SettingsRepository
import com.locus.app.core.data.settings.settingsDataStore

/** 手动依赖注入容器：数据库与各 Repository 单例懒加载 */
class AppContainer(context: Context) {

    private val appContext = context.applicationContext

    private val database: LocusDatabase by lazy { LocusDatabase.build(appContext) }

    val settingsRepository: SettingsRepository by lazy { SettingsRepository(appContext.settingsDataStore) }

    val streakRepository: StreakRepository by lazy {
        RoomStreakRepository(
            streakDao = database.streakDao(),
            urgeDao = database.urgeDao(),
            checkInDao = database.checkInDao(),
        )
    }

    val activityRepository: ActivityRepository by lazy {
        RoomActivityRepository(database.activityDao())
    }

    val timeLogRepository: TimeLogRepository by lazy {
        RoomTimeLogRepository(database.timeLogDao())
    }
}
