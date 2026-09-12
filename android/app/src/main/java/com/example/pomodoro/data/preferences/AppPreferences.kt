package com.example.pomodoro.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

data class TimerSettings(
    val focusDurationMin: Int = 25,
    val shortBreakMin: Int = 5,
    val longBreakMin: Int = 15,
    val longBreakInterval: Int = 4,
    val autoStartFocus: Boolean = false,
    val autoStartShortBreak: Boolean = false,
    val autoStartLongBreak: Boolean = false,
    val warningMinBefore: Int = 1,
    val keepScreenOn: Boolean = true,
    val soundEnabled: Boolean = true,
    val vibrationEnabled: Boolean = true,
    val themeMode: String = "SYSTEM" // LIGHT, DARK, SYSTEM
)

class AppPreferences(private val context: Context) {

    // Keys
    private val FOCUS_DURATION = intPreferencesKey("focus_duration_min")
    private val SHORT_BREAK = intPreferencesKey("short_break_min")
    private val LONG_BREAK = intPreferencesKey("long_break_min")
    private val LONG_BREAK_INTERVAL = intPreferencesKey("long_break_interval")
    private val AUTO_START_FOCUS = booleanPreferencesKey("auto_start_focus")
    private val AUTO_START_SHORT_BREAK = booleanPreferencesKey("auto_start_short_break")
    private val AUTO_START_LONG_BREAK = booleanPreferencesKey("auto_start_long_break")
    private val WARNING_MIN = intPreferencesKey("warning_min_before")
    private val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
    private val SOUND_ENABLED = booleanPreferencesKey("sound_enabled")
    private val VIBRATION_ENABLED = booleanPreferencesKey("vibration_enabled")
    private val THEME_MODE = stringPreferencesKey("theme_mode")
    private val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    private val SELECTED_TASK_ID = longPreferencesKey("selected_task_id")
    private val SELECTED_PROJECT_ID = longPreferencesKey("selected_project_id")
    private val REQUIRE_TASK = booleanPreferencesKey("require_task")

    val timerSettings: Flow<TimerSettings> = context.dataStore.data.map { prefs ->
        TimerSettings(
            focusDurationMin = prefs[FOCUS_DURATION] ?: 25,
            shortBreakMin = prefs[SHORT_BREAK] ?: 5,
            longBreakMin = prefs[LONG_BREAK] ?: 15,
            longBreakInterval = prefs[LONG_BREAK_INTERVAL] ?: 4,
            autoStartFocus = prefs[AUTO_START_FOCUS] ?: false,
            autoStartShortBreak = prefs[AUTO_START_SHORT_BREAK] ?: false,
            autoStartLongBreak = prefs[AUTO_START_LONG_BREAK] ?: false,
            warningMinBefore = prefs[WARNING_MIN] ?: 1,
            keepScreenOn = prefs[KEEP_SCREEN_ON] ?: true,
            soundEnabled = prefs[SOUND_ENABLED] ?: true,
            vibrationEnabled = prefs[VIBRATION_ENABLED] ?: true,
            themeMode = prefs[THEME_MODE] ?: "SYSTEM"
        )
    }

    val isOnboardingComplete: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[ONBOARDING_COMPLETE] ?: false
    }

    val selectedTaskId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_TASK_ID]
    }

    val selectedProjectId: Flow<Long?> = context.dataStore.data.map { prefs ->
        prefs[SELECTED_PROJECT_ID]
    }

    val requireTask: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[REQUIRE_TASK] ?: false
    }

    suspend fun updateTimerSettings(settings: TimerSettings) {
        context.dataStore.edit { prefs ->
            prefs[FOCUS_DURATION] = settings.focusDurationMin
            prefs[SHORT_BREAK] = settings.shortBreakMin
            prefs[LONG_BREAK] = settings.longBreakMin
            prefs[LONG_BREAK_INTERVAL] = settings.longBreakInterval
            prefs[AUTO_START_FOCUS] = settings.autoStartFocus
            prefs[AUTO_START_SHORT_BREAK] = settings.autoStartShortBreak
            prefs[AUTO_START_LONG_BREAK] = settings.autoStartLongBreak
            prefs[WARNING_MIN] = settings.warningMinBefore
            prefs[KEEP_SCREEN_ON] = settings.keepScreenOn
            prefs[SOUND_ENABLED] = settings.soundEnabled
            prefs[VIBRATION_ENABLED] = settings.vibrationEnabled
            prefs[THEME_MODE] = settings.themeMode
        }
    }

    suspend fun setOnboardingComplete() {
        context.dataStore.edit { it[ONBOARDING_COMPLETE] = true }
    }

    suspend fun setSelectedTaskId(taskId: Long?) {
        context.dataStore.edit { prefs ->
            if (taskId != null) prefs[SELECTED_TASK_ID] = taskId
            else prefs.remove(SELECTED_TASK_ID)
        }
    }

    suspend fun setSelectedProjectId(projectId: Long?) {
        context.dataStore.edit { prefs ->
            if (projectId != null) prefs[SELECTED_PROJECT_ID] = projectId
            else prefs.remove(SELECTED_PROJECT_ID)
        }
    }

    suspend fun setRequireTask(require: Boolean) {
        context.dataStore.edit { it[REQUIRE_TASK] = require }
    }

    suspend fun getTimerSettingsSnapshot(): TimerSettings {
        var settings = TimerSettings()
        context.dataStore.edit { prefs ->
            settings = TimerSettings(
                focusDurationMin = prefs[FOCUS_DURATION] ?: 25,
                shortBreakMin = prefs[SHORT_BREAK] ?: 5,
                longBreakMin = prefs[LONG_BREAK] ?: 15,
                longBreakInterval = prefs[LONG_BREAK_INTERVAL] ?: 4,
                autoStartFocus = prefs[AUTO_START_FOCUS] ?: false,
                autoStartShortBreak = prefs[AUTO_START_SHORT_BREAK] ?: false,
                autoStartLongBreak = prefs[AUTO_START_LONG_BREAK] ?: false,
                warningMinBefore = prefs[WARNING_MIN] ?: 1,
                keepScreenOn = prefs[KEEP_SCREEN_ON] ?: true,
                soundEnabled = prefs[SOUND_ENABLED] ?: true,
                vibrationEnabled = prefs[VIBRATION_ENABLED] ?: true,
                themeMode = prefs[THEME_MODE] ?: "SYSTEM"
            )
        }
        return settings
    }
}
