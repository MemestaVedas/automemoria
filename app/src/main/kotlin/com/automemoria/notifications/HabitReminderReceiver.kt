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

class HabitReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val habitId = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_ID) ?: return
        val habitName = intent.getStringExtra(HabitReminderScheduler.EXTRA_HABIT_NAME) ?: "Your habit"

        ensureChannel(context)

        val openAppIntent = Intent(context, MainActivity::class.java)
        val openAppPendingIntent = PendingIntent.getActivity(
            context,
            habitId.hashCode(),
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Habit reminder")
            .setContentText("Time for $habitName")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(openAppPendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(habitId.hashCode(), notification)
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
        private const val CHANNEL_ID = "habit_reminders"
    }
}
