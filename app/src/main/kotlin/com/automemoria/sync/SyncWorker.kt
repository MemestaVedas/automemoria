package com.automemoria.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.automemoria.data.repository.*
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val habitRepository: HabitRepository,
    private val goalRepository: GoalRepository,
    private val noteRepository: NoteRepository,
    private val boardRepository: BoardRepository,
    private val calendarRepository: CalendarRepository,
    private val syncPreferences: SyncPreferences,
    private val supabase: SupabaseClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Timber.d("SyncWorker: starting sync")

        return try {
            val hasAuthenticatedSession = supabase.auth.currentSessionOrNull() != null
            if (!hasAuthenticatedSession) {
                Timber.d("SyncWorker: no authenticated session, skipping cloud sync")
                return Result.success()
            }

            val lastSync = syncPreferences.getLastSyncTimestamp()

            // 1. Pull remote changes into Room
            habitRepository.pullFromSupabase(since = lastSync)
            goalRepository.pullFromSupabase(since = lastSync)
            noteRepository.pullFromSupabase(since = lastSync)
            boardRepository.pullFromSupabase(since = lastSync)
            calendarRepository.pullFromSupabase(since = lastSync)

            // 2. Push local pending changes to Supabase
            habitRepository.pushPendingToSupabase()
            goalRepository.pushPendingToSupabase()
            noteRepository.pushPendingToSupabase()
            boardRepository.pushPendingToSupabase()
            calendarRepository.pushPendingToSupabase()

            // Update last sync timestamp
            syncPreferences.setLastSyncTimestamp(
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            )

            Timber.d("SyncWorker: sync complete")
            Result.success()
        } catch (e: Exception) {
            Timber.e(e, "SyncWorker: sync failed")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        const val WORK_NAME_PERIODIC = "sync_periodic"
        const val WORK_NAME_IMMEDIATE = "sync_immediate"

        /** Schedule periodic sync every 15 minutes (WorkManager minimum) */
        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 1, TimeUnit.MINUTES)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME_PERIODIC,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        /** Trigger an immediate one-shot sync (called when network reconnects) */
        fun triggerImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<SyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME_IMMEDIATE,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }
}
