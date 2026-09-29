package com.locus.app.core.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.locus.app.core.data.BuiltInActivities
import com.locus.app.core.data.local.dao.ActivityDao
import com.locus.app.core.data.local.dao.CheckInDao
import com.locus.app.core.data.local.dao.StreakDao
import com.locus.app.core.data.local.dao.TimeLogDao
import com.locus.app.core.data.local.dao.UrgeDao
import com.locus.app.core.data.local.entity.ActivityEntity
import com.locus.app.core.data.local.entity.DailyCheckInEntity
import com.locus.app.core.data.local.entity.StreakRecordEntity
import com.locus.app.core.data.local.entity.TimeLogEntity
import com.locus.app.core.data.local.entity.UrgeEventEntity

@Database(
    entities = [
        StreakRecordEntity::class,
        UrgeEventEntity::class,
        DailyCheckInEntity::class,
        ActivityEntity::class,
        TimeLogEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
@TypeConverters(LocusConverters::class)
abstract class LocusDatabase : RoomDatabase() {

    abstract fun streakDao(): StreakDao

    abstract fun urgeDao(): UrgeDao

    abstract fun checkInDao(): CheckInDao

    abstract fun activityDao(): ActivityDao

    abstract fun timeLogDao(): TimeLogDao

    companion object {
        const val NAME = "locus.db"

        fun build(context: Context): LocusDatabase =
            Room.databaseBuilder(context.applicationContext, LocusDatabase::class.java, NAME)
                .addCallback(buildCallback())
                .build()

        /** 首次建库时预置 20 条内置活动（内容与 BuiltInActivities 原样一致） */
        fun buildCallback(): Callback = object : Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                seedBuiltInActivities(db)
            }
        }

        private fun seedBuiltInActivities(db: SupportSQLiteDatabase) {
            BuiltInActivities.ALL.forEach { activity ->
                db.execSQL(
                    "INSERT INTO activities " +
                        "(id, title, description, duration_minutes, category, tags, is_built_in) " +
                        "VALUES (?, ?, ?, ?, ?, ?, 1)",
                    arrayOf<Any?>(
                        activity.id,
                        activity.title,
                        activity.description,
                        activity.durationMinutes,
                        activity.category.name,
                        activity.tags.joinToString(LocusConverters.TAG_SEPARATOR),
                    ),
                )
            }
        }
    }
}
