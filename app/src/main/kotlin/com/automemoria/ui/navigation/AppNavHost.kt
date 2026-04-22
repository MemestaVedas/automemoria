package moe.memesta.automemoria.ui.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.*
import moe.memesta.automemoria.ui.habits.HabitListScreen
import moe.memesta.automemoria.ui.home.HomeScreen
import moe.memesta.automemoria.ui.calendar.CalendarScreen
import moe.memesta.automemoria.ui.kanban.BoardListScreen
import moe.memesta.automemoria.ui.notes.NoteListScreen
import moe.memesta.automemoria.ui.settings.SettingsScreen

// ─── Route definitions ────────────────────────────────────────────────────────

sealed class Screen(val route: String) {
    // Bottom nav destinations
    object Home     : Screen("home")
    object Habits   : Screen("habits")
    object Goals    : Screen("goals")
    object Calendar : Screen("calendar")
    object Boards   : Screen("boards")

    // Detail screens
    object HabitDetail  : Screen("habit/{habitId}") {
        fun createRoute(id: String) = "habit/$id"
    }
    object HabitEditor  : Screen("habit_editor?id={id}") {
        fun createRoute(id: String? = null) = if (id != null) "habit_editor?id=$id" else "habit_editor"
    }
    object GoalDetail   : Screen("goal/{goalId}") {
        fun createRoute(id: String) = "goal/$id"
    }
    object BoardDetail  : Screen("board/{boardId}") {
        fun createRoute(id: String) = "board/$id"
    }
    object NoteList     : Screen("notes")
    object NoteEditor   : Screen("note_editor?id={id}") {
        fun createRoute(id: String? = null) = if (id != null) "note_editor?id=$id" else "note_editor"
    }
    object NoteDetail   : Screen("note/{noteId}") {
        fun createRoute(id: String) = "note/$id"
    }
    object Settings     : Screen("settings")
    object Setup        : Screen("setup")
}

// ─── Bottom nav items ─────────────────────────────────────────────────────────

data class BottomNavItem(
    val screen: Screen,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector = icon
)

val bottomNavItems = listOf(
    BottomNavItem(Screen.Home,     "Home",     Icons.Default.Home),
    BottomNavItem(Screen.Habits,   "Habits",   Icons.Default.CheckCircle),
    BottomNavItem(Screen.Goals,    "Goals",    Icons.Default.TrackChanges),
    BottomNavItem(Screen.Calendar, "Calendar", Icons.Default.CalendarMonth),
    BottomNavItem(Screen.Boards,   "Boards",   Icons.Default.ViewKanban)
)

// ─── NavHost ──────────────────────────────────────────────────────────────────

@Composable
fun AppNavHost() {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination

    val showBottomBar = bottomNavItems.any { it.screen.route == currentDestination?.route }

    Scaffold(
        bottomBar = {
            if (showBottomBar) {
                NavigationBar {
                    bottomNavItems.forEach { item ->
                        val selected = currentDestination?.hierarchy
                            ?.any { it.route == item.screen.route } == true
                        NavigationBarItem(
                            selected     = selected,
                            onClick      = {
                                navController.navigate(item.screen.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState    = true
                                }
                            },
                            icon  = { Icon(item.icon, contentDescription = item.label) },
                            label = { Text(item.label) }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController    = navController,
            startDestination = Screen.Home.route,
            modifier         = Modifier.padding(innerPadding),
            enterTransition  = { fadeIn() + slideInHorizontally { it / 4 } },
            exitTransition   = { fadeOut() + slideOutHorizontally { -it / 4 } },
            popEnterTransition  = { fadeIn() + slideInHorizontally { -it / 4 } },
            popExitTransition   = { fadeOut() + slideOutHorizontally { it / 4 } }
        ) {
            composable(Screen.Home.route)     { HomeScreen(navController) }
            composable(Screen.Habits.route)   { HabitListScreen(navController) }
            composable(Screen.Goals.route)    { GoalsPlaceholderScreen() }
            composable(Screen.Calendar.route) { CalendarScreen(navController) }
            composable(Screen.Boards.route)   { BoardListScreen(navController) }

            composable(Screen.HabitDetail.route)  { HabitDetailPlaceholder(navController) }
            composable(Screen.HabitEditor.route)  { HabitEditorPlaceholder(navController) }
            composable(Screen.NoteList.route)     { NoteListScreen(navController) }
            composable(Screen.NoteEditor.route)   { NoteEditorPlaceholder(navController) }
            composable(Screen.Settings.route)     { SettingsScreen(navController) }
        }
    }
}

// Temporary placeholder composables for screens not yet built
@Composable private fun GoalsPlaceholderScreen() = PlaceholderScreen("Goals — Coming in Phase 2")
@Composable private fun HabitDetailPlaceholder(nav: NavHostController) = PlaceholderScreen("Habit Detail")
@Composable private fun HabitEditorPlaceholder(nav: NavHostController) = PlaceholderScreen("Habit Editor")
@Composable private fun NoteEditorPlaceholder(nav: NavHostController) = PlaceholderScreen("Note Editor")

@Composable
private fun PlaceholderScreen(title: String) {
    androidx.compose.foundation.layout.Box(
        modifier = Modifier.then(Modifier),
        contentAlignment = androidx.compose.ui.Alignment.Center
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
    }
}
