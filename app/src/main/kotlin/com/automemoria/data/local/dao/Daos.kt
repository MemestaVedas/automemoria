package com.automemoria.data.local.dao

import androidx.room.*
import com.automemoria.data.local.entity.*
import com.automemoria.domain.model.SyncStatus
import kotlinx.coroutines.flow.Flow

// ─── Habit DAO ────────────────────────────────────────────────────────────────

@Dao
interface HabitDao {
    @Query("SELECT * FROM habits WHERE deletedAt IS NULL ORDER BY createdAt ASC")
    fun observeAll(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    fun observeById(id: String): Flow<HabitEntity?>

    @Query("SELECT * FROM habits WHERE deletedAt IS NULL AND isArchived = 0 ORDER BY createdAt ASC")
    fun observeActive(): Flow<List<HabitEntity>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getById(id: String): HabitEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(habit: HabitEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(habits: List<HabitEntity>)

    @Query("UPDATE habits SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE habits SET deletedAt = :deletedAt, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("DELETE FROM habits WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT * FROM habits WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<HabitEntity>
}

// ─── Habit Log DAO ────────────────────────────────────────────────────────────

@Dao
interface HabitLogDao {
    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId ORDER BY loggedDate DESC")
    fun observeForHabit(habitId: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE loggedDate = :date")
    fun observeAllForDate(date: String): Flow<List<HabitLogEntity>>

    @Query("SELECT * FROM habit_logs WHERE habitId = :habitId AND loggedDate = :date LIMIT 1")
    suspend fun getForDate(habitId: String, date: String): HabitLogEntity?

    @Query("SELECT * FROM habit_logs WHERE loggedDate = :date")
    suspend fun getAllForDate(date: String): List<HabitLogEntity>

    @Query("""
        SELECT * FROM habit_logs 
        WHERE habitId = :habitId 
        AND loggedDate BETWEEN :from AND :to 
        ORDER BY loggedDate DESC
    """)
    suspend fun getForDateRange(habitId: String, from: String, to: String): List<HabitLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(log: HabitLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(logs: List<HabitLogEntity>)

    @Query("UPDATE habit_logs SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("SELECT * FROM habit_logs WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<HabitLogEntity>
}

// ─── Goal DAO ─────────────────────────────────────────────────────────────────

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE deletedAt IS NULL AND status = :status ORDER BY createdAt DESC")
    fun observeByStatus(status: String): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getById(id: String): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(goal: GoalEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(goals: List<GoalEntity>)

    @Query("UPDATE goals SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("UPDATE goals SET deletedAt = :deletedAt, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("DELETE FROM goals WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("SELECT * FROM goals WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<GoalEntity>
}

@Dao
interface GoalMilestoneDao {
    @Query("SELECT * FROM goal_milestones WHERE goalId = :goalId ORDER BY createdAt ASC")
    fun observeForGoal(goalId: String): Flow<List<GoalMilestoneEntity>>

    @Query("SELECT * FROM goal_milestones WHERE id = :id")
    suspend fun getById(id: String): GoalMilestoneEntity?

    @Query("SELECT * FROM goal_milestones WHERE goalId = :goalId")
    suspend fun getAllForGoal(goalId: String): List<GoalMilestoneEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(milestone: GoalMilestoneEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(milestones: List<GoalMilestoneEntity>)

    @Query("UPDATE goal_milestones SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("SELECT * FROM goal_milestones WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<GoalMilestoneEntity>
}

// ─── Calendar DAO ─────────────────────────────────────────────────────────────

@Dao
interface CalendarEventDao {
    @Query("SELECT * FROM calendar_events WHERE deletedAt IS NULL ORDER BY startTime ASC")
    fun observeAll(): Flow<List<CalendarEventEntity>>

    @Query("""
        SELECT * FROM calendar_events 
        WHERE deletedAt IS NULL 
        AND startTime BETWEEN :from AND :to 
        ORDER BY startTime ASC
    """)
    fun observeInRange(from: String, to: String): Flow<List<CalendarEventEntity>>

    @Query("SELECT * FROM calendar_events WHERE id = :id")
    suspend fun getById(id: String): CalendarEventEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(event: CalendarEventEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(events: List<CalendarEventEntity>)

    @Query("UPDATE calendar_events SET deletedAt = :deletedAt, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("DELETE FROM calendar_events WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("UPDATE calendar_events SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("SELECT * FROM calendar_events WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<CalendarEventEntity>
}

// ─── Board DAO ────────────────────────────────────────────────────────────────

@Dao
interface BoardDao {
    @Query("SELECT * FROM boards WHERE deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<BoardEntity>>

    @Query("SELECT * FROM boards WHERE id = :id")
    suspend fun getById(id: String): BoardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(board: BoardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(boards: List<BoardEntity>)

    @Query("UPDATE boards SET deletedAt = :deletedAt, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("DELETE FROM boards WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("UPDATE boards SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("SELECT * FROM boards WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<BoardEntity>
}

@Dao
interface BoardColumnDao {
    @Query("SELECT * FROM board_columns WHERE boardId = :boardId ORDER BY sortOrder ASC")
    fun observeForBoard(boardId: String): Flow<List<BoardColumnEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(column: BoardColumnEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(columns: List<BoardColumnEntity>)

    @Query("SELECT * FROM board_columns WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<BoardColumnEntity>
}

@Dao
interface CardDao {
    @Query("SELECT * FROM cards WHERE columnId = :columnId AND deletedAt IS NULL ORDER BY sortOrder ASC")
    fun observeForColumn(columnId: String): Flow<List<CardEntity>>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getById(id: String): CardEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(card: CardEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(cards: List<CardEntity>)

    @Query("UPDATE cards SET deletedAt = :deletedAt, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("SELECT * FROM cards WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<CardEntity>
}

// ─── Notes DAO ────────────────────────────────────────────────────────────────

@Dao
interface NoteDao {
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<NoteEntity>>

    @Query("""
        SELECT * FROM notes 
        WHERE deletedAt IS NULL 
        AND (title LIKE '%' || :query || '%' OR content LIKE '%' || :query || '%')
        ORDER BY updatedAt DESC
    """)
    fun search(query: String): Flow<List<NoteEntity>>

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: String): NoteEntity?

    @Query("SELECT * FROM notes WHERE title = :title AND deletedAt IS NULL LIMIT 1")
    suspend fun getByTitle(title: String): NoteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(note: NoteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(notes: List<NoteEntity>)

    @Query("UPDATE notes SET deletedAt = :deletedAt, syncStatus = 'PENDING_DELETE' WHERE id = :id")
    suspend fun softDelete(id: String, deletedAt: String)

    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun hardDelete(id: String)

    @Query("UPDATE notes SET syncStatus = :status WHERE id = :id")
    suspend fun updateSyncStatus(id: String, status: SyncStatus)

    @Query("SELECT * FROM notes WHERE syncStatus != 'SYNCED'")
    suspend fun getPendingSync(): List<NoteEntity>
}

@Dao
interface NoteLinkDao {
    @Query("SELECT * FROM note_links WHERE sourceNoteId = :noteId")
    fun observeOutgoing(noteId: String): Flow<List<NoteLinkEntity>>

    @Query("SELECT * FROM note_links WHERE targetNoteId = :noteId")
    fun observeBacklinks(noteId: String): Flow<List<NoteLinkEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(link: NoteLinkEntity)

    @Query("DELETE FROM note_links WHERE sourceNoteId = :sourceNoteId")
    suspend fun deleteAllForSource(sourceNoteId: String)
}
