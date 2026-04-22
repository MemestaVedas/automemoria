package com.automemoria.ui.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.automemoria.domain.model.Goal
import com.automemoria.ui.common.iconForKey
import com.automemoria.ui.navigation.Screen
import com.automemoria.ui.theme.AppColors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalListScreen(
    navController: NavController,
    viewModel: GoalViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Goals", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate(Screen.GoalEditor.createRoute()) },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create goal")
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ── Hero Progress Header ──────────────────────────────────────────
            item {
                GoalHeroHeader(progress = uiState.totalProgress)
            }

            item {
                Text(
                    "Active Goals",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            if (uiState.goals.isEmpty() && !uiState.isLoading) {
                item {
                    EmptyGoalsState()
                }
            } else {
                items(uiState.goals) { goal ->
                    GoalCard(
                        goal = goal,
                        onClick = { navController.navigate(Screen.GoalDetail.createRoute(goal.id)) }
                    )
                }
            }
        }
    }
}

@Composable
fun GoalHeroHeader(progress: Int) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(primaryColor, secondaryColor)
                    )
                )
                .padding(24.dp)
        ) {
            Column(modifier = Modifier.align(Alignment.CenterStart)) {
                Text(
                    "Overall Progress",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.labelLarge
                )
                Text(
                    "$progress%",
                    color = Color.White,
                    style = MaterialTheme.typography.displayMedium,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .size(80.dp)
                    .align(Alignment.CenterEnd)
            ) {
                CircularProgressIndicator(
                    progress = progress / 100f,
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White,
                    strokeWidth = 8.dp,
                    trackColor = Color.White.copy(alpha = 0.2f)
                )
                Icon(
                    Icons.Default.TrendingUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        }
    }
}

@Composable
fun GoalCard(goal: Goal, onClick: () -> Unit) {
    val goalColor = goal.color?.let { parseColor(it) } ?: MaterialTheme.colorScheme.primary

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(goalColor.copy(alpha = 0.1f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = iconForKey(goal.icon, fallback = Icons.Default.TrackChanges),
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = goalColor
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        goal.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    goal.description?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                    }
                }
                Text(
                    "${goal.progress}%",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = goalColor
                )
            }

            Spacer(Modifier.height(16.dp))

            LinearProgressIndicator(
                progress = goal.progress / 100f,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape),
                color = goalColor,
                trackColor = goalColor.copy(alpha = 0.1f)
            )

            if (goal.targetDate != null) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Event,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        "Target: ${goal.targetDate}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun EmptyGoalsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            Icons.Default.TrackChanges,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "No goals active",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun parseColor(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: Exception) {
    Color(0xFF7C3AED)
}
