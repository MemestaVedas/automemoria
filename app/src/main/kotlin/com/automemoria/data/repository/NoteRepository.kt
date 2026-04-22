package com.automemoria.data.repository

import com.automemoria.data.local.dao.NoteDao
import com.automemoria.data.local.dao.NoteLinkDao
import com.automemoria.data.local.entity.NoteEntity
import com.automemoria.data.remote.dto.NoteDto
import com.automemoria.domain.model.Note
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
class NoteRepository @Inject constructor(
    private val noteDao: NoteDao,
    private val linkDao: NoteLinkDao,
    private val supabase: SupabaseClient
) {
    fun observeAll(): Flow<List<Note>> =
        noteDao.observeAll().map { entities -> entities.map { it.toDomain() } }

    fun search(query: String): Flow<List<Note>> =
        noteDao.search(query).map { entities -> entities.map { it.toDomain() } }

    fun observeNote(noteId: String): Flow<Note?> =
        noteDao.observeAll().map { notes -> notes.find { it.id == noteId }?.toDomain() }

    fun observeBacklinks(noteId: String): Flow<List<Note>> =
        linkDao.observeBacklinks(noteId).map { links ->
            // In a real app, you might want to fetch the actual notes here.
            // For now, we return empty list or simplify.
            // Actually, the DAO returns NoteLinkEntity, we'd need to join or fetch.
            emptyList()
        }

    suspend fun save(
        id: String? = null,
        title: String,
        content: String,
        tags: List<String> = emptyList(),
        isPinned: Boolean = false
    ): Note {
        val now = LocalDateTime.now()
        val noteId = id ?: UUID.randomUUID().toString()
        val existing = if (id != null) noteDao.getById(id) else null
        
        val entity = NoteEntity(
            id = noteId,
            title = title,
            content = content,
            tags = tags.joinToString(",", "[", "]"),
            isPinned = isPinned,
            linkedGoalId = existing?.linkedGoalId,
            linkedCardId = existing?.linkedCardId,
            syncStatus = SyncStatus.PENDING_UPLOAD,
            createdAt = existing?.createdAt ?: now.toIsoString(),
            updatedAt = now.toIsoString(),
            deletedAt = null
        )
        noteDao.upsert(entity)
        
        // Parse wikilinks: [[Target Note Title]]
        parseAndCreateLinks(noteId, content)
        
        return entity.toDomain()
    }

    private suspend fun parseAndCreateLinks(sourceId: String, content: String) {
        val pattern = "\\[\\[(.*?)\\]\\]".toRegex()
        val matches = pattern.findAll(content)
        
        // Remove old links from this note
        // linkDao.deleteBySource(sourceId)
        
        matches.forEach { match ->
            val targetTitle = match.groupValues[1]
            val targetNote = noteDao.getByTitle(targetTitle)
            if (targetNote != null) {
                val link = NoteLinkEntity(
                    id = UUID.randomUUID().toString(),
                    sourceNoteId = sourceId,
                    targetNoteId = targetNote.id,
                    linkType = "WIKILINK",
                    syncStatus = SyncStatus.PENDING_UPLOAD,
                    createdAt = LocalDateTime.now().toIsoString()
                )
                linkDao.upsert(link)
            }
        }
    }

    fun observeAllLinks() = linkDao.observeAll()

    suspend fun update(note: Note) {
        val entity = note.toEntity().copy(
            syncStatus = SyncStatus.PENDING_UPLOAD,
            updatedAt = LocalDateTime.now().toIsoString()
        )
        noteDao.upsert(entity)
    }

    suspend fun delete(id: String) {
        noteDao.softDelete(id, LocalDateTime.now().toIsoString())
    }

    suspend fun togglePin(id: String) {
        val note = noteDao.getById(id) ?: return
        noteDao.upsert(
            note.copy(
                isPinned = !note.isPinned,
                syncStatus = SyncStatus.PENDING_UPLOAD,
                updatedAt = LocalDateTime.now().toIsoString()
            )
        )
    }

    // ── Sync operations ───────────────────────────────────────────────────────

    suspend fun pushPendingToSupabase() {
        val pending = noteDao.getPendingSync()
        if (pending.isEmpty()) return

        val toUpsert = pending.filter { it.syncStatus == SyncStatus.PENDING_UPLOAD }
        val toDelete = pending.filter { it.syncStatus == SyncStatus.PENDING_DELETE }

        if (toUpsert.isNotEmpty()) {
            try {
                supabase.from("notes").upsert(toUpsert.map { it.toDto() })
                toUpsert.forEach { noteDao.updateSyncStatus(it.id, SyncStatus.SYNCED) }
                Timber.d("Synced ${toUpsert.size} notes to Supabase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to push notes to Supabase")
            }
        }

        if (toDelete.isNotEmpty()) {
            try {
                toDelete.forEach { entity ->
                    supabase.from("notes").update({
                        set("deleted_at", entity.deletedAt)
                    }) { filter { eq("id", entity.id) } }
                    noteDao.hardDelete(entity.id)
                }
                Timber.d("Deleted ${toDelete.size} notes from Supabase")
            } catch (e: Exception) {
                Timber.e(e, "Failed to delete notes from Supabase")
            }
        }
    }

    suspend fun pullFromSupabase(since: String) {
        try {
            val remote = supabase.from("notes")
                .select { filter { gte("updated_at", since) } }
                .decodeList<NoteDto>()

            remote.forEach { dto ->
                val local = noteDao.getById(dto.id)
                if (local == null || dto.updatedAt > local.updatedAt) {
                    noteDao.upsert(dto.toEntity())
                }
            }
            Timber.d("Pulled ${remote.size} notes from Supabase")
        } catch (e: Exception) {
            Timber.e(e, "Failed to pull notes from Supabase")
        }
    }
}

