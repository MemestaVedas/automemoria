package com.automemoria.data.repository

import com.automemoria.data.local.dao.BoardDao
import com.automemoria.data.local.dao.BoardColumnDao
import com.automemoria.data.local.dao.CardDao
import com.automemoria.data.local.entity.*
import com.automemoria.data.remote.dto.BoardDto
import com.automemoria.domain.model.*
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
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
class BoardRepository @Inject constructor(
    private val boardDao: BoardDao,
    private val columnDao: BoardColumnDao,
    private val cardDao: CardDao,
    private val supabase: io.github.jan.supabase.SupabaseClient
) {
    fun observeAll(): Flow<List<Board>> =
        boardDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun observeColumns(boardId: String): Flow<List<BoardColumn>> =
        columnDao.observeForBoard(boardId).map { entities -> entities.map { it.toDomain() } }

    fun observeCards(columnId: String): Flow<List<Card>> =
        cardDao.observeForColumn(columnId).map { entities -> entities.map { it.toDomain() } }

    suspend fun create(
        title: String,
        description: String = "",
        icon: String = "📋",
        color: String = "#7C3AED",
        linkedGoalId: String? = null
    ): Board {
        val now = LocalDateTime.now()
        // Simple sort order logic
        val nextOrder = 0 // In real app, fetch max sort order

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

    // ── Sync operations ───────────────────────────────────────────────────────

    suspend fun pushPendingToSupabase() {
        // 1. Boards
        val pendingBoards = boardDao.getPendingSync()
        if (pendingBoards.isNotEmpty()) {
            val toUpsert = pendingBoards.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
            val toDelete = pendingBoards.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

            if (toUpsert.isNotEmpty()) {
                try {
                    supabase.from("boards").upsert(toUpsert.map { it.toDto() })
                    toUpsert.forEach { boardDao.updateSyncStatus(it.id, SyncStatus.SYNCED) }
                    Timber.d("Synced ${toUpsert.size} boards to Supabase")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to push boards to Supabase")
                }
            }

            if (toDelete.isNotEmpty()) {
                try {
                    toDelete.forEach { entity ->
                        supabase.from("boards").update({
                            set("deleted_at", entity.deletedAt)
                        }) { filter { eq("id", entity.id) } }
                        boardDao.hardDelete(entity.id)
                    }
                    Timber.d("Deleted ${toDelete.size} boards from Supabase")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to delete boards from Supabase")
                }
            }
        }

        // 2. Columns
        val pendingColumns = columnDao.getPendingSync()
        if (pendingColumns.isNotEmpty()) {
            val toUpsert = pendingColumns.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
            if (toUpsert.isNotEmpty()) {
                try {
                    supabase.from("board_columns").upsert(toUpsert.map { it.toDto() })
                    // columnDao.updateSyncStatus(it.id, SyncStatus.SYNCED) // Add this to DAO if needed
                    Timber.d("Synced ${toUpsert.size} columns to Supabase")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to push columns to Supabase")
                }
            }
        }

        // 3. Cards
        val pendingCards = cardDao.getPendingSync()
        if (pendingCards.isNotEmpty()) {
            val toUpsert = pendingCards.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
            if (toUpsert.isNotEmpty()) {
                try {
                    supabase.from("cards").upsert(toUpsert.map { it.toDto() })
                    Timber.d("Synced ${toUpsert.size} cards to Supabase")
                } catch (e: Exception) {
                    Timber.e(e, "Failed to push cards to Supabase")
                }
            }
        }
    }

    suspend fun pullFromSupabase(since: String) {
        try {
            // Pull Boards
            val remoteBoards = supabase.from("boards")
                .select { filter { gte("updated_at", since) } }
                .decodeList<BoardDto>()
            remoteBoards.forEach { dto ->
                val local = boardDao.getById(dto.id)
                if (local == null || dto.updatedAt > local.updatedAt) {
                    boardDao.upsert(dto.toEntity())
                }
            }

            // Pull Columns
            val remoteColumns = supabase.from("board_columns")
                .select { filter { gte("updated_at", since) } }
                .decodeList<com.automemoria.data.remote.dto.BoardColumnDto>()
            remoteColumns.forEach { dto ->
                columnDao.upsert(dto.toEntity())
            }

            // Pull Cards
            val remoteCards = supabase.from("cards")
                .select { filter { gte("updated_at", since) } }
                .decodeList<com.automemoria.data.remote.dto.CardDto>()
            remoteCards.forEach { dto ->
                cardDao.upsert(dto.toEntity())
            }

            Timber.d("Pulled Kanban data from Supabase")
        } catch (e: Exception) {
            Timber.e(e, "Failed to pull Kanban data from Supabase")
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

// ─── BoardColumn Mappers ──────────────────────────────────────────────────────

fun BoardColumnEntity.toDomain() = BoardColumn(
    id = id,
    boardId = boardId,
    title = title,
    color = color,
    sortOrder = sortOrder,
    createdAt = LocalDateTime.parse(createdAt),
    syncStatus = syncStatus
)

fun BoardColumn.toEntity() = BoardColumnEntity(
    id = id,
    boardId = boardId,
    title = title,
    color = color,
    sortOrder = sortOrder,
    createdAt = formatDateTime(createdAt),
    updatedAt = formatDateTime(createdAt), // Simplified
    syncStatus = syncStatus
)

fun com.automemoria.data.remote.dto.BoardColumnDto.toEntity() = BoardColumnEntity(
    id = id,
    boardId = boardId,
    title = title,
    color = color,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt,
    syncStatus = SyncStatus.SYNCED
)

fun BoardColumnEntity.toDto() = com.automemoria.data.remote.dto.BoardColumnDto(
    id = id,
    boardId = boardId,
    title = title,
    color = color,
    sortOrder = sortOrder,
    createdAt = createdAt,
    updatedAt = updatedAt
)

// ─── Card Mappers ───────────────────────────────────────────────────────────

fun CardEntity.toDomain() = Card(
    id = id,
    columnId = columnId,
    title = title,
    description = description,
    priority = priority,
    dueDate = dueDate?.let { LocalDate.parse(it) },
    labels = labels.trim('[', ']').split(",").mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } },
    sortOrder = sortOrder,
    isCompleted = isCompleted,
    linkedNoteId = linkedNoteId,
    createdAt = LocalDateTime.parse(createdAt),
    updatedAt = LocalDateTime.parse(updatedAt),
    syncStatus = syncStatus
)

fun Card.toEntity() = CardEntity(
    id = id,
    columnId = columnId,
    title = title,
    description = description,
    priority = priority,
    dueDate = dueDate?.toString(),
    labels = labels.joinToString(",", "[", "]"),
    sortOrder = sortOrder,
    isCompleted = isCompleted,
    linkedNoteId = linkedNoteId,
    createdAt = formatDateTime(createdAt),
    updatedAt = formatDateTime(updatedAt),
    deletedAt = null,
    syncStatus = syncStatus
)

fun com.automemoria.data.remote.dto.CardDto.toEntity() = CardEntity(
    id = id,
    columnId = columnId,
    title = title,
    description = description,
    priority = CardPriority.valueOf(priority.uppercase()),
    dueDate = dueDate,
    labels = labels.joinToString(",", "[", "]"),
    sortOrder = sortOrder,
    isCompleted = isCompleted,
    linkedNoteId = linkedNoteId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt,
    syncStatus = SyncStatus.SYNCED
)

fun CardEntity.toDto() = com.automemoria.data.remote.dto.CardDto(
    id = id,
    columnId = columnId,
    title = title,
    description = description,
    priority = priority.name.lowercase(),
    dueDate = dueDate,
    labels = labels.trim('[', ']').split(",").mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } },
    sortOrder = sortOrder,
    isCompleted = isCompleted,
    linkedNoteId = linkedNoteId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun formatDateTime(dateTime: LocalDateTime): String =
    dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
