package moe.memesta.automemoria.assist

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import moe.memesta.automemoria.ui.theme.AutomemoriaTheme
import dagger.hilt.android.AndroidEntryPoint

enum class QuickCaptureType(val label: String, val icon: ImageVector) {
    NOTE("Note", Icons.Default.Notes),
    TASK("Task", Icons.Default.CheckBox),
    HABIT("Habit", Icons.Default.CheckCircle),
    EVENT("Event", Icons.Default.Event)
}

@AndroidEntryPoint
class QuickCaptureActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Make window transparent so the overlay floats above whatever app is open
        window.setBackgroundDrawableResource(android.R.color.transparent)

        setContent {
            AutomemoriaTheme {
                QuickCaptureOverlay(
                    onDismiss = { finish() },
                    onSave = { text, type ->
                        handleQuickCapture(text, type)
                        finish()
                    }
                )
            }
        }
    }

    private fun handleQuickCapture(text: String, type: QuickCaptureType) {
        // TODO: inject repositories via Hilt and dispatch accordingly
        // For Phase 7 full implementation:
        // QuickCaptureType.NOTE  → NoteRepository.createNote(title = text)
        // QuickCaptureType.TASK  → CardRepository.createCard(title = text, columnId = defaultColumnId)
        // QuickCaptureType.HABIT → open habit selector sheet
        // QuickCaptureType.EVENT → CalendarRepository.createEvent(title = text, startTime = now)
    }
}

@Composable
fun QuickCaptureOverlay(
    onDismiss: () -> Unit,
    onSave: (text: String, type: QuickCaptureType) -> Unit
) {
    var text by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(QuickCaptureType.NOTE) }
    val focusRequester = remember { FocusRequester() }

    // Dim background — tap outside to dismiss
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onDismiss)
    ) {
        // Bottom sheet card — don't propagate taps to the dimmer
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .clickable(enabled = false) { /* consume */ },
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
                // Drag handle
                Box(
                    modifier = Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .align(Alignment.CenterHorizontally)
                        .also { /* rounded */ }
                )

                Text(
                    "Quick Capture",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                // Type selector
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

                // Text input
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    placeholder = {
                        Text(
                            when (selectedType) {
                                QuickCaptureType.NOTE  -> "Note title..."
                                QuickCaptureType.TASK  -> "Task name..."
                                QuickCaptureType.HABIT -> "Search habit..."
                                QuickCaptureType.EVENT -> "Event title..."
                            }
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )

                // Action buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(onClick = onDismiss, modifier = Modifier.weight(1f)) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = { if (text.isNotBlank()) onSave(text, selectedType) },
                        enabled = text.isNotBlank(),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Save")
                    }
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}
