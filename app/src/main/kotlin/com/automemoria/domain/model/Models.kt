package com.automemoria.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

// ─── Sync ────────────────────────────────────────────────────────────────────

enum class SyncStatus { SYNCED, PENDING_UPLOAD, PENDING_DELETE }

// ─── Habit ───────────────────────────────────────────────────────────────────

data class Habit(
    val id: String,
    val name: String,
    val description: String?,
    val icon: String?,
    val color: String?,
    val frequency: HabitFrequency,
    val frequencyDays: List<Int>,   // day-of-week: 1=Mon..7=Sun (empty if not weekly)
    val targetStreak: Int,
    val isArchived: Boolean,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val syncStatus: SyncStatus
)

enum class HabitFrequency { DAILY, WEEKLY, CUSTOM }

data class HabitLog(
    val id: String,
    val habitId: String,
    val loggedDate: LocalDate,
    val completed: Boolean,
    val note: String?,
    val createdAt: LocalDateTime,
    val syncStatus: SyncStatus
)

data class HabitWithStreak(
    val habit: Habit,
    val currentStreak: Int,
    val longestStreak: Int,
    val totalCompletions: Int,
    val completedToday: Boolean
)

// ─── Goal ────────────────────────────────────────────────────────────────────

data class Goal(
    val id: String,
    val title: String,
    val description: String?,
    val icon: String?,
    val color: String?,
    val status: GoalStatus,
    val progress: Int,       // 0–100
    val targetDate: LocalDate?,
    val parentId: String?,
    val linkedHabitIds: List<String>,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val syncStatus: SyncStatus
)

enum class GoalStatus { ACTIVE, COMPLETED, PAUSED, ABANDONED }

data class GoalMilestone(
    val id: String,
    val goalId: String,
    val title: String,
    val isCompleted: Boolean,
    val dueDate: LocalDate?,
    val completedAt: LocalDateTime?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val syncStatus: SyncStatus
)

// ─── Calendar ────────────────────────────────────────────────────────────────

data class CalendarEvent(
    val id: String,
    val title: String,
    val description: String?,
    val location: String?,
    val startTime: LocalDateTime,
    val endTime: LocalDateTime?,
    val isAllDay: Boolean,
    val color: String?,
    val recurrence: String?,   // JSON rrule string
    val linkedGoalId: String?,
    val linkedHabitId: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val syncStatus: SyncStatus
)

// ─── Board / Kanban ───────────────────────────────────────────────────────────

data class Board(
    val id: String,
    val title: String,
    val description: String?,
    val color: String?,
    val icon: String?,
    val sortOrder: Int,
    val linkedGoalId: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val syncStatus: SyncStatus
)

data class BoardColumn(
    val id: String,
    val boardId: String,
    val title: String,
    val color: String?,
    val sortOrder: Int,
    val createdAt: LocalDateTime,
    val syncStatus: SyncStatus
)

enum class CardPriority { NONE, LOW, MEDIUM, HIGH, URGENT }

data class Card(
    val id: String,
    val columnId: String,
    val title: String,
    val description: String?,
    val priority: CardPriority,
    val dueDate: LocalDate?,
    val labels: List<String>,
    val sortOrder: Int,
    val isCompleted: Boolean,
    val linkedNoteId: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val syncStatus: SyncStatus
)

// ─── Notes ────────────────────────────────────────────────────────────────────

data class Note(
    val id: String,
    val title: String,
    val content: String?,
    val tags: List<String>,
    val isPinned: Boolean,
    val linkedGoalId: String?,
    val linkedCardId: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val syncStatus: SyncStatus
)

data class NoteLink(
    val id: String,
    val sourceNoteId: String,
    val targetNoteId: String,
    val createdAt: LocalDateTime
)
