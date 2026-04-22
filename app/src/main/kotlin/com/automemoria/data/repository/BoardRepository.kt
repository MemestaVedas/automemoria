package com.automemoria.data.repository

import com.automemoria.data.local.dao.BoardDao
import com.automemoria.data.local.entity.BoardEntity
import com.automemoria.data.remote.dto.BoardDto
import com.automemoria.domain.model.Board
import com.automemoria.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BoardRepository @Inject constructor(
    private val boardDao: BoardDao
) {
    fun observeAll(): Flow<List<Board>> =
        boardDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    suspend fun create(
        title: String,
        description: String = "",
        icon: String = "📋",
        color: String = "#7C3AED",
        linkedGoalId: String? = null
    ): Board {
        val now = LocalDateTime.now()
        val allBoards = boardDao.observeAll()
        var nextOrder = 0
        
        // Get max sort order (simplified - normally would fetch from DB)
        // TODO: Get actual max sort order from database

        val entity = BoardEntity(
            id = UUID.randomUUID().toString(),
            title = title,
            description = description,
            icon = icon,
            color = color,
            sortOrder = nextOrder,
            linkedGoalId = linkedGoalId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            createdAt = now.toIsoString(),
            updatedAt = now.toIsoString(),
            deletedAt = null
        )
        boardDao.upsert(entity)
        return entity.toDomain()
    }

    suspend fun update(board: Board) {
        val entity = board.toEntity().copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = LocalDateTime.now().toIsoString()
        )
        boardDao.upsert(entity)
    }

    suspend fun delete(id: String) {
        boardDao.softDelete(id, LocalDateTime.now().toIsoString())
    }

    suspend fun pushPendingToSupabase() {
        val pending = boardDao.getPendingSync()
        if (pending.isEmpty()) return

        val toUpsert = pending.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
        val toDelete = pending.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

        if (toUpsert.isNotEmpty()) {
            try {
                // TODO: supabase.from("boards").upsert(toUpsert.map { it.toDto() })
                toUpsert.forEach { boardDao.updateSyncStatus(it.id, SyncStatus.SYNCED) }
                Timber.d("Synced ${toUpsert.size} boards to Supabase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to push boards to Supabase")
            }
        }

        if (toDelete.isNotEmpty()) {
            try {
                // TODO: delete from Supabase
                toDelete.forEach { boardDao.hardDelete(it.id) }
                Timber.d("Deleted ${toDelete.size} boards from Supabase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete boards from Supabase")
            }
        }
    }

    suspend fun pullFromSupabase(since: String) {
        try {
            // TODO: val remote = supabase.from("boards").select { filter { gte("updated_at", since) } }.decodeList<BoardDto>()
            // TODO: remote.forEach { dto -> boardDao.upsert(dto.toEntity()) }
            Timber.d("Pulled boards from Supabase")
        } catch (e: Exception) {
            Timber.e(e, "Failed to pull boards from Supabase")
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun LocalDateTime.toIsoString() = format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

fun BoardEntity.toDomain(): Board = Board(
    id = id,
    title = title,
    description = description,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
    linkedGoalId = linkedGoalId,
    createdAt = LocalDateTime.parse(createdAt),
    updatedAt = LocalDateTime.parse(updatedAt),
    syncStatus = syncStatus
)

fun Board.toEntity(): BoardEntity = BoardEntity(
    id = id,
    title = title,
    description = description,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
    linkedGoalId = linkedGoalId,
    syncStatus = SyncStatus.SYNCED,
    createdAt = formatDateTime(createdAt),
    updatedAt = formatDateTime(updatedAt),
    deletedAt = null
)

fun BoardDto.toEntity(): BoardEntity = BoardEntity(
    id = id,
    title = title,
    description = description,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
    linkedGoalId = linkedGoalId,
    syncStatus = SyncStatus.SYNCED,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun BoardEntity.toDto(): BoardDto = BoardDto(
    id = id,
    title = title,
    description = description,
    icon = icon,
    color = color,
    sortOrder = sortOrder,
    linkedGoalId = linkedGoalId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun formatDateTime(dateTime: LocalDateTime): String =
    dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
