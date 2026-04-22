package com.automemoria.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalEditorScreen(
    navController: NavController,
    viewModel: GoalEditorViewModel = hiltViewModel()
) {
    var showMilestoneDialog by remember { mutableStateOf(false) }
    var newMilestoneTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Goal") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveGoal { navController.popBackStack() } }) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // ── Title ───────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = viewModel.title,
                    onValueChange = { viewModel.title = it },
                    label = { Text("Goal Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Description ─────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    label = { Text("Description (Optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }

            // ── Visuals ─────────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Icon", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable { /* TODO: icon picker */ },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(viewModel.icon, fontSize = 24.sp)
                        }
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Color", style = MaterialTheme.typography.labelLarge)
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(parseColor(viewModel.color))
                                .clickable { /* TODO: color picker */ }
                        )
                    }
                }
            }

            // ── Milestones ──────────────────────────────────────────────────
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Milestones", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    IconButton(onClick = { showMilestoneDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Add milestone")
                    }
                }
            }

            itemsIndexed(viewModel.milestones) { index, milestone ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(12.dp))
                        Text(milestone, modifier = Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removeMilestone(index) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
        }
    }

    if (showMilestoneDialog) {
        AlertDialog(
            onDismissRequest = { showMilestoneDialog = false },
            title = { Text("Add Milestone") },
            text = {
                OutlinedTextField(
                    value = newMilestoneTitle,
                    onValueChange = { newMilestoneTitle = it },
                    label = { Text("Milestone Title") },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newMilestoneTitle.isNotBlank()) {
                            viewModel.addMilestone(newMilestoneTitle)
                            newMilestoneTitle = ""
                            showMilestoneDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showMilestoneDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF7C3AED)
}
