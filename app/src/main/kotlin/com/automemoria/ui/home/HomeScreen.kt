package com.automemoria.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import com.automemoria.ui.navigation.Screen
import com.automemoria.ui.theme.AppColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val today = LocalDate.now()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = today.format(DateTimeFormatter.ofPattern("EEEE")),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = today.format(DateTimeFormatter.ofPattern("MMMM d")),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                actions = {
                    // Sync indicator
                    when (uiState.syncStatus) {
                        SyncIndicator.Syncing -> CircularProgressIndicator(
                            modifier = Modifier.size(20.dp).padding(end = 8.dp),
                            strokeWidth = 2.dp
                        )
                        SyncIndicator.Error -> Icon(
                            Icons.Default.CloudOff,
                            contentDescription = "Sync error",
                            tint = AppColors.Error,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        SyncIndicator.Idle -> Icon(
                            Icons.Default.CloudDone,
                            contentDescription = "Synced",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                    }
                    IconButton(onClick = { navController.navigate(Screen.Settings.route) }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { /* TODO: open quick capture bottom sheet */ },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Quick capture")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
        ) {
            // ── Today's Habits ──────────────────────────────────────────────
            SectionHeader(title = "Today's Habits", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))

            if (uiState.todayHabits.isEmpty()) {
                EmptyState(
                    message = "No habits yet",
                    actionLabel = "Add your first habit",
                    onAction = { navController.navigate(Screen.Habits.route) },
                    modifier = Modifier.padding(16.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(uiState.todayHabits) { item ->
                        HabitChip(
                            name = item.habit.name,
                            icon = item.habit.icon ?: "✅",
                            completed = item.completedToday,
                            color = item.habit.color?.let { parseColor(it) }
                                ?: MaterialTheme.colorScheme.primary,
                            onToggle = { viewModel.toggleHabit(item.habit.id) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Goals ───────────────────────────────────────────────────────
            SectionHeader(
                title = "Active Goals",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (uiState.activeGoals.isEmpty()) {
                EmptyState(
                    message = "No active goals",
                    actionLabel = "Create a goal",
                    onAction = { navController.navigate(Screen.Goals.route) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    items(uiState.activeGoals) { goal ->
                        HomeGoalCard(
                            goal = goal,
                            onClick = { navController.navigate(Screen.GoalDetail.createRoute(goal.id)) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // ── Recent Notes ────────────────────────────────────────────────
            SectionHeader(
                title = "Recent Notes",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            
            if (uiState.recentNotes.isEmpty()) {
                EmptyState(
                    message = "No notes yet",
                    actionLabel = "Start writing",
                    onAction = { navController.navigate(Screen.NoteList.route) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            } else {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    uiState.recentNotes.forEach { note ->
                        HomeNoteItem(
                            note = note,
                            onClick = { navController.navigate(Screen.NoteEditor.createRoute(note.id)) }
                        )
                        Spacer(Modifier.height(8.dp))
                    }
                }
            }

            // ── Quick Actions ───────────────────────────────────────────────
            SectionHeader(
                title = "Quick Actions",
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(start = 16.dp, top = 0.dp, end = 16.dp, bottom = 24.dp)
            ) {
                QuickActionCard(
                    label = "Notes",
                    icon = Icons.Default.Notes,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.NoteList.route) }
                )
                QuickActionCard(
                    label = "Boards",
                    icon = Icons.Default.ViewKanban,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Boards.route) }
                )
                QuickActionCard(
                    label = "Calendar",
                    icon = Icons.Default.CalendarMonth,
                    modifier = Modifier.weight(1f),
                    onClick = { navController.navigate(Screen.Calendar.route) }
                )
            }
        }
    }
}

// ─── Reusable components ──────────────────────────────────────────────────────

@Composable
fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier
    )
}

@Composable
fun HabitChip(
    name: String,
    icon: String,
    completed: Boolean,
    color: Color,
    onToggle: () -> Unit
) {
    val bg = if (completed) color.copy(alpha = 0.2f) else MaterialTheme.colorScheme.surfaceVariant
    val border = if (completed) color else MaterialTheme.colorScheme.outline

    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onToggle),
        color = bg,
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, border)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text(icon, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (completed) FontWeight.SemiBold else FontWeight.Normal,
                color = if (completed) color else MaterialTheme.colorScheme.onSurface
            )
            if (completed) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun QuickActionCard(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 16.dp, horizontal = 8.dp).fillMaxWidth()
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(28.dp))
            Spacer(Modifier.height(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
fun HomeGoalCard(goal: com.automemoria.domain.model.Goal, onClick: () -> Unit) {
    val color = goal.color?.let { parseColor(it) } ?: MaterialTheme.colorScheme.primary
    
    Card(
        modifier = Modifier
            .width(160.dp)
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(goal.icon ?: "🎯", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(8.dp))
            Text(goal.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = goal.progress / 100f,
                modifier = Modifier.fillMaxWidth().height(4.dp).clip(CircleShape),
                color = color,
                trackColor = color.copy(alpha = 0.2f)
            )
        }
    }
}

@Composable
fun HomeNoteItem(note: com.automemoria.domain.model.Note, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Description, contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(12.dp))
            Text(
                if (note.title.isNotBlank()) note.title else note.content.take(30),
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1
            )
        }
    }
}

@Composable
fun EmptyState(
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(24.dp).fillMaxWidth()
        ) {
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF7C3AED)
}
