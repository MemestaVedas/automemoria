package com.automemoria.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.automemoria.domain.model.GoalMilestone
import com.automemoria.domain.model.Habit
import com.automemoria.ui.common.iconForKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    navController: NavController,
    viewModel: GoalDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val goal = uiState.goal

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(goal?.title ?: "Goal Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: edit goal */ }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading || goal == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // ── Progress Card ───────────────────────────────────────────────
            item {
                GoalProgressSection(goal.progress, goal.color)
            }

            // ── Description ─────────────────────────────────────────────────
            goal.description?.let {
                item {
                    Column {
                        Text("About this Goal", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.height(8.dp))
                        Text(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            // ── Milestones ──────────────────────────────────────────────────
            item {
                Text("Milestones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            
            if (uiState.milestones.isEmpty()) {
                item {
                    Text("No milestones defined.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                items(uiState.milestones) { milestone ->
                    MilestoneItem(
                        milestone = milestone,
                        onToggle = { viewModel.toggleMilestone(milestone.id, it) }
                    )
                }
            }

            // ── Linked Habits ───────────────────────────────────────────────
            if (uiState.linkedHabits.isNotEmpty()) {
                item {
                    Text("Linked Habits", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                }
                items(uiState.linkedHabits) { habit ->
                    LinkedHabitItem(habit = habit)
                }
            }
        }
    }
}

@Composable
fun GoalProgressSection(progress: Int, colorHex: String?) {
    val color = colorHex?.let { parseColor(it) } ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.05f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "$progress%",
                    style = MaterialTheme.typography.displaySmall,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Spacer(Modifier.width(16.dp))
                LinearProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier
                        .weight(1f)
                        .height(12.dp)
                        .clip(CircleShape),
                    color = color,
                    trackColor = color.copy(alpha = 0.1f)
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                if (progress == 100) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = color
                    )
                    Text(
                        "Goal Achieved!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "Keep going, you're getting closer!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun MilestoneItem(milestone: GoalMilestone, onToggle: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = milestone.isCompleted,
            onCheckedChange = onToggle
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = milestone.title,
            style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (milestone.isCompleted) androidx.compose.ui.text.style.TextDecoration.LineThrough else null,
            color = if (milestone.isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun LinkedHabitItem(habit: Habit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconForKey(habit.icon, fallback = Icons.Default.Bolt),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(habit.name, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.weight(1f))
            Icon(Icons.Default.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF7C3AED)
}
