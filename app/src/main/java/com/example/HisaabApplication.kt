package com.example

import android.app.Application
import com.example.database.AppDatabase
import com.example.repository.LoanRepository
import com.example.reminder.ReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class HisaabApplication : Application() {
    val database by lazy { AppDatabase.getDatabase(this) }
    val repository by lazy { LoanRepository(database.loanDao()) }

    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        // Initialize Notification channel and schedule alarms on startup
        ReminderScheduler.createNotificationChannel(this)
        ReminderScheduler.scheduleDailyAlarm(this)

        applicationScope.launch {
            repository.autoGenerateMissingRecords()
        }
    }
}
