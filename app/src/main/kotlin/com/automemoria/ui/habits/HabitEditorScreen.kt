package com.automemoria.ui.habits

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.automemoria.domain.model.HabitFrequency
import com.automemoria.ui.common.HabitIconOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HabitEditorScreen(
    navController: NavController,
    viewModel: HabitEditorViewModel = hiltViewModel()
) {
    val state = viewModel.uiState.collectAsStateWithLifecycle().value
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.error) {
        if (!state.error.isNullOrBlank()) {
            snackbarHostState.showSnackbar(state.error)
        }
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) navController.popBackStack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEditMode) "Edit Habit" else "New Habit") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val icons = HabitIconOptions
        val colors = listOf("#7C3AED", "#2563EB", "#0D9488", "#DC2626", "#EA580C", "#16A34A")

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item { Spacer(Modifier.height(4.dp)) }

            item {
                OutlinedTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = { Text("Habit name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                OutlinedTextField(
                    value = state.description,
                    onValueChange = viewModel::onDescriptionChange,
                    label = { Text("Description") },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Text("Icon", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    icons.forEach { option ->
                        val selected = state.icon == option.key
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .background(
                                    if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                                    RoundedCornerShape(10.dp)
                                )
                                .clickable { viewModel.onIconChange(option.key) },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = option.image,
                                contentDescription = option.contentDescription,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }

            item {
                Text("Color", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    colors.forEach { hex ->
                        val parsed = parseColor(hex)
                        val selected = state.color == hex
                        Box(
                            modifier = Modifier
                                .size(if (selected) 30.dp else 26.dp)
                                .background(parsed, RoundedCornerShape(7.dp))
                                .clickable { viewModel.onColorChange(hex) }
                        )
                    }
                }
            }

            item {
                Text("Frequency", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    HabitFrequency.entries.forEach { freq ->
                        FilterChip(
                            selected = state.frequency == freq,
                            onClick = { viewModel.onFrequencyChange(freq) },
                            label = {
                                Text(freq.name.lowercase().replaceFirstChar { it.uppercase() })
                            }
                        )
                    }
                }
            }

            item {
                OutlinedTextField(
                    value = state.targetStreak,
                    onValueChange = viewModel::onTargetStreakChange,
                    label = { Text("Target streak") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item {
                Spacer(Modifier.height(8.dp))
                Button(
                    onClick = viewModel::save,
                    enabled = !state.isSaving,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSaving) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                    } else {
                        Text(if (state.isEditMode) "Save Changes" else "Create Habit")
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (_: IllegalArgumentException) {
    Color(0xFF7C3AED)
}
