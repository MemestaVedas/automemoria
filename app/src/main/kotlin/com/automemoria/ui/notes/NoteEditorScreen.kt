package com.automemoria.ui.notes

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    navController: NavController,
    viewModel: NoteEditorViewModel = hiltViewModel()
) {
    var showPreview by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (showPreview) "Preview" else "Edit Note") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showPreview = !showPreview }) {
                        Icon(
                            if (showPreview) Icons.Default.Edit else Icons.Default.Visibility,
                            contentDescription = "Toggle Preview"
                        )
                    }
                    IconButton(onClick = { viewModel.isPinned = !viewModel.isPinned }) {
                        Icon(
                            if (viewModel.isPinned) Icons.Default.PushPin else Icons.Default.PushPin, // Use filled icon if pinned
                            tint = if (viewModel.isPinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            contentDescription = "Pin"
                        )
                    }
                    TextButton(onClick = { viewModel.saveNote { navController.popBackStack() } }) {
                        Text("Save", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            if (showPreview) {
                NotePreview(title = viewModel.title, content = viewModel.content)
            } else {
                NoteEditor(
                    title = viewModel.title,
                    onTitleChange = { viewModel.title = it },
                    content = viewModel.content,
                    onContentChange = { viewModel.content = it }
                )
            }
        }
    }
}

@Composable
fun NoteEditor(
    title: String,
    onTitleChange: (String) -> Unit,
    content: String,
    onContentChange: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TextField(
            value = title,
            onValueChange = onTitleChange,
            placeholder = { Text("Title", style = MaterialTheme.typography.headlineSmall) },
            modifier = Modifier.fillMaxWidth(),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
        )
        
        Spacer(Modifier.height(8.dp))
        
        TextField(
            value = content,
            onValueChange = onContentChange,
            placeholder = { Text("Start typing... Use [[links]] for backlinks") },
            modifier = Modifier.fillMaxWidth().weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            ),
            textStyle = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
fun NotePreview(title: String, content: String) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        item {
            Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
        }
        item {
            // Very simple markdown-like rendering for wikilinks
            val annotatedContent = remember(content) {
                // This would be a real markdown parser in a full app
                content
            }
            Text(annotatedContent, style = MaterialTheme.typography.bodyLarge)
        }
    }
}
