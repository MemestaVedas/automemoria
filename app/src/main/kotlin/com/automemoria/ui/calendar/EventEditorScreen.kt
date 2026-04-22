package com.automemoria.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EventEditorScreen(
    navController: NavController,
    viewModel: EventEditorViewModel = hiltViewModel()
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("New Event") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.Close, contentDescription = "Cancel")
                    }
                },
                actions = {
                    TextButton(onClick = { viewModel.saveEvent { navController.popBackStack() } }) {
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
                    label = { Text("Event Title") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            }

            // ── Time & Date ─────────────────────────────────────────────────
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    OutlinedTextField(
                        value = viewModel.startTime.format(DateTimeFormatter.ofPattern("MMM d, HH:mm")),
                        onValueChange = { },
                        label = { Text("Start") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { /* TODO: date time picker */ }
                    )
                    OutlinedTextField(
                        value = viewModel.endTime.format(DateTimeFormatter.ofPattern("MMM d, HH:mm")),
                        onValueChange = { },
                        label = { Text("End") },
                        modifier = Modifier.weight(1f),
                        readOnly = true,
                        shape = RoundedCornerShape(12.dp),
                        trailingIcon = { /* TODO: date time picker */ }
                    )
                }
            }

            // ── Location ────────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = viewModel.location,
                    onValueChange = { viewModel.location = it },
                    label = { Text("Location") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    leadingIcon = { Icon(Icons.Default.LocationOn, null) }
                )
            }

            // ── Description ─────────────────────────────────────────────────
            item {
                OutlinedTextField(
                    value = viewModel.description,
                    onValueChange = { viewModel.description = it },
                    label = { Text("Description") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    minLines = 3
                )
            }

            // ── Color ───────────────────────────────────────────────────────
            item {
                Column {
                    Text("Event Color", style = MaterialTheme.typography.labelLarge)
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
    }
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF3B82F6)
}
