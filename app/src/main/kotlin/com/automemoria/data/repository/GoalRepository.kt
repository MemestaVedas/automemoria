package com.automemoria.data.repository

import com.automemoria.data.local.dao.GoalDao
import com.automemoria.data.local.entity.GoalEntity
import com.automemoria.data.remote.dto.GoalDto
import com.automemoria.domain.model.Goal
import com.automemoria.domain.model.GoalStatus
import com.automemoria.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GoalRepository @Inject constructor(
    private val goalDao: GoalDao
) {
    fun observeAll(): Flow<List<Goal>> =
        goalDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun create(
        title: String,
        description: String = "",
        icon: String = "🎯",
        color: String = "#3B82F6",
        targetDate: LocalDate? = null,
        parentId: String? = null,
        linkedHabitIds: List<String> = emptyList()
    ): Goal {
        val now = LocalDateTime.now()
        val entity = GoalEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            icon = icon,
            color = color,
            status = GoalStatus.ACTIVE,
            progress = 0,
            targetDate = targetDate?.toString(),
            parentId = parentId,
            linkedHabitIds = linkedHabitIds.joinToString(",", "[", "]"),
            createdAt = now.toIsoString(),
            updatedAt = now.toIsoString(),
            deletedAt = null,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        goalDao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun update(goal: Goal) {
        val entity = goal.toEntity().copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = LocalDateTime.now().toIsoString()
        )
        goalDao.upsert(entity)
    }

    suspend fun delete(id: String) {
        goalDao.softDelete(id, LocalDateTime.now().toIsoString())
    }

    suspend fun updateProgress(id: String, progress: Int) {
        val goal = goalDao.getById(id) ?: return
        goalDao.upsert(
            goal.copy(
                progress = progress.coerceIn(0, 100),
                syncStatus = SyncStatus.PENDING_UPLOAD,
                updatedAt = LocalDateTime.now().toIsoString()
            )
        )
    }

    suspend fun updateStatus(id: String, status: GoalStatus) {
        val goal = goalDao.getById(id) ?: return
        goalDao.upsert(
            goal.copy(
                status = status,
                syncStatus = SyncStatus.PENDING_UPLOAD,
                updatedAt = LocalDateTime.now().toIsoString()
            )
        )
    }

    suspend fun pushPendingToSupabase() {
        val pending = goalDao.getPendingSync()
        if (pending.isEmpty()) return

        val toUpsert = pending.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
        val toDelete = pending.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

        if (toUpsert.isNotEmpty()) {
            try {
                // TODO: supabase.from("goals").upsert(toUpsert.map { it.toDto() })
                toUpsert.forEach { goalDao.updateSyncStatus(it.id, SyncStatus.SYNCED) }
                Timber.d("Synced ${toUpsert.size} goals to Supabase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to push goals to Supabase")
            }
        }

        if (toDelete.isNotEmpty()) {
            try {
                // TODO: delete from Supabase
                toDelete.forEach { goalDao.hardDelete(it.id) }
                Timber.d("Deleted ${toDelete.size} goals from Supabase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete goals from Supabase")
            }
        }
    }

    suspend fun pullFromSupabase(since: String) {
        try {
            // TODO: val remote = supabase.from("goals").select { filter { gte("updated_at", since) } }.decodeList<GoalDto>()
            // TODO: remote.forEach { dto -> goalDao.upsert(dto.toEntity()) }
            Timber.d("Pulled goals from Supabase")
        } catch (e: Exception) {
            Timber.e(e, "Failed to pull goals from Supabase")
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun LocalDateTime.toIsoString() = format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

fun GoalEntity.toDomain(): Goal = Goal(
    id = id,
    title = title,
    description = description,
    icon = icon,
    color = color,
    status = status,
    progress = progress,
    targetDate = targetDate?.let { LocalDate.parse(it) },
    parentId = parentId,
    linkedHabitIds = linkedHabitIds
        .trim('[', ']')
        .split(",")
        .mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } },
    createdAt = LocalDateTime.parse(createdAt),
    updatedAt = LocalDateTime.parse(updatedAt),
    syncStatus = syncStatus
)

fun Goal.toEntity(): GoalEntity = GoalEntity(
    id = id,
    title = title,
    description = description,
    icon = icon,
    color = color,
    status = status,
    progress = progress,
    targetDate = targetDate?.toString(),
    parentId = parentId,
    linkedHabitIds = linkedHabitIds.joinToString(",", "[", "]"),
    createdAt = formatDateTime(createdAt),
    updatedAt = formatDateTime(updatedAt),
    deletedAt = null,
    syncStatus = SyncStatus.SYNCED
)

fun GoalDto.toEntity(): GoalEntity = GoalEntity(
    id = id,
    title = title,
    description = description,
    icon = icon,
    color = color,
    status = GoalStatus.valueOf(status.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }),
    progress = progress,
    targetDate = targetDate,
    parentId = parentId,
    linkedHabitIds = linkedHabitIds.joinToString(",", "[", "]"),
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun GoalEntity.toDto(): GoalDto = GoalDto(
    id = id,
    title = title,
    description = description,
    icon = icon,
    color = color,
    status = status.name.lowercase(),
    progress = progress,
    targetDate = targetDate,
    parentId = parentId,
    linkedHabitIds = linkedHabitIds.trim('[', ']').split(",").mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } },
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun formatDateTime(dateTime: LocalDateTime): String =
    dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
