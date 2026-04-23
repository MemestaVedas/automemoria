package com.automemoria.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.automemoria.MainActivity
import com.automemoria.R
import com.automemoria.data.repository.CalendarRepository
import com.automemoria.data.repository.HabitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@HiltWorker
class NowFocusWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val calendarRepository: CalendarRepository,
    private val habitRepository: HabitRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        ensureChannel(appContext)

        val now = LocalDateTime.now()
        val event = calendarRepository.getCurrentOrNextEvent(now)
        val habits = habitRepository.getTodayCompletionSnapshot()

        val title = when {
            event == null -> "Now Focus"
            event.startTime.isAfter(now) -> "Up Next"
            else -> "Right Now"
        }

        val body = when {
            event == null -> {
                if (habits.totalHabits == 0) {
                    "No schedule items today yet."
                } else {
                    "No schedule items. Habits: ${habits.completedHabits}/${habits.totalHabits}."
                }
            }

            event.startTime.isAfter(now) -> {
                "${event.title} at ${event.startTime.format(TIME_FORMATTER)}"
            }

            else -> {
                val end = event.endTime ?: event.startTime.plusHours(1)
                "${event.title} until ${end.format(TIME_FORMATTER)}"
            }
        }

        val openAppIntent = Intent(appContext, MainActivity::class.java)
        val contentIntent = PendingIntent.getActivity(
            appContext,
            9901,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(contentIntent)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()

        NotificationManagerCompat.from(appContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    companion object {
        private const val CHANNEL_ID = "now_focus"
        private const val CHANNEL_NAME = "Now Focus"
        private const val NOTIFICATION_ID = 9901
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        private const val WORK_NAME = "now_focus_periodic"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            val request = PeriodicWorkRequestBuilder<NowFocusWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.UPDATE,
                request
            )
        }

        private fun ensureChannel(context: Context) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Persistent at-a-glance schedule and habit accountability"
                setShowBadge(false)
            }
            manager.createNotificationChannel(channel)
        }
    }
}
