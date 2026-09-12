package com.example.pomodoro.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Screen(
    val route: String,
    val title: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
) {
    data object Home : Screen("home", "Home", Icons.Outlined.Home, Icons.Filled.Home)
    data object Timer : Screen("timer", "Timer", Icons.Outlined.Timer, Icons.Filled.Timer)
    data object Tasks : Screen("tasks", "Tasks", Icons.Outlined.Checklist, Icons.Filled.Checklist)
    data object Projects : Screen("projects", "Projects", Icons.Outlined.Folder, Icons.Filled.Folder)
    data object Statistics : Screen("statistics", "Statistics", Icons.Outlined.BarChart, Icons.Filled.BarChart)
    data object History : Screen("history", "History", Icons.Outlined.History, Icons.Filled.History)
    data object Settings : Screen("settings", "Settings", Icons.Outlined.Settings, Icons.Filled.Settings)

    // Detail screens (not in bottom nav)
    data object TaskDetail : Screen("task_detail/{taskId}", "Task", Icons.Outlined.Task, Icons.Filled.Task)
    data object ProjectDetail : Screen("project_detail/{projectId}", "Project", Icons.Outlined.Folder, Icons.Filled.Folder)
    data object Calendar : Screen("calendar", "Calendar", Icons.Outlined.CalendarMonth, Icons.Filled.CalendarMonth)
    data object Goals : Screen("goals", "Goals", Icons.Outlined.Flag, Icons.Filled.Flag)
    data object FocusMode : Screen("focus_mode", "Focus", Icons.Outlined.Fullscreen, Icons.Filled.Fullscreen)
}

val bottomNavItems = listOf(
    Screen.Home,
    Screen.Timer,
    Screen.Tasks,
    Screen.Statistics,
    Screen.Settings
)
