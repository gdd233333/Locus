package com.locus.app.core.data.room

import com.locus.app.core.data.ActivityRepository
import com.locus.app.core.data.local.dao.ActivityDao
import com.locus.app.core.data.local.toModel
import com.locus.app.core.model.Activity
import com.locus.app.core.model.ActivityCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking

class RoomActivityRepository(
    private val activityDao: ActivityDao,
) : ActivityRepository {

    override fun getActivities(category: ActivityCategory?): Flow<List<Activity>> =
        activityDao.observeAll().map { list ->
            list.filter { category == null || it.category == category }.map { it.toModel() }
        }

    /**
     * 接口契约为同步方法（ViewModel 在协程里调用），活动表仅 20 行，这里用 runBlocking 读取。
     */
    override fun getRandomActivities(count: Int, category: ActivityCategory?): List<Activity> =
        runBlocking { activityDao.getByCategory(category?.name) }
            .map { it.toModel() }
            .shuffled()
            .take(count)
}
