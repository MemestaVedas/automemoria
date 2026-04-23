package com.automemoria.ui.kanban

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.automemoria.domain.model.Card
import com.automemoria.domain.model.CardPriority

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BoardDetailScreen(
    navController: NavController,
    viewModel: BoardDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val board = uiState.board
    var addCardColumnId by remember { mutableStateOf<String?>(null) }
    var newCardTitle by remember { mutableStateOf("") }
    var showAddColumnDialog by remember { mutableStateOf(false) }
    var newColumnTitle by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(board?.title ?: "Board Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: edit board */ }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading || board == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        LazyRow(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(uiState.columns) { colWithCards ->
                KanbanColumn(
                    column = colWithCards,
                    onAddCard = { addCardColumnId = colWithCards.column.id },
                    onCardClick = { /* Card detail editor can be added in next iteration. */ }
                )
            }
            
            item {
                AddColumnButton(onClick = { showAddColumnDialog = true })
            }
        }
    }

    if (addCardColumnId != null) {
        AlertDialog(
            onDismissRequest = {
                addCardColumnId = null
                newCardTitle = ""
            },
            title = { Text("New Card") },
            text = {
                OutlinedTextField(
                    value = newCardTitle,
                    onValueChange = { newCardTitle = it },
                    label = { Text("Card title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val columnId = addCardColumnId
                        if (columnId != null && newCardTitle.isNotBlank()) {
                            viewModel.addCard(columnId = columnId, title = newCardTitle)
                            addCardColumnId = null
                            newCardTitle = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        addCardColumnId = null
                        newCardTitle = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showAddColumnDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddColumnDialog = false
                newColumnTitle = ""
            },
            title = { Text("New Column") },
            text = {
                OutlinedTextField(
                    value = newColumnTitle,
                    onValueChange = { newColumnTitle = it },
                    label = { Text("Column title") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newColumnTitle.isNotBlank()) {
                            viewModel.addColumn(newColumnTitle)
                            showAddColumnDialog = false
                            newColumnTitle = ""
                        }
                    }
                ) {
                    Text("Create")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showAddColumnDialog = false
                        newColumnTitle = ""
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun KanbanColumn(
    column: BoardColumnWithCards,
    onAddCard: () -> Unit,
    onCardClick: (Card) -> Unit
) {
    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxHeight()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Text(
                column.column.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Text(
                "${column.cards.size}",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                    .padding(horizontal = 8.dp, vertical = 2.dp)
            )
            IconButton(onClick = onAddCard) {
                Icon(Icons.Default.Add, contentDescription = "Add card", modifier = Modifier.size(20.dp))
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
            contentPadding = PaddingValues(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(column.cards) { card ->
                KanbanCard(card = card, onClick = { onCardClick(card) })
            }
        }
    }
}

@Composable
fun KanbanCard(card: Card, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            if (card.priority != CardPriority.NONE) {
                PriorityBadge(card.priority)
                Spacer(Modifier.height(8.dp))
            }
            Text(card.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
            
            if (card.labels.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    card.labels.take(3).forEach { label ->
                        Box(
                            modifier = Modifier
                                .size(width = 32.dp, height = 4.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.3f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PriorityBadge(priority: CardPriority) {
    val color = when (priority) {
        CardPriority.URGENT -> Color(0xFFEF4444)
        CardPriority.HIGH -> Color(0xFFF59E0B)
        CardPriority.MEDIUM -> Color(0xFF3B82F6)
        CardPriority.LOW -> Color(0xFF10B981)
        else -> MaterialTheme.colorScheme.outline
    }
    
    Box(
        modifier = Modifier
            .size(width = 40.dp, height = 4.dp)
            .clip(CircleShape)
            .background(color)
    )
}

@Composable
fun AddColumnButton(onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier
            .width(280.dp)
            .height(56.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text("Add Column")
    }
}
