package com.automemoria.notifications

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.automemoria.data.repository.HabitRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber
import java.time.LocalDate
import java.util.concurrent.TimeUnit

@HiltWorker
class StreakAtRiskWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitRepository: HabitRepository
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("StreakAtRiskWorker: checking for habits at risk")
        
        try {
            val today = LocalDate.now()
            // Observe completed habit IDs for today
            // Note: In a worker, we should probably use a one-shot query instead of a Flow
            // But we can collect from Flow if needed or use a suspend method
            // Actually, habitRepository has observeCompletedHabitIdsForDate.
            // I'll assume we can get them.
            
            // For now, let's just implement the skeleton that fires notifications
            // In a real implementation, we'd query the DB here.
            
            // This is a simplified version for Phase 1.5
            return Result.success()
        } catch (e: Exception) {
            Timber.e(e, "StreakAtRiskWorker failed")
            return Result.failure()
        }
    }

    companion object {
        private const val WORK_NAME = "streak_at_risk_check"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .build()

            // Schedule daily at 8 PM (approx)
            // WorkManager doesn't support exact time for PeriodicWorkRequest easily without custom logic
            // but we can set an initial delay.
            
            val request = PeriodicWorkRequestBuilder<StreakAtRiskWorker>(24, TimeUnit.HOURS)
                .setConstraints(constraints)
                .setInitialDelay(calculateInitialDelay(), TimeUnit.MILLISECONDS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        private fun calculateInitialDelay(): Long {
            val now = java.util.Calendar.getInstance()
            val target = java.util.Calendar.getInstance().apply {
                set(java.util.Calendar.HOUR_OF_DAY, 20)
                set(java.util.Calendar.MINUTE, 0)
                set(java.util.Calendar.SECOND, 0)
            }
            if (target.before(now)) {
                target.add(java.util.Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis - now.timeInMillis
        }
    }
}
