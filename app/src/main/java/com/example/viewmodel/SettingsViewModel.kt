package com.example.viewmodel

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val sharedPrefs = application.getSharedPreferences("hisaab_prefs", Context.MODE_PRIVATE)

    private val _isDarkMode = MutableStateFlow(sharedPrefs.getBoolean("dark_mode_enabled", true))
    val isDarkMode: StateFlow<Boolean> = _isDarkMode.asStateFlow()

    private val _remindersEnabled = MutableStateFlow(sharedPrefs.getBoolean("reminders_enabled", true))
    val remindersEnabled: StateFlow<Boolean> = _remindersEnabled.asStateFlow()

    private val _reminderHour = MutableStateFlow(sharedPrefs.getInt("reminder_hour", 9))
    val reminderHour: StateFlow<Int> = _reminderHour.asStateFlow()

    private val _reminderMinute = MutableStateFlow(sharedPrefs.getInt("reminder_minute", 0))
    val reminderMinute: StateFlow<Int> = _reminderMinute.asStateFlow()

    private val _backupStatus = MutableStateFlow("Last backed up: Just now")
    val backupStatus: StateFlow<String> = _backupStatus.asStateFlow()

    private val _isBackingUp = MutableStateFlow(false)
    val isBackingUp: StateFlow<Boolean> = _isBackingUp.asStateFlow()

    fun toggleDarkMode() {
        val newVal = !_isDarkMode.value
        sharedPrefs.edit().putBoolean("dark_mode_enabled", newVal).apply()
        _isDarkMode.value = newVal
    }

    fun toggleReminders(enabled: Boolean) {
        sharedPrefs.edit().putBoolean("reminders_enabled", enabled).apply()
        _remindersEnabled.value = enabled
        ReminderScheduler.scheduleDailyAlarm(getApplication())
    }

    fun updateReminderTime(hour: Int, minute: Int) {
        sharedPrefs.edit()
            .putInt("reminder_hour", hour)
            .putInt("reminder_minute", minute)
            .apply()
        _reminderHour.value = hour
        _reminderMinute.value = minute
        ReminderScheduler.scheduleDailyAlarm(getApplication())
    }

    fun triggerBackup(onCompleted: () -> Unit) {
        _isBackingUp.value = true
        _backupStatus.value = "Backing up to secured cloud storage..."
        // Simulate background task
        _isBackingUp.value = false
        _backupStatus.value = "Last backed up: Today, 8:30 AM"
        onCompleted()
    }
}
