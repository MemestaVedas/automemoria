package com.automemoria.ui.habits

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.automemoria.domain.model.Habit
import com.automemoria.domain.model.HabitFrequency
import com.automemoria.ui.navigation.Screen
import com.automemoria.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitListScreen(
    navController: NavController,
    viewModel: HabitViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Habits", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = viewModel::showCreateSheet,
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "New habit")
            }
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.habits.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🌱", style = MaterialTheme.typography.displayLarge)
                    Spacer(Modifier.height(16.dp))
                    Text("No habits yet", style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Start building a streak",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(Modifier.height(24.dp))
                    Button(onClick = viewModel::showCreateSheet) { Text("Add first habit") }
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.padding(padding)
            ) {
                items(uiState.habits, key = { it.id }) { habit ->
                    HabitCard(
                        habit = habit,
                        onToggle = { viewModel.toggleToday(habit.id) },
                        onClick = { navController.navigate(Screen.HabitDetail.createRoute(habit.id)) },
                        onArchive = { viewModel.archiveHabit(habit.id) }
                    )
                }
            }
        }
    }

    // ── Create Habit Bottom Sheet ─────────────────────────────────────────────
    if (uiState.showCreateSheet) {
        ModalBottomSheet(
            onDismissRequest = viewModel::hideCreateSheet,
            sheetState = sheetState
        ) {
            CreateHabitSheet(
                onSave = { name, desc, icon, color, freq ->
                    viewModel.createHabit(name, desc, icon, color, freq)
                },
                onDismiss = viewModel::hideCreateSheet
            )
        }
    }
}

// ─── Habit Card ───────────────────────────────────────────────────────────────

@Composable
fun HabitCard(
    habit: Habit,
    onToggle: () -> Unit,
    onClick: () -> Unit,
    onArchive: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val accentColor = habit.color?.let {
        try { Color(android.graphics.Color.parseColor(it)) } catch (e: Exception) { AppColors.Primary }
    } ?: AppColors.Primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Icon bubble
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(habit.icon ?: "✅", style = MaterialTheme.typography.titleLarge)
            }

            // Name + frequency
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = habit.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = habit.frequency.name.lowercase().replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Streak chip
            Surface(
                color = AppColors.Warning.copy(alpha = 0.15f),
                shape = RoundedCornerShape(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("🔥", style = MaterialTheme.typography.bodySmall)
                    Spacer(Modifier.width(2.dp))
                    Text(
                        text = "0",
                        style = MaterialTheme.typography.labelLarge,
                        color = AppColors.Warning,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Check button
            IconButton(onClick = onToggle) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Mark complete",
                    tint = accentColor
                )
            }

            // Overflow menu
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Options")
                }
                DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                    DropdownMenuItem(
                        text = { Text("Archive") },
                        leadingIcon = { Icon(Icons.Default.Archive, null) },
                        onClick = { showMenu = false; onArchive() }
                    )
                }
            }
        }
    }
}

// ─── Create Habit Sheet ───────────────────────────────────────────────────────

@Composable
fun CreateHabitSheet(
    onSave: (name: String, desc: String?, icon: String?, color: String?, freq: HabitFrequency) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedFreq by remember { mutableStateOf(HabitFrequency.DAILY) }
    val icons = listOf("✅", "💪", "📚", "🏃", "💧", "🧘", "🎯", "🌿", "🍎", "😴")
    var selectedIcon by remember { mutableStateOf(icons.first()) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            "New Habit",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Habit name") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Description (optional)") },
            minLines = 2,
            modifier = Modifier.fillMaxWidth()
        )

        // Icon picker
        Text("Icon", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            icons.forEach { icon ->
                val isSelected = icon == selectedIcon
                Surface(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { selectedIcon = icon },
                    color = if (isSelected)
                        MaterialTheme.colorScheme.primaryContainer
                    else
                        MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(icon, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        // Frequency selector
        Text("Frequency", style = MaterialTheme.typography.labelLarge)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            HabitFrequency.entries.forEach { freq ->
                FilterChip(
                    selected = selectedFreq == freq,
                    onClick = { selectedFreq = freq },
                    label = { Text(freq.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                Text("Cancel")
            }
            Button(
                onClick = {
                    onSave(name, description.ifBlank { null }, selectedIcon, null, selectedFreq)
                },
                enabled = name.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Create")
            }
        }
    }
}
