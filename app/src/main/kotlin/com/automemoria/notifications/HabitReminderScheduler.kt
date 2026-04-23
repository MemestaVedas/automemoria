package com.automemoria.notifications

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitReminderScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDaily(habitId: String, habitName: String, hour: Int = 20, minute: Int = 0) {
        val pendingIntent = reminderPendingIntent(habitId, habitName)

        val now = Calendar.getInstance()
        val trigger = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (before(now)) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            trigger.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancel(habitId: String) {
        val pendingIntent = reminderPendingIntent(habitId, habitName = null)
        alarmManager.cancel(pendingIntent)
    }

    private fun reminderPendingIntent(habitId: String, habitName: String?): PendingIntent {
        val intent = Intent(context, HabitReminderReceiver::class.java).apply {
            action = ACTION_HABIT_REMINDER
            putExtra(EXTRA_HABIT_ID, habitId)
            if (habitName != null) {
                putExtra(EXTRA_HABIT_NAME, habitName)
            }
        }

        return PendingIntent.getBroadcast(
            context,
            habitId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_HABIT_REMINDER = "com.automemoria.action.HABIT_REMINDER"
        const val ACTION_DONE = "com.automemoria.action.HABIT_DONE"
        const val ACTION_SNOOZE = "com.automemoria.action.HABIT_SNOOZE"
        const val ACTION_SKIP = "com.automemoria.action.HABIT_SKIP"
        const val EXTRA_HABIT_ID = "extra_habit_id"
        const val EXTRA_HABIT_NAME = "extra_habit_name"
    }
}
