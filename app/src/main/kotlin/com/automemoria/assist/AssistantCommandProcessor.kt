package com.automemoria.assist

import com.automemoria.data.repository.CalendarRepository
import com.automemoria.data.repository.HabitRepository
import dagger.hilt.android.scopes.ActivityRetainedScoped
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@ActivityRetainedScoped
class AssistantCommandProcessor @Inject constructor(
    private val habitRepository: HabitRepository,
    private val calendarRepository: CalendarRepository
) {

    suspend fun execute(rawInput: String): AssistantCommandResult {
        val input = rawInput.trim()
        if (input.isBlank()) {
            return AssistantCommandResult(
                confirmation = "I did not catch that. Try saying a command like 'what's next'.",
                matchedIntent = AssistantIntent.UNKNOWN
            )
        }

        val normalized = input.lowercase(Locale.ENGLISH)

        return when {
            normalized in setOf("done", "finished", "completed") -> markDone()
            normalized.contains("skip") -> skipCurrent()
            normalized.contains("snooze") -> snoozeCurrent()
            normalized.contains("what's next") || normalized.contains("whats next") -> whatsNext()
            normalized.contains("how am i doing") || normalized.contains("how am i doing?") -> progressUpdate()
            normalized.startsWith("add a habit for") || normalized.startsWith("add habit") -> addHabit(input)
            normalized.startsWith("remind me at") -> quickReminder(input)
            else -> AssistantCommandResult(
                confirmation = "I couldn't map that yet. Try: done, what's next, add a habit..., remind me at 9pm.",
                matchedIntent = AssistantIntent.UNKNOWN
            )
        }
    }

    private suspend fun markDone(): AssistantCommandResult {
        val habit = habitRepository.markAnyActiveHabitDoneToday()
        if (habit != null) {
            return AssistantCommandResult(
                confirmation = "Marked ${habit.name} complete for today.",
                matchedIntent = AssistantIntent.MARK_DONE
            )
        }

        val event = calendarRepository.markCurrentEventDone()
        if (event != null) {
            return AssistantCommandResult(
                confirmation = "Marked '${event.title}' as done.",
                matchedIntent = AssistantIntent.MARK_DONE
            )
        }

        return AssistantCommandResult(
            confirmation = "I could not find an active habit or schedule item to mark done.",
            matchedIntent = AssistantIntent.MARK_DONE
        )
    }

    private suspend fun skipCurrent(): AssistantCommandResult {
        val event = calendarRepository.markCurrentEventSkipped()
        return if (event != null) {
            AssistantCommandResult(
                confirmation = "Skipped '${event.title}'.",
                matchedIntent = AssistantIntent.SKIP
            )
        } else {
            AssistantCommandResult(
                confirmation = "There is no current schedule item to skip.",
                matchedIntent = AssistantIntent.SKIP
            )
        }
    }

    private suspend fun snoozeCurrent(): AssistantCommandResult {
        val event = calendarRepository.snoozeCurrentEventBy(minutes = 15)
        return if (event != null) {
            AssistantCommandResult(
                confirmation = "Snoozed '${event.title}' by 15 minutes.",
                matchedIntent = AssistantIntent.SNOOZE
            )
        } else {
            AssistantCommandResult(
                confirmation = "There is no current schedule item to snooze.",
                matchedIntent = AssistantIntent.SNOOZE
            )
        }
    }

    private suspend fun whatsNext(): AssistantCommandResult {
        val event = calendarRepository.getCurrentOrNextEvent()
        return if (event != null) {
            AssistantCommandResult(
                confirmation = "Next: ${event.title} at ${event.startTime.format(TIME_FORMATTER)}.",
                matchedIntent = AssistantIntent.WHATS_NEXT
            )
        } else {
            AssistantCommandResult(
                confirmation = "You have no remaining schedule items today.",
                matchedIntent = AssistantIntent.WHATS_NEXT
            )
        }
    }

    private suspend fun progressUpdate(): AssistantCommandResult {
        val snapshot = habitRepository.getTodayCompletionSnapshot()
        val confirmation = if (snapshot.totalHabits == 0) {
            "You have no active habits yet."
        } else {
            "You completed ${snapshot.completedHabits} of ${snapshot.totalHabits} habits today (${snapshot.completionPercent}%)."
        }
        return AssistantCommandResult(
            confirmation = confirmation,
            matchedIntent = AssistantIntent.PROGRESS
        )
    }

    private suspend fun addHabit(input: String): AssistantCommandResult {
        val habitName = input
            .replace(Regex("(?i)^add a habit for\\s+"), "")
            .replace(Regex("(?i)^add habit\\s+"), "")
            .replace(Regex("(?i)\\s+(every day|daily)$"), "")
            .trim()

        if (habitName.isBlank()) {
            return AssistantCommandResult(
                confirmation = "Tell me the habit name, for example: add a habit for reading 20 minutes every day.",
                matchedIntent = AssistantIntent.CREATE_HABIT
            )
        }

        val created = habitRepository.createHabit(name = habitName)
        return AssistantCommandResult(
            confirmation = "Habit added: ${created.name}.",
            matchedIntent = AssistantIntent.CREATE_HABIT
        )
    }

    private suspend fun quickReminder(input: String): AssistantCommandResult {
        val match = REMIND_ME_REGEX.find(input)
        if (match == null) {
            return AssistantCommandResult(
                confirmation = "Use: remind me at 9pm or remind me at 21:30.",
                matchedIntent = AssistantIntent.CREATE_REMINDER
            )
        }

        val hourRaw = match.groupValues[1].toIntOrNull() ?: return AssistantCommandResult(
            confirmation = "I couldn't parse that time.",
            matchedIntent = AssistantIntent.CREATE_REMINDER
        )
        val minute = match.groupValues[2].takeIf { it.isNotBlank() }?.toIntOrNull() ?: 0
        val amPm = match.groupValues[3].lowercase(Locale.ENGLISH)

        val hour24 = when {
            amPm == "pm" && hourRaw in 1..11 -> hourRaw + 12
            amPm == "am" && hourRaw == 12 -> 0
            else -> hourRaw
        }.coerceIn(0, 23)

        val now = LocalDateTime.now()
        var target = now.withHour(hour24).withMinute(minute).withSecond(0).withNano(0)
        if (!target.isAfter(now)) {
            target = target.plusDays(1)
        }

        calendarRepository.create(
            title = "Reminder",
            description = "Created by assistant command",
            startTime = target,
            endTime = target.plusMinutes(15)
        )

        return AssistantCommandResult(
            confirmation = "Reminder set for ${target.format(TIME_FORMATTER)}.",
            matchedIntent = AssistantIntent.CREATE_REMINDER
        )
    }

    companion object {
        private val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm a")
        private val REMIND_ME_REGEX = Regex("(?i)remind me at\\s+(\\d{1,2})(?::(\\d{2}))?\\s*(am|pm)?")
    }
}

enum class AssistantIntent {
    MARK_DONE,
    SKIP,
    SNOOZE,
    WHATS_NEXT,
    PROGRESS,
    CREATE_HABIT,
    CREATE_REMINDER,
    UNKNOWN
}

data class AssistantCommandResult(
    val confirmation: String,
    val matchedIntent: AssistantIntent
)
