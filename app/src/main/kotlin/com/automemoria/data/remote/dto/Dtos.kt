package com.automemoria.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// All DTOs mirror Supabase Postgres column names (snake_case).
// They are intentionally separate from Room entities so schema changes
// on either side don't force changes to the other.

@Serializable
data class HabitDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val frequency: String,
    @SerialName("frequency_days") val frequencyDays: List<Int> = emptyList(),
    @SerialName("target_streak") val targetStreak: Int = 0,
    @SerialName("is_archived") val isArchived: Boolean = false,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class HabitLogDto(
    val id: String,
    @SerialName("habit_id") val habitId: String,
    @SerialName("logged_date") val loggedDate: String,
    val completed: Boolean = true,
    val note: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class GoalDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val icon: String? = null,
    val color: String? = null,
    val status: String = "active",
    val progress: Int = 0,
    @SerialName("target_date") val targetDate: String? = null,
    @SerialName("parent_id") val parentId: String? = null,
    @SerialName("linked_habit_ids") val linkedHabitIds: List<String> = emptyList(),
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class GoalMilestoneDto(
    val id: String,
    @SerialName("goal_id") val goalId: String,
    val title: String,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("completed_at") val completedAt: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class CalendarEventDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val location: String? = null,
    @SerialName("start_time") val startTime: String,
    @SerialName("end_time") val endTime: String? = null,
    @SerialName("is_all_day") val isAllDay: Boolean = false,
    val color: String? = null,
    val recurrence: String? = null,
    @SerialName("linked_goal_id") val linkedGoalId: String? = null,
    @SerialName("linked_habit_id") val linkedHabitId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class BoardDto(
    val id: String,
    val title: String,
    val description: String? = null,
    val color: String? = null,
    val icon: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("linked_goal_id") val linkedGoalId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class BoardColumnDto(
    val id: String,
    @SerialName("board_id") val boardId: String,
    val title: String,
    val color: String? = null,
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String
)

@Serializable
data class CardDto(
    val id: String,
    @SerialName("column_id") val columnId: String,
    val title: String,
    val description: String? = null,
    val priority: String = "none",
    @SerialName("due_date") val dueDate: String? = null,
    val labels: List<String> = emptyList(),
    @SerialName("sort_order") val sortOrder: Int = 0,
    @SerialName("is_completed") val isCompleted: Boolean = false,
    @SerialName("linked_note_id") val linkedNoteId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)

@Serializable
data class NoteDto(
    val id: String,
    val title: String,
    val content: String? = null,
    val tags: List<String> = emptyList(),
    @SerialName("is_pinned") val isPinned: Boolean = false,
    @SerialName("linked_goal_id") val linkedGoalId: String? = null,
    @SerialName("linked_card_id") val linkedCardId: String? = null,
    @SerialName("created_at") val createdAt: String,
    @SerialName("updated_at") val updatedAt: String,
    @SerialName("deleted_at") val deletedAt: String? = null
)
