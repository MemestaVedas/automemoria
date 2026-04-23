package com.automemoria.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.automemoria.MainActivity
import com.automemoria.R
import timber.log.Timber
import java.util.Calendar

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_ID) ?: return
        val habitName = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_NAME) ?: "Your habit"

        if (handleAction(context, intent.action, habitId, habitName)) {
            return
        }

        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val fullScreenIntent = Intent(context, ReminderAlarmActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(HabitReminderScheduler.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderScheduler.EXTRA_HABIT_NAME, habitName)
        }
        val fullScreenPendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode() + 1000,
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val donePendingIntent = actionPendingIntent(
            context = context,
            action = HabitReminderScheduler.ACTION_DONE,
            habitId = habitId,
            habitName = habitName,
            requestCodeOffset = 10
        )

        val snoozePendingIntent = actionPendingIntent(
            context = context,
            action = HabitReminderScheduler.ACTION_SNOOZE,
            habitId = habitId,
            habitName = habitName,
            requestCodeOffset = 20
        )

        val skipPendingIntent = actionPendingIntent(
            context = context,
            action = HabitReminderScheduler.ACTION_SKIP,
            habitId = habitId,
            habitName = habitName,
            requestCodeOffset = 30
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Habit reminder")
            .setContentText("Time for $habitName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(openAppPendingIntent)
            .setFullScreenIntent(fullScreenPendingIntent, true)
            .addAction(0, "Done", donePendingIntent)
            .addAction(0, "Snooze 15", snoozePendingIntent)
            .addAction(0, "Skip", skipPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(habitId.hashCode(), notification)
    }

    private fun handleAction(context: Context, action: String?, habitId: String, habitName: String): Boolean {
        when (action) {
            HabitReminderScheduler.ACTION_DONE,
            HabitReminderScheduler.ACTION_SKIP -> {
                NotificationManagerCompat.from(context).cancel(habitId.hashCode())
                return true
            }

            HabitReminderScheduler.ACTION_SNOOZE -> {
                scheduleSnooze(context, habitId, habitName)
                NotificationManagerCompat.from(context).cancel(habitId.hashCode())
                return true
            }

            else -> return false
        }
    }

    private fun scheduleSnooze(context: Context, habitId: String, habitName: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val triggerAtMillis = Calendar.getInstance().apply {
            add(Calendar.MINUTE, 15)
        }.timeInMillis

        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = HabitReminderScheduler.ACTION_HABIT_REMINDER
            putExtra(HabitReminderScheduler.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderScheduler.EXTRA_HABIT_NAME, habitName)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            habitId.hashCode() + 2000,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        runCatching {
            alarmManager.setExactAndAllowWhileIdle(
                android.app.AlarmManager.RTC_WAKEUP,
                triggerAtMillis,
                pendingIntent
            )
        }.onFailure { Timber.w(it, "Failed to schedule snooze") }
    }

    private fun actionPendingIntent(
        context: Context,
        action: String,
        habitId: String,
        habitName: String,
        requestCodeOffset: Int
    ): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            this.action = action
            putExtra(HabitReminderScheduler.EXTRA_HABIT_ID, habitId)
            putExtra(HabitReminderScheduler.EXTRA_HABIT_NAME, habitName)
        }

        return PendingIntent.getBroadcast(
            context,
            habitId.hashCode() + requestCodeOffset,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            "Habit Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily reminders for your habits"
        }

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    companion object {
        fun sendAction(
            context: Context,
            action: String,
            habitId: String,
            habitName: String
        ) {
            val intent = Intent(context, HabitReminderReceiver::class.java).apply {
                this.action = action
                putExtra(HabitReminderScheduler.EXTRA_HABIT_ID, habitId)
                putExtra(HabitReminderScheduler.EXTRA_HABIT_NAME, habitName)
            }
            context.sendBroadcast(intent)
        }

        private const val CHANNEL_ID = "habit_reminders"
    }
}
