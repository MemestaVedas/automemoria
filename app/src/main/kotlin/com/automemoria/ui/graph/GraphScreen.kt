package com.automemoria.ui.graph

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GraphScreen(
    navController: NavController,
    viewModel: GraphViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var scale by remember { mutableStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Knowledge Graph", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale *= zoom
                        offset += pan
                    }
                }
        ) {
            GraphCanvas(
                nodes = uiState.nodes,
                edges = uiState.edges,
                scale = scale,
                offset = offset
            )
        }
    }
}

@Composable
fun GraphCanvas(
    nodes: List<GraphNode>,
    edges: List<GraphEdge>,
    scale: Float,
    offset: Offset
) {
    val nodePositions = remember(nodes) {
        // Place nodes in a circle for now
        nodes.mapIndexed { index, node ->
            val angle = 2 * Math.PI * index / nodes.size
            val radius = 300f
            node.id to Offset(
                (radius * cos(angle)).toFloat(),
                (radius * sin(angle)).toFloat()
            )
        }.toMap()
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2, size.height / 2) + offset

        // Draw edges
        edges.forEach { edge ->
            val start = nodePositions[edge.sourceId]
            val end = nodePositions[edge.targetId]
            if (start != null && end != null) {
                drawLine(
                    color = Color.Gray.copy(alpha = 0.3f),
                    start = center + start * scale,
                    end = center + end * scale,
                    strokeWidth = 2f * scale
                )
            }
        }

        // Draw nodes
        nodes.forEach { node ->
            val pos = nodePositions[node.id]
            if (pos != null) {
                val finalPos = center + pos * scale
                drawCircle(
                    color = Color(0xFF7C3AED),
                    radius = 20f * scale,
                    center = finalPos
                )
                // In a real app, you'd use native canvas to draw text or LayoutNode
            }
        }
    }
}
