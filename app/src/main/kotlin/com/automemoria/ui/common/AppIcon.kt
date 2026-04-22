package com.automemoria.ui.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DirectionsRun
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.SelfImprovement
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.ViewKanban
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.ui.graphics.vector.ImageVector

data class AppSelectableIcon(
    val key: String,
    val image: ImageVector,
    val contentDescription: String
)

val HabitIconOptions = listOf(
    AppSelectableIcon("check_circle", Icons.Default.CheckCircle, "Check"),
    AppSelectableIcon("fitness_center", Icons.Default.FitnessCenter, "Fitness"),
    AppSelectableIcon("menu_book", Icons.Default.MenuBook, "Reading"),
    AppSelectableIcon("directions_run", Icons.Default.DirectionsRun, "Running"),
    AppSelectableIcon("water_drop", Icons.Default.WaterDrop, "Hydration"),
    AppSelectableIcon("self_improvement", Icons.Default.SelfImprovement, "Mindfulness"),
    AppSelectableIcon("track_changes", Icons.Default.TrackChanges, "Target"),
    AppSelectableIcon("eco", Icons.Default.Eco, "Nature"),
    AppSelectableIcon("restaurant", Icons.Default.Restaurant, "Nutrition"),
    AppSelectableIcon("bedtime", Icons.Default.Bedtime, "Sleep")
)

fun iconForKey(key: String?, fallback: ImageVector = Icons.Default.CheckCircle): ImageVector {
    return when (key?.trim()) {
        "check_circle", "✅" -> Icons.Default.CheckCircle
        "fitness_center", "💪" -> Icons.Default.FitnessCenter
        "menu_book", "📚" -> Icons.Default.MenuBook
        "directions_run", "🏃" -> Icons.Default.DirectionsRun
        "water_drop", "💧" -> Icons.Default.WaterDrop
        "self_improvement", "🧘" -> Icons.Default.SelfImprovement
        "track_changes", "🎯" -> Icons.Default.TrackChanges
        "eco", "🌿", "🌱" -> Icons.Default.Eco
        "restaurant", "🍎" -> Icons.Default.Restaurant
        "bedtime", "😴" -> Icons.Default.Bedtime
        "local_fire_department", "🔥" -> Icons.Default.LocalFireDepartment
        "bolt", "⚡" -> Icons.Default.Bolt
        "view_kanban", "📋" -> Icons.Default.ViewKanban
        else -> fallback
    }
}
