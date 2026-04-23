package com.automemoria.assist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBox
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notes
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.automemoria.data.repository.BoardRepository
import com.automemoria.data.repository.CalendarRepository
import com.automemoria.data.repository.HabitRepository
import com.automemoria.data.repository.NoteRepository
import com.automemoria.sync.SyncPreferences
import com.automemoria.ui.theme.AutomemoriaTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import javax.inject.Inject

enum class QuickCaptureType(val label: String, val icon: ImageVector) {
    NOTE("Note", Icons.Default.Notes),
    TASK("Task", Icons.Default.CheckBox),
    HABIT("Habit", Icons.Default.CheckCircle),
    EVENT("Event", Icons.Default.Event)
}

@AndroidEntryPoint
class QuickCaptureActivity : ComponentActivity() {

    @Inject
    lateinit var assistantCommandProcessor: AssistantCommandProcessor

    @Inject
    lateinit var noteRepository: NoteRepository

    @Inject
    lateinit var boardRepository: BoardRepository

    @Inject
    lateinit var habitRepository: HabitRepository

    @Inject
    lateinit var calendarRepository: CalendarRepository

    @Inject
    lateinit var syncPreferences: SyncPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            AutomemoriaTheme {
                QuickCaptureOverlay(
                    onDismiss = { finish() },
                    onSave = { text, type ->
                        lifecycleScope.launch {
                            handleQuickCapture(text = text, type = type)
                            finish()
                        }
                    },
                    onAssistantCommand = { command, onResult ->
                        lifecycleScope.launch {
                            val result = assistantCommandProcessor.execute(command)
                            onResult(result.confirmation)
                        }
                    }
                )
            }
        }
    }

    private suspend fun handleQuickCapture(text: String, type: QuickCaptureType) {
        val trimmed = text.trim()
        if (trimmed.isBlank()) return

        when (type) {
            QuickCaptureType.NOTE -> {
                noteRepository.save(title = trimmed, content = "")
            }

            QuickCaptureType.TASK -> {
                val defaultBoardId = runCatching { syncPreferences.getDefaultBoardId() }.getOrNull()
                boardRepository.quickCaptureTask(title = trimmed, defaultBoardId = defaultBoardId)
            }

            QuickCaptureType.HABIT -> {
                habitRepository.quickCaptureHabit(trimmed)
            }

            QuickCaptureType.EVENT -> {
                val start = LocalDateTime.now().withSecond(0).withNano(0)
                calendarRepository.create(
                    title = trimmed,
                    startTime = start,
                    endTime = start.plusHours(1)
                )
            }
        }
    }
}

@Composable
fun QuickCaptureOverlay(
    onDismiss: () -> Unit,
    onSave: (text: String, type: QuickCaptureType) -> Unit,
    onAssistantCommand: (command: String, onResult: (String) -> Unit) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(QuickCaptureType.NOTE) }
    var assistantMode by remember { mutableStateOf(false) }
    var assistantConfirmation by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) { },
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .padding(top = 20.dp, bottom = 36.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .align(Alignment.CenterHorizontally)
                )

                Text(
                    text = "Quick Capture",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = assistantMode,
                        onClick = { assistantMode = !assistantMode },
                        label = { Text(if (assistantMode) "Assistant On" else "Assistant Off") },
                        leadingIcon = {
                            Icon(Icons.Default.Mic, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    )
                    if (assistantMode) {
                        Text(
                            text = "Try: done, what's next, add a habit for reading",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    QuickCaptureType.entries.forEach { type ->
                        FilterChip(
                            selected = selectedType == type,
                            onClick = { selectedType = type },
                            label = { Text(type.label) },
                            leadingIcon = {
                                Icon(type.icon, contentDescription = null, modifier = Modifier.size(16.dp))
                            }
                        )
                    }
                }

                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            if (assistantMode) {
                                "Type a command..."
                            } else {
                                when (selectedType) {
                                    QuickCaptureType.NOTE -> "Note title..."
                                    QuickCaptureType.TASK -> "Task name..."
                                    QuickCaptureType.HABIT -> "Habit name..."
                                    QuickCaptureType.EVENT -> "Event title..."
                                }
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                assistantConfirmation?.let { message ->
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
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
                            if (text.isBlank()) return@Button
                            if (assistantMode) {
                                onAssistantCommand(text) { confirmation ->
                                    assistantConfirmation = confirmation
                                    text = ""
                                }
                            } else {
                                onSave(text, selectedType)
                            }
                        },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (assistantMode) Icons.Default.Mic else Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(if (assistantMode) "Run" else "Save")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
