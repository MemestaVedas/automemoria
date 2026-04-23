package com.automemoria.data.repository

import com.automemoria.data.local.dao.HabitDao
import com.automemoria.data.local.dao.HabitLogDao
import com.automemoria.data.local.entity.HabitEntity
import com.automemoria.data.local.entity.HabitLogEntity
import com.automemoria.data.remote.dto.HabitDto
import com.automemoria.data.remote.dto.HabitLogDto
import com.automemoria.domain.model.*
import com.automemoria.notifications.HabitReminderScheduler
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HabitRepository @Inject constructor(
    private val habitDao: HabitDao,
    private val habitLogDao: HabitLogDao,
    private val goalDao: com.automemoria.data.local.dao.GoalDao,
    private val supabase: SupabaseClient,
    private val reminderScheduler: HabitReminderScheduler
) {
    // ── Observe (Room → UI) ───────────────────────────────────────────────────

    fun observeAllHabits(): Flow<List<Habit>> =
        habitDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeHabitById(habitId: String): Flow<Habit?> =
        habitDao.observeById(habitId).map { it?.toDomain() }

    fun observeActiveHabits(): Flow<List<Habit>> =
        habitDao.observeActive().map { entities -> entities.map { it.toDomain() } }

    fun observeHabitLogs(habitId: String): Flow<List<HabitLog>> =
        habitLogDao.observeForHabit(habitId).map { entities -> entities.map { it.toDomain() } }

    fun observeCompletedHabitIdsForDate(date: LocalDate): Flow<Set<String>> =
        habitLogDao.observeAllForDate(date.toString())
            .map { logs ->
                logs.filter { it.completed }
                    .map { it.habitId }
                    .toSet()
            }

    // ── Write (Room first, sync later) ────────────────────────────────────────

    suspend fun createHabit(
        name: String,
        description: String? = null,
        icon: String? = null,
        color: String? = null,
        frequency: HabitFrequency = HabitFrequency.DAILY,
        frequencyDays: List<Int> = emptyList(),
        targetStreak: Int = 0
    ): Habit {
        val now = LocalDateTime.now()
        val entity = HabitEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            icon = icon,
            color = color,
            frequency = frequency,
            frequencyDays = frequencyDays.joinToString(",", "[", "]"),
            targetStreak = targetStreak,
            isArchived = false,
            createdAt = now.toIsoString(),
            updatedAt = now.toIsoString(),
            deletedAt = null,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        habitDao.upsert(entity)
        reminderScheduler.scheduleDaily(habitId = entity.id, habitName = entity.name)
        return entity.toDomain()
    }

    suspend fun updateHabit(
        habitId: String,
        name: String,
        description: String? = null,
        icon: String? = null,
        color: String? = null,
        frequency: HabitFrequency = HabitFrequency.DAILY,
        frequencyDays: List<Int> = emptyList(),
        targetStreak: Int = 0
    ): Habit? {
        val existing = habitDao.getById(habitId) ?: return null
        val updated = existing.copy(
            name = name,
            description = description,
            icon = icon,
            color = color,
            frequency = frequency,
            frequencyDays = frequencyDays.joinToString(",", "[", "]"),
            targetStreak = targetStreak,
            updatedAt = LocalDateTime.now().toIsoString(),
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        habitDao.upsert(updated)
        reminderScheduler.scheduleDaily(habitId = updated.id, habitName = updated.name)
        return updated.toDomain()
    }

    suspend fun toggleHabitCompletion(habitId: String, date: LocalDate = LocalDate.now()) {
        val dateStr = date.toString()
        val existing = habitLogDao.getForDate(habitId, dateStr)
        if (existing != null) {
            // Toggle: flip completed state
            val updated = existing.copy(
                completed = !existing.completed,
                updatedAt = LocalDateTime.now().toIsoString(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            habitLogDao.upsert(updated)
        } else {
            // Create new log
            val log = HabitLogEntity(
                id = UUID.randomUUID().toString(),
                habitId = habitId,
                loggedDate = dateStr,
                completed = true,
                note = null,
                createdAt = LocalDateTime.now().toIsoString(),
                updatedAt = LocalDateTime.now().toIsoString(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
            habitLogDao.upsert(log)
        }
        
        // 7.2: Trigger linked goal progress update
        // We'll iterate all active goals and update progress if they are linked to this habit
        // In a real app, this should be done via a dedicated linking table or query.
        val activeGoals = goalDao.observeByStatus(GoalStatus.ACTIVE.name).first()
        activeGoals.forEach { goal ->
            if (goal.linkedHabitIds.contains(habitId)) {
                // Trigger recalculation (simplified: we'd need to fetch actual completions)
                // For now, we assume GoalRepository handles this via updateGoalProgress
                // But we don't want circular dep, so we'll just mark the goal as pending sync
                // to let the worker handle it or have a shared component.
                goalDao.upsert(goal.copy(syncStatus = SyncStatus.PENDING_UPLOAD))
            }
        }
    }

    suspend fun quickCaptureHabit(input: String) {
        val query = input.trim()
        if (query.isBlank()) return

        val activeHabits = habitDao.getActive()
        val matched = activeHabits.firstOrNull { it.name.equals(query, ignoreCase = true) }
            ?: activeHabits.firstOrNull { it.name.contains(query, ignoreCase = true) }

        if (matched != null) {
            toggleHabitCompletion(matched.id)
        } else {
            createHabit(name = query)
        }
    }

    suspend fun archiveHabit(habitId: String) {
        val existing = habitDao.getById(habitId) ?: return
        habitDao.upsert(
            existing.copy(
                isArchived = true,
                updatedAt = LocalDateTime.now().toIsoString(),
                syncStatus = SyncStatus.PENDING_UPLOAD
            )
        )
        reminderScheduler.cancel(habitId)
    }

    suspend fun deleteHabit(habitId: String) {
        reminderScheduler.cancel(habitId)
        habitDao.softDelete(habitId, LocalDateTime.now().toIsoString())
    }

    // ── Streak calculation ────────────────────────────────────────────────────

    suspend fun calculateStreak(habitId: String): Triple<Int, Int, Int> {
        val habit = habitDao.getById(habitId)?.toDomain() ?: return Triple(0, 0, 0)
        val logs = habitLogDao.getForDateRange(
            habitId,
            from = LocalDate.now().minusYears(2).toString(),
            to = LocalDate.now().toString()
        ).filter { it.completed }

        val completedDates = logs.map { LocalDate.parse(it.loggedDate) }.toSortedSet().reversed()

        var currentStreak = 0
        var longestStreak = 0
        var runningStreak = 0
        var checkDate = LocalDate.now()

        for (date in completedDates) {
            if (date == checkDate || date == checkDate.minusDays(1)) {
                runningStreak++
                longestStreak = maxOf(longestStreak, runningStreak)
                if (runningStreak == 1 && (date == LocalDate.now() || date == LocalDate.now().minusDays(1))) {
                    currentStreak = runningStreak
                } else if (date == checkDate.minusDays(1)) {
                    currentStreak = runningStreak
                }
                checkDate = date.minusDays(1)
            } else {
                runningStreak = 0
                checkDate = date.minusDays(1)
            }
        }

        return Triple(currentStreak, longestStreak, completedDates.size)
    }

    // ── Sync operations (called by SyncWorker) ────────────────────────────────

    suspend fun pushPendingToSupabase() {
        val pending = habitDao.getPendingSync()
        if (pending.isEmpty()) return

        val toUpsert = pending.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
        val toDelete = pending.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

        if (toUpsert.isNotEmpty()) {
            try {
                supabase.from("habits").upsert(toUpsert.map { it.toDto() })
                toUpsert.forEach { habitDao.updateSyncStatus(it.id, SyncStatus.SYNCED) }
                Timber.d("Synced ${toUpsert.size} habits to Supabase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to push habits to Supabase")
            }
        }

        if (toDelete.isNotEmpty()) {
            try {
                toDelete.forEach { entity ->
                    supabase.from("habits").update(
                        { set("deleted_at", entity.deletedAt) }
                    ) { filter { eq("id", entity.id) } }
                    habitDao.hardDelete(entity.id)
                }
                Timber.d("Deleted ${toDelete.size} habits from Supabase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete habits from Supabase")
            }
        }
    }

    suspend fun pullFromSupabase(since: String) {
        try {
            val remote = supabase.from("habits")
                .select { filter { gte("updated_at", since) } }
                .decodeList<HabitDto>()

            remote.forEach { dto ->
                val local = habitDao.getById(dto.id)
                if (local == null || dto.updatedAt > local.updatedAt) {
                    habitDao.upsert(dto.toEntity())
                }
            }
            Timber.d("Pulled ${remote.size} habits from Supabase")
        } catch (e: Exception) {
            Timber.e(e, "Failed to pull habits from Supabase")
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun LocalDateTime.toIsoString() = format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

fun HabitEntity.toDomain() = Habit(
    id = id,
    name = name,
    description = description,
    icon = icon,
    color = color,
    frequency = frequency,
    frequencyDays = frequencyDays
        .trim('[', ']')
        .split(",")
        .mapNotNull { it.trim().toIntOrNull() },
    targetStreak = targetStreak,
    isArchived = isArchived,
    createdAt = LocalDateTime.parse(createdAt),
    updatedAt = LocalDateTime.parse(updatedAt),
    syncStatus = syncStatus
)

fun HabitLogEntity.toDomain() = HabitLog(
    id = id,
    habitId = habitId,
    loggedDate = LocalDate.parse(loggedDate),
    completed = completed,
    note = note,
    createdAt = LocalDateTime.parse(createdAt),
    syncStatus = syncStatus
)

fun HabitEntity.toDto() = HabitDto(
    id = id,
    name = name,
    description = description,
    icon = icon,
    color = color,
    frequency = frequency.name.lowercase(),
    frequencyDays = frequencyDays.trim('[', ']').split(",").mapNotNull { it.trim().toIntOrNull() },
    targetStreak = targetStreak,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun HabitDto.toEntity() = HabitEntity(
    id = id,
    name = name,
    description = description,
    icon = icon,
    color = color,
    frequency = HabitFrequency.valueOf(frequency.uppercase()),
    frequencyDays = frequencyDays.joinToString(",", "[", "]"),
    targetStreak = targetStreak,
    isArchived = isArchived,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)
