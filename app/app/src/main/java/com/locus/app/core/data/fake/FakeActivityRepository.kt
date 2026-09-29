package com.locus.app.core.data.fake

import com.locus.app.core.data.ActivityRepository
import com.locus.app.core.data.BuiltInActivities
import com.locus.app.core.model.Activity
import com.locus.app.core.model.ActivityCategory
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** 参考实现（内存假数据），内置活动与 Room 预置数据同源（BuiltInActivities） */
class FakeActivityRepository : ActivityRepository {

    private val builtInActivities = BuiltInActivities.ALL

    override fun getActivities(category: ActivityCategory?): Flow<List<Activity>> = flowOf(
        if (category == null) builtInActivities
        else builtInActivities.filter { it.category == category }
    )

    override fun getRandomActivities(count: Int, category: ActivityCategory?): List<Activity> {
        val pool = if (category == null) builtInActivities
        else builtInActivities.filter { it.category == category }
        return pool.shuffled().take(count)
    }
}
