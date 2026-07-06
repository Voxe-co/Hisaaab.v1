package com.example.reminder

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.HisaabApplication
import com.example.MainActivity
import com.example.database.MonthlyInterestRecord
import com.example.util.DummyData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as HisaabApplication
        val repository = app.repository

        if (action == "com.example.HISAAB_DAILY_ALARM") {
            // Schedule the next daily alarm
            ReminderScheduler.scheduleDailyAlarm(context)

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    // 1. Auto Status Update: Recalculate and update records for all active loans
                    repository.autoGenerateMissingRecords()

                    // 2. Scan interest records for due/overdue reminders
                    val todayMs = System.currentTimeMillis()
                    val interestRecords = repository.getAllInterestRecordsSync()
                    val activeLoans = repository.allLoans.let {
                        // Gather currently active loans
                        var list = emptyList<com.example.database.LoanEntity>()
                        val job = CoroutineScope(Dispatchers.IO).launch {
                            repository.allLoans.collect { list = it }
                        }
                        // Sleep slightly or fetch synchronously
                        Thread.sleep(200)
                        job.cancel()
                        list.filter { it.status == "ACTIVE" }
                    }

                    for (loan in activeLoans) {
                        val loanRecords = interestRecords.filter { it.loanId == loan.id }
                        for (record in loanRecords) {
                            val isPaid = record.interestPaid >= record.interestAmount
                            if (isPaid) continue

                            val daysDiff = getDaysDiff(record.dueDate, todayMs)
                            val daysOverdue = getDaysDiff(todayMs, record.dueDate)

                            var shouldRemind = false
                            var title = ""
                            var text = ""

                            when {
                                // 1 day before due date
                                daysDiff == 1 -> {
                                    shouldRemind = true
                                    title = "Upcoming Interest Payment"
                                    text = "${loan.borrowerName}'s interest of ${DummyData.formatCurrency(record.interestAmount)} is due tomorrow."
                                }
                                // On due date
                                daysDiff == 0 -> {
                                    shouldRemind = true
                                    title = "Interest Payment Due Today"
                                    text = "${loan.borrowerName}'s interest of ${DummyData.formatCurrency(record.interestAmount)} is due today."
                                }
                                // 7 days overdue
                                daysOverdue == 7 -> {
                                    shouldRemind = true
                                    title = "Overdue Interest: 7 Days"
                                    text = "${loan.borrowerName}'s interest is 7 days overdue: ${DummyData.formatCurrency(record.interestAmount)}."
                                }
                                // 30 days overdue
                                daysOverdue == 30 -> {
                                    shouldRemind = true
                                    title = "Overdue Interest: 30 Days"
                                    text = "${loan.borrowerName}'s interest is 30 days overdue: ${DummyData.formatCurrency(record.interestAmount)}."
                                }
                            }

                            if (shouldRemind) {
                                showNotification(
                                    context = context,
                                    notificationId = record.id.toInt(),
                                    title = title,
                                    text = text,
                                    borrowerId = loan.borrowerId,
                                    loanId = loan.id,
                                    recordId = record.id,
                                    interestAmount = record.interestAmount - record.interestPaid,
                                    borrowerName = loan.borrowerName,
                                    loanNote = loan.note
                                )
                            }
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                } finally {
                    pendingResult.finish()
                }
            }
        } else if (action == "com.example.HISAAB_SNOOZE_ALARM") {
            val loanId = intent.getLongExtra("loan_id", -1L)
            val recordId = intent.getLongExtra("record_id", -1L)
            val interestAmount = intent.getDoubleExtra("interest_amount", 0.0)
            val borrowerName = intent.getStringExtra("borrower_name") ?: "Borrower"
            val loanNote = intent.getStringExtra("loan_note") ?: ""
            val borrowerId = intent.getLongExtra("borrower_id", -1L)

            showNotification(
                context = context,
                notificationId = recordId.toInt(),
                title = "Reminder: Interest Payment",
                text = "$borrowerName's interest of ${DummyData.formatCurrency(interestAmount)} needs attention.",
                borrowerId = borrowerId,
                loanId = loanId,
                recordId = recordId,
                interestAmount = interestAmount,
                borrowerName = borrowerName,
                loanNote = loanNote
            )
        }
    }

    private fun showNotification(
        context: Context,
        notificationId: Int,
        title: String,
        text: String,
        borrowerId: Long,
        loanId: Long,
        recordId: Long,
        interestAmount: Double,
        borrowerName: String,
        loanNote: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Intent to open MainActivity and navigate to borrower details
        val openIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("borrower_id", borrowerId)
        }
        val openPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Intent for action: Received
        val receivedIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.ACTION_RECEIVED"
            putExtra("loan_id", loanId)
            putExtra("record_id", recordId)
            putExtra("interest_amount", interestAmount)
            putExtra("notification_id", notificationId)
        }
        val receivedPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 1,
            receivedIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        // Intent for action: Snooze / Remind Later
        val snoozeIntent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = "com.example.ACTION_SNOOZE"
            putExtra("loan_id", loanId)
            putExtra("record_id", recordId)
            putExtra("interest_amount", interestAmount)
            putExtra("borrower_name", borrowerName)
            putExtra("loan_note", loanNote)
            putExtra("notification_id", notificationId)
            putExtra("borrower_id", borrowerId)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 10 + 2,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0
        )

        val builder = NotificationCompat.Builder(context, ReminderScheduler.CHANNEL_ID)
            .setSmallIcon(com.example.R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(text)
            .setSubText(if (loanNote.isNotBlank()) loanNote else "Interest Ledger")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            // Button 1: Received
            .addAction(
                com.example.R.mipmap.ic_launcher, // Default icon
                "✔ Received",
                receivedPendingIntent
            )
            // Button 2: Remind Later
            .addAction(
                com.example.R.mipmap.ic_launcher,
                "⏰ Remind Later",
                snoozePendingIntent
            )
            // Button 3: Open Loan
            .addAction(
                com.example.R.mipmap.ic_launcher,
                "📂 Open Loan",
                openPendingIntent
            )

        notificationManager.notify(notificationId, builder.build())
    }

    private fun getDaysDiff(time1: Long, time2: Long): Int {
        val cal1 = Calendar.getInstance().apply { timeInMillis = time1; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val cal2 = Calendar.getInstance().apply { timeInMillis = time2; set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val diffMs = cal1.timeInMillis - cal2.timeInMillis
        return (diffMs / (24 * 60 * 60 * 1000)).toInt()
    }
}