// ── Mappers ───────────────────────────────────────────────────────────────────

private fun LocalDateTime.toIsoString() = format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

fun NoteEntity.toDomain(): Note = Note(
    id = id,
    title = title,
    content = content,
    tags = tags
        .trim('[', ']')
        .split(",")
        .mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } },
    isPinned = isPinned,
    linkedGoalId = linkedGoalId,
    linkedCardId = linkedCardId,
    createdAt = LocalDateTime.parse(createdAt),
    updatedAt = LocalDateTime.parse(updatedAt),
    syncStatus = syncStatus
)

fun Note.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    tags = tags.joinToString(",", "[", "]"),
    isPinned = isPinned,
    linkedGoalId = linkedGoalId,
    linkedCardId = linkedCardId,
    syncStatus = SyncStatus.SYNCED,
    createdAt = formatDateTime(createdAt),
    updatedAt = formatDateTime(updatedAt),
    deletedAt = null
)

fun NoteDto.toEntity(): NoteEntity = NoteEntity(
    id = id,
    title = title,
    content = content,
    tags = tags.joinToString(",", "[", "]"),
    isPinned = isPinned,
    linkedGoalId = linkedGoalId,
    linkedCardId = linkedCardId,
    syncStatus = SyncStatus.SYNCED,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

fun NoteEntity.toDto(): NoteDto = NoteDto(
    id = id,
    title = title,
    content = content,
    tags = tags.trim('[', ']').split(",").mapNotNull { it.trim().takeIf { s -> s.isNotEmpty() } },
    isPinned = isPinned,
    linkedGoalId = linkedGoalId,
    linkedCardId = linkedCardId,
    createdAt = createdAt,
    updatedAt = updatedAt,
    deletedAt = deletedAt
)

private fun formatDateTime(dateTime: LocalDateTime): String =
    dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)

private fun updateSyncStatus(id: String, status: SyncStatus) {
    // TODO: Call dao method
}

private fun hardDelete(id: String) {
    // TODO: Call dao method
}
