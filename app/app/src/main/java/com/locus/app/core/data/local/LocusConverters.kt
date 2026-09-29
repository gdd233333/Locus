package com.locus.app.core.data.local

import androidx.room.TypeConverter
import com.locus.app.core.model.ActivityCategory
import com.locus.app.core.model.UrgeIntensity
import java.time.Instant
import java.time.LocalDate

class LocusConverters {

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun instantToEpochMilli(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun epochMilliToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun urgeIntensityToString(value: UrgeIntensity?): String? = value?.name

    @TypeConverter
    fun stringToUrgeIntensity(value: String?): UrgeIntensity? = value?.let(UrgeIntensity::valueOf)

    @TypeConverter
    fun activityCategoryToString(value: ActivityCategory?): String? = value?.name

    @TypeConverter
    fun stringToActivityCategory(value: String?): ActivityCategory? = value?.let(ActivityCategory::valueOf)

    @TypeConverter
    fun stringListToString(value: List<String>?): String? =
        value?.joinToString(TAG_SEPARATOR)

    @TypeConverter
    fun stringToStringList(value: String?): List<String> =
        value?.takeIf { it.isNotEmpty() }?.split(TAG_SEPARATOR) ?: emptyList()

    companion object {
        internal const val TAG_SEPARATOR = "\u001F"
    }
}
