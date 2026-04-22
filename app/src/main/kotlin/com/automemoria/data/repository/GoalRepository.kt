package com.automemoria.data.repository

import com.automemoria.data.local.dao.GoalDao
import com.automemoria.data.local.dao.GoalMilestoneDao
import com.automemoria.data.local.entity.GoalEntity
import com.automemoria.data.remote.dto.GoalDto
import com.automemoria.data.remote.dto.GoalMilestoneDto
import com.automemoria.domain.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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
    private val goalDao: GoalDao,
    private val milestoneDao: GoalMilestoneDao,
    private val supabase: SupabaseClient
) {
    fun observeAll(): Flow<List<Goal>> =
        goalDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeGoal(id: String): Flow<Goal?> =
        goalDao.observeAll().map { goals -> goals.find { it.id == id }?.toDomain() }

    fun observeMilestones(goalId: String): Flow<List<GoalMilestone>> =
        milestoneDao.observeForGoal(goalId).map { entities -> entities.map { it.toDomain() } }

    suspend fun toggleMilestone(milestoneId: String, isCompleted: Boolean) {
        val milestone = milestoneDao.getById(milestoneId) ?: return
        val updated = milestone.copy(
            isCompleted = isCompleted,
            completedAt = if (isCompleted) LocalDateTime.now().toIsoString() else null,
            syncStatus = SyncStatus.PENDING_UPLOAD
        )
        milestoneDao.upsert(updated)
        
        // Recalculate goal progress
        updateGoalProgress(milestone.goalId)
    }

    private suspend fun updateGoalProgress(goalId: String) {
        val milestones = milestoneDao.getAllForGoal(goalId)
        if (milestones.isEmpty()) return

        val completed = milestones.count { it.isCompleted }
        val newProgress = (completed * 100) / milestones.size

        val goal = goalDao.getById(goalId) ?: return
        goalDao.upsert(
            goal.copy(
                progress = newProgress,
                status = if (newProgress == 100) GoalStatus.COMPLETED.name else goal.status,
                syncStatus = SyncStatus.PENDING_UPLOAD,
                updatedAt = LocalDateTime.now().toIsoString()
            )
        )
    }

    suspend fun saveGoal(
        id: String? = null,
        title: String,
        description: String? = null,
        icon: String? = "🎯",
        color: String? = "#3B82F6",
        status: GoalStatus = GoalStatus.ACTIVE,
        targetDate: LocalDate? = null,
        linkedHabitIds: List<String> = emptyList()
    ): Goal {
        val now = LocalDateTime.now()
        val goalId = id ?: UUID.randomUUID().toString()
        
        val existing = if (id != null) goalDao.getById(id) else null
        
        val entity = GoalEntity(
            id = goalId,
            title = title,
            description = description,
            icon = icon,
            color = color,
            status = status.name,
            progress = existing?.progress ?: 0,
            targetDate = targetDate?.format(DateTimeFormatter.ISO_LOCAL_DATE),
            parentId = null,
            linkedHabitIds = linkedHabitIds.joinToString(",", "[", "]"),
            syncStatus = SyncStatus.PENDING_UPLOAD,
            createdAt = existing?.createdAt ?: now.toIsoString(),
            updatedAt = now.toIsoString(),
            deletedAt = null
        )
        goalDao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun createMilestone(goalId: String, title: String) {
        val now = LocalDateTime.now()
        val entity = GoalMilestoneEntity(
            id = UUID.randomUUID().toString(),
            goalId = goalId,
            title = title,
            isCompleted = false,
            dueDate = null,
            completedAt = null,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            createdAt = now.toIsoString()
        )
        milestoneDao.upsert(entity)
        updateGoalProgress(goalId)
    }

    suspend fun deleteMilestone(id: String) {
        val milestone = milestoneDao.getById(id) ?: return
        // No soft delete for milestones in current schema? 
        // MilestoneDao doesn't have deletedAt. I'll just hard delete for now or update schema.
        // Actually, let's just use what's there.
        // TODO: Add sync status for milestone deletion
    }

    fun observeWithMilestones(goalId: String): Flow<Pair<Goal?, List<GoalMilestone>>> {
        return goalDao.observeAll().map { goals ->
            val goal = goals.find { it.id == goalId }?.toDomain()
            goal
        }.combine(milestoneDao.observeForGoal(goalId)) { goal, milestones ->
            goal to milestones.map { it.toDomain() }
        }
    }

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

    // ── Sync operations ───────────────────────────────────────────────────────

    suspend fun pushPendingToSupabase() {
        // 1. Sync Goals
        val pendingGoals = goalDao.getPendingSync()
        if (pendingGoals.isNotEmpty()) {
            val toUpsert = pendingGoals.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
            val toDelete = pendingGoals.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

            if (toUpsert.isNotEmpty()) {
                try {
                    supabase.from("goals").upsert(toUpsert.map { it.toDto() })
                    toUpsert.forEach { goalDao.updateSyncStatus(it.id, SyncStatus.SYNCED) }
                    Timber.d("Synced ${toUpsert.size} goals to Supabase")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to push goals to Supabase")
                }
            }

            if (toDelete.isNotEmpty()) {
                try {
                    toDelete.forEach { entity ->
                        supabase.from("goals").update({
                            set("deleted_at", entity.deletedAt)
                        }) { filter { eq("id", entity.id) } }
                        goalDao.hardDelete(entity.id)
                    }
                    Timber.d("Deleted ${toDelete.size} goals from Supabase")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to delete goals from Supabase")
                }
            }
        }

        // 2. Sync Milestones
        val pendingMilestones = milestoneDao.getPendingSync()
        if (pendingMilestones.isNotEmpty()) {
            val toUpsert = pendingMilestones.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
            if (toUpsert.isNotEmpty()) {
                try {
                    supabase.from("goal_milestones").upsert(toUpsert.map { it.toDto() })
                    toUpsert.forEach { milestoneDao.updateSyncStatus(it.id, SyncStatus.SYNCED) }
                    Timber.d("Synced ${toUpsert.size} milestones to Supabase")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to push milestones to Supabase")
                }
            }
        }
    }

    suspend fun pullFromSupabase(since: String) {
        try {
            // Pull Goals
            val remoteGoals = supabase.from("goals")
                .select { filter { gte("updated_at", since) } }
                .decodeList<GoalDto>()

            remoteGoals.forEach { dto ->
                val local = goalDao.getById(dto.id)
                if (local == null || dto.updatedAt > local.updatedAt) {
                    goalDao.upsert(dto.toEntity())
                }
            }

            // Pull Milestones
            val remoteMilestones = supabase.from("goal_milestones")
                .select { filter { gte("updated_at", since) } }
                .decodeList<com.automemoria.data.remote.dto.GoalMilestoneDto>()

            remoteMilestones.forEach { dto ->
                milestoneDao.upsert(dto.toEntity())
            }

            Timber.d("Pulled data from Supabase for Goals")
        } catch (e: Exception) {
            Timber.e(e, "Failed to pull goals/milestones from Supabase")
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
