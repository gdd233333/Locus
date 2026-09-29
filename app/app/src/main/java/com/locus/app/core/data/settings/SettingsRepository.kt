package com.locus.app.core.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsRepository(private val dataStore: DataStore<Preferences>) {

    val userInitial: Flow<String> =
        dataStore.data.map { it[KEY_USER_INITIAL] ?: DEFAULT_USER_INITIAL }

    val eveningReminderEnabled: Flow<Boolean> =
        dataStore.data.map { it[KEY_EVENING_REMINDER_ENABLED] ?: DEFAULT_EVENING_REMINDER_ENABLED }

    val eveningReminderTime: Flow<String> =
        dataStore.data.map { it[KEY_EVENING_REMINDER_TIME] ?: DEFAULT_EVENING_REMINDER_TIME }

    val seedVersion: Flow<Int> =
        dataStore.data.map { it[KEY_SEED_VERSION] ?: DEFAULT_SEED_VERSION }

    suspend fun setUserInitial(value: String) {
        dataStore.edit { it[KEY_USER_INITIAL] = value }
    }

    suspend fun setEveningReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_EVENING_REMINDER_ENABLED] = enabled }
    }

    suspend fun setEveningReminderTime(value: String) {
        dataStore.edit { it[KEY_EVENING_REMINDER_TIME] = value }
    }

    suspend fun setSeedVersion(version: Int) {
        dataStore.edit { it[KEY_SEED_VERSION] = version }
    }

    companion object {
        const val DEFAULT_USER_INITIAL = "K"
        const val DEFAULT_EVENING_REMINDER_ENABLED = true
        const val DEFAULT_EVENING_REMINDER_TIME = "21:30"
        const val DEFAULT_SEED_VERSION = 1

        private val KEY_USER_INITIAL = stringPreferencesKey("user_initial")
        private val KEY_EVENING_REMINDER_ENABLED = booleanPreferencesKey("evening_reminder_enabled")
        private val KEY_EVENING_REMINDER_TIME = stringPreferencesKey("evening_reminder_time")
        private val KEY_SEED_VERSION = intPreferencesKey("seed_version")
    }
}
