package com.automemoria.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.DayOfWeek
import java.time.LocalDate

@Composable
fun HabitHeatmap(
    completionCounts: Map<LocalDate, Int>,
    modifier: Modifier = Modifier
) {
    val firstMonday = LocalDate.now()
        .minusWeeks(51)
        .with(DayOfWeek.MONDAY)

    val weeks = (0 until 52).map { weekIndex ->
        (0 until 7).map { dayIndex ->
            firstMonday.plusWeeks(weekIndex.toLong()).plusDays(dayIndex.toLong())
        }
    }

    Column(modifier = modifier) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            listOf("M", "T", "W", "T", "F", "S", "S").forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.size(10.dp)
                )
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            weeks.forEach { week ->
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { date ->
                        val value = completionCounts[date] ?: 0
                        Box(
                            modifier = Modifier
                                .size(10.dp)
                                .clip(RoundedCornerShape(2.dp))
                                .background(colorForCount(value))
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Past 52 weeks",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun colorForCount(count: Int): Color {
    val base = MaterialTheme.colorScheme.primary
    return when {
        count <= 0 -> MaterialTheme.colorScheme.surfaceVariant
        count == 1 -> base.copy(alpha = 0.35f)
        count == 2 -> base.copy(alpha = 0.55f)
        count == 3 -> base.copy(alpha = 0.75f)
        else -> base
    }
}
