package com.automemoria.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.automemoria.domain.model.CardPriority
import com.automemoria.domain.model.GoalStatus
import com.automemoria.domain.model.HabitFrequency
import com.automemoria.domain.model.SyncStatus

// ─── Converters are in TypeConverters.kt ─────────────────────────────────────

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey val id: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val color: String?,
    val frequency: HabitFrequency,
    val frequencyDays: String,      // JSON array of ints, e.g. "[1,2,3]"
    val targetStreak: Int,
    val isArchived: Boolean,
    val createdAt: String,          // ISO-8601
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: SyncStatus
)

@Entity(
    tableName = "habit_logs",
    indices = [Index(value = ["habitId", "loggedDate"], unique = true)],
    foreignKeys = [ForeignKey(
        entity = HabitEntity::class,
        parentColumns = ["id"],
        childColumns = ["habitId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class HabitLogEntity(
    @PrimaryKey val id: String,
    val habitId: String,
    val loggedDate: String,         // ISO date "2025-04-20"
    val completed: Boolean,
    val note: String?,
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: SyncStatus
)

@Entity(tableName = "goals")
data class GoalEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val icon: String?,
    val color: String?,
    val status: GoalStatus,
    val progress: Int,
    val targetDate: String?,
    val parentId: String?,
    val linkedHabitIds: String,     // JSON array of UUIDs
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: SyncStatus
)

@Entity(
    tableName = "goal_milestones",
    foreignKeys = [ForeignKey(
        entity = GoalEntity::class,
        parentColumns = ["id"],
        childColumns = ["goalId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class GoalMilestoneEntity(
    @PrimaryKey val id: String,
    val goalId: String,
    val title: String,
    val isCompleted: Boolean,
    val dueDate: String?,
    val completedAt: String?,
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: SyncStatus
)

@Entity(tableName = "calendar_events")
data class CalendarEventEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startTime: String,
    val endTime: String?,
    val isAllDay: Boolean,
    val color: String?,
    val recurrence: String?,
    val linkedGoalId: String?,
    val linkedHabitId: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: SyncStatus
)

@Entity(tableName = "boards")
data class BoardEntity(
    @PrimaryKey val id: String,
    val title: String,
    val description: String?,
    val color: String?,
    val icon: String?,
    val sortOrder: Int,
    val linkedGoalId: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: SyncStatus
)

@Entity(
    tableName = "board_columns",
    foreignKeys = [ForeignKey(
        entity = BoardEntity::class,
        parentColumns = ["id"],
        childColumns = ["boardId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BoardColumnEntity(
    @PrimaryKey val id: String,
    val boardId: String,
    val title: String,
    val color: String?,
    val sortOrder: Int,
    val createdAt: String,
    val updatedAt: String,
    val syncStatus: SyncStatus
)

@Entity(
    tableName = "cards",
    foreignKeys = [ForeignKey(
        entity = BoardColumnEntity::class,
        parentColumns = ["id"],
        childColumns = ["columnId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class CardEntity(
    @PrimaryKey val id: String,
    val columnId: String,
    val title: String,
    val description: String?,
    val priority: CardPriority,
    val dueDate: String?,
    val labels: String,             // JSON array of strings
    val sortOrder: Int,
    val isCompleted: Boolean,
    val linkedNoteId: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: SyncStatus
)

@Entity(tableName = "notes")
data class NoteEntity(
    @PrimaryKey val id: String,
    val title: String,
    val content: String?,
    val tags: String,               // JSON array of strings
    val isPinned: Boolean,
    val linkedGoalId: String?,
    val linkedCardId: String?,
    val createdAt: String,
    val updatedAt: String,
    val deletedAt: String?,
    val syncStatus: SyncStatus
)

@Entity(
    tableName = "note_links",
    indices = [Index(value = ["sourceNoteId", "targetNoteId"], unique = true)]
)
data class NoteLinkEntity(
    @PrimaryKey val id: String,
    val sourceNoteId: String,
    val targetNoteId: String,
    val createdAt: String
)
