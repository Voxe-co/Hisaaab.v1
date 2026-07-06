package com.example.reminder

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.Calendar

object ReminderScheduler {
    const val CHANNEL_ID = "hisaab_reminders"
    const val DAILY_ALARM_REQ_CODE = 1001
    const val SNOOZE_ALARM_REQ_CODE_BASE = 2000

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Hisaab Reminders"
            val descriptionText = "Notifications for loan due dates and overdue records"
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                enableVibration(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleDailyAlarm(context: Context) {
        val sharedPrefs = context.getSharedPreferences("hisaab_prefs", Context.MODE_PRIVATE)
        val enabled = sharedPrefs.getBoolean("reminders_enabled", true)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.HISAAB_DAILY_ALARM"
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            DAILY_ALARM_REQ_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        if (!enabled) {
            alarmManager.cancel(pendingIntent)
            return
        }

        val hour = sharedPrefs.getInt("reminder_hour", 9)
        val minute = sharedPrefs.getInt("reminder_minute", 0)

        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    cal.timeInMillis,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                cal.timeInMillis,
                pendingIntent
            )
        }
    }

    fun scheduleSnoozeAlarm(context: Context, loanId: Long, recordId: Long, interestAmount: Double, borrowerName: String, loanNote: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java).apply {
            action = "com.example.HISAAB_SNOOZE_ALARM"
            putExtra("loan_id", loanId)
            putExtra("record_id", recordId)
            putExtra("interest_amount", interestAmount)
            putExtra("borrower_name", borrowerName)
            putExtra("loan_note", loanNote)
        }
        val reqCode = SNOOZE_ALARM_REQ_CODE_BASE + recordId.toInt()
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Snooze for 2 hours
        val triggerAt = System.currentTimeMillis() + 2 * 60 * 60 * 1000

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pendingIntent
                )
            }
        } catch (e: SecurityException) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerAt,
                pendingIntent
            )
        }
    }
}
