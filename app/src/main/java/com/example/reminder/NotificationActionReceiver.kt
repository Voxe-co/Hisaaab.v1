package com.example.reminder

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.HisaabApplication
import com.example.database.PaymentEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val app = context.applicationContext as HisaabApplication
        val repository = app.repository
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val notificationId = intent.getIntExtra("notification_id", -1)
        if (notificationId != -1) {
            notificationManager.cancel(notificationId)
        }

        when (action) {
            "com.example.ACTION_RECEIVED" -> {
                val loanId = intent.getLongExtra("loan_id", -1L)
                val amount = intent.getDoubleExtra("interest_amount", 0.0)

                if (loanId != -1L && amount > 0.0) {
                    val pendingResult = goAsync()
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val payment = PaymentEntity(
                                loanId = loanId,
                                paymentType = "Interest",
                                interestPaid = amount,
                                principalPaid = 0.0,
                                totalPaid = amount,
                                paymentDate = System.currentTimeMillis(),
                                note = "Interest payment received via Quick Action"
                            )
                            repository.insertPayment(payment)
                        } catch (e: Exception) {
                            e.printStackTrace()
                        } finally {
                            pendingResult.finish()
                        }
                    }
                }
            }

            "com.example.ACTION_SNOOZE" -> {
                val loanId = intent.getLongExtra("loan_id", -1L)
                val recordId = intent.getLongExtra("record_id", -1L)
                val amount = intent.getDoubleExtra("interest_amount", 0.0)
                val borrowerName = intent.getStringExtra("borrower_name") ?: "Borrower"
                val loanNote = intent.getStringExtra("loan_note") ?: ""
                val borrowerId = intent.getLongExtra("borrower_id", -1L)

                if (recordId != -1L) {
                    ReminderScheduler.scheduleSnoozeAlarm(
                        context = context,
                        loanId = loanId,
                        recordId = recordId,
                        interestAmount = amount,
                        borrowerName = borrowerName,
                        loanNote = loanNote
                    )
                }
            }
        }
    }
}
