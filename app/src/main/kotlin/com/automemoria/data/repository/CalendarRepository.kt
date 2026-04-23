package com.automemoria.data.repository

import com.automemoria.data.local.dao.CalendarEventDao
import com.automemoria.data.local.entity.CalendarEventEntity
import com.automemoria.data.remote.dto.CalendarEventDto
import com.automemoria.domain.model.CalendarEvent
import com.automemoria.domain.model.SyncStatus
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalendarRepository @Inject constructor(
    private val eventDao: CalendarEventDao,
    private val supabase: io.github.jan.supabase.SupabaseClient
) {
    fun observeAll(): Flow<List<CalendarEvent>> =
        eventDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeInRange(from: LocalDateTime, to: LocalDateTime): Flow<List<CalendarEvent>> =
        eventDao.observeInRange(
            from.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME),
            to.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        ).map { entities -> entities.map { it.toDomain() } }

    suspend fun create(
        title: String,
        description: String = "",
        location: String = "",
        startTime: LocalDateTime,
        endTime: LocalDateTime? = null,
        isAllDay: Boolean = false,
        color: String = "#3B82F6",
        recurrence: String? = null,
        linkedGoalId: String? = null,
        linkedHabitId: String? = null
    ): CalendarEvent {
        val now = LocalDateTime.now()
        val entity = CalendarEventEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            location = location,
            startTime = startTime.toIsoString(),
            endTime = endTime?.toIsoString(),
            isAllDay = isAllDay,
            color = color,
            recurrence = recurrence,
            linkedGoalId = linkedGoalId,
            linkedHabitId = linkedHabitId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            createdAt = now.toIsoString(),
            updatedAt = now.toIsoString(),
            deletedAt = null
        )
        eventDao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun update(event: CalendarEvent) {
        val entity = event.toEntity().copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = LocalDateTime.now().toIsoString()
        )
        eventDao.upsert(entity)
    }

    suspend fun delete(id: String) {
        eventDao.softDelete(id, LocalDateTime.now().toIsoString())
    }

    // ── Sync operations ───────────────────────────────────────────────────────

    suspend fun pushPendingToSupabase() {
        val pending = eventDao.getPendingSync()
        if (pending.isEmpty()) return

        val toUpsert = pending.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
        val toDelete = pending.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

        if (toUpsert.isNotEmpty()) {
            try {
                supabase.from("calendar_events").upsert(toUpsert.map { it.toDto() })
                toUpsert.forEach { eventDao.updateSyncStatus(it.id, SyncStatus.SYNCED) }
                Timber.d("Synced ${toUpsert.size} events to Supabase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to push events to Supabase")
            }
        }

        if (toDelete.isNotEmpty()) {
            try {
                toDelete.forEach { entity ->
                    supabase.from("calendar_events").update({
                        set("deleted_at", entity.deletedAt)
                    }) { filter { eq("id", entity.id) } }
                    eventDao.hardDelete(entity.id)
                }
                Timber.d("Deleted ${toDelete.size} events from Supabase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete events from Supabase")
            }
        }
    }

    suspend fun pullFromSupabase(since: String) {
        try {
            val remote = supabase.from("calendar_events")
                .select { filter { gte("updated_at", since) } }
                .decodeList<CalendarEventDto>()

            remote.forEach { dto ->
                val local = eventDao.getById(dto.id)
                if (local == null || dto.updatedAt > local.updatedAt) {
                    eventDao.upsert(dto.toEntity())
                }
            }
            Timber.d("Pulled ${remote.size} events from Supabase")
        } catch (e: Exception) {
            Timber.e(e, "Failed to pull events from Supabase")
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun LocalDateTime.toIsoString() = format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

fun CalendarEventEntity.toDomain(): CalendarEvent = CalendarEvent(
    id = id,
    title = title,
    description = description,
    location = location,
    startTime = LocalDateTime.parse(startTime),
    endTime = endTime?.let { LocalDateTime.parse(it) },
    isAllDay = isAllDay,
    color = color,
    recurrence = recurrence,
    linkedGoalId = linkedGoalId,
    linkedHabitId = linkedHabitId,
    createdAt = LocalDateTime.parse(createdAt),
    updatedAt = LocalDateTime.parse(updatedAt),
    syncStatus = syncStatus
)

fun CalendarEvent.toEntity(): CalendarEventEntity = CalendarEventEntity(
    id = id,
    title = title,
    description = description,
    location = location,
    startTime = formatDateTime(startTime),
    endTime = endTime?.let { formatDateTime(it) },
    isAllDay = isAllDay,
    color = color,
    recurrence = recurrence,
    linkedGoalId = linkedGoalId,
    linkedHabitId = linkedHabitId,
    syncStatus = SyncStatus.SYNCED,
    createdAt = formatDateTime(createdAt),
    updatedAt = formatDateTime(updatedAt),
    deletedAt = null
)

fun CalendarEventDto.toEntity(): CalendarEventEntity = CalendarEventEntity(
    id = id,
    title = title,
    description = description,
    location = location,
    startTime = startTime,
    endTime = endTime,
    isAllDay = isAllDay,
    color = color,
    recurrence = recurrence,
    linkedGoalId = linkedGoalId,
    linkedHabitId = linkedHabitId,
    syncStatus = SyncStatus.SYNCED,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun CalendarEventEntity.toDto(): CalendarEventDto = CalendarEventDto(
    id = id,
    title = title,
    description = description,
    location = location,
    startTime = startTime,
    endTime = endTime,
    isAllDay = isAllDay,
    color = color,
    recurrence = recurrence,
    linkedGoalId = linkedGoalId,
    linkedHabitId = linkedHabitId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun formatDateTime(dateTime: LocalDateTime): String =
    dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
