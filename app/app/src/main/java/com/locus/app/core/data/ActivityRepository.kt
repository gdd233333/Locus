package com.locus.app.core.data

import com.locus.app.core.model.Activity
import com.locus.app.core.model.ActivityCategory
import kotlinx.coroutines.flow.Flow

interface ActivityRepository {

    fun getActivities(category: ActivityCategory?): Flow<List<Activity>>

    fun getRandomActivities(count: Int, category: ActivityCategory?): List<Activity>
}
