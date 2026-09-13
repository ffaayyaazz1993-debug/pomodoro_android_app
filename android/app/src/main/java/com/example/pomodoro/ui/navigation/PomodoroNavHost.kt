package com.example.pomodoro.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.pomodoro.ui.screens.home.HomeScreen
import com.example.pomodoro.ui.screens.timer.TimerScreen
import com.example.pomodoro.ui.screens.timer.MultiTimerScreen
import com.example.pomodoro.ui.screens.tasks.TasksScreen
import com.example.pomodoro.ui.screens.tasks.TaskDetailScreen
import com.example.pomodoro.ui.screens.projects.ProjectsScreen
import com.example.pomodoro.ui.screens.projects.ProjectDetailScreen
import com.example.pomodoro.ui.screens.statistics.StatisticsScreen
import com.example.pomodoro.ui.screens.history.HistoryScreen
import com.example.pomodoro.ui.screens.settings.SettingsScreen
import com.example.pomodoro.ui.screens.goals.GoalsScreen
import com.example.pomodoro.ui.screens.goals.CalendarScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PomodoroNavHost(
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()

    Scaffold(
        bottomBar = { PomodoroBottomBar(navController) },
        modifier = modifier
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(Screen.Home.route) {
                HomeScreen(
                    onNavigateToTimer = { navController.navigate(Screen.Timer.route) },
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) },
                    onNavigateToTaskDetail = { taskId ->
                        navController.navigate("task_detail/$taskId")
                    },
                    onNavigateToStatistics = { navController.navigate(Screen.Statistics.route) }
                )
            }
            composable(Screen.Timer.route) {
                MultiTimerScreen(
                    onNavigateToTask = { taskId ->
                        navController.navigate("task_detail/$taskId")
                    },
                    onNavigateToTasks = { navController.navigate(Screen.Tasks.route) }
                )
            }
            composable(Screen.Tasks.route) {
                TasksScreen(
                    onTaskClick = { taskId ->
                        navController.navigate("task_detail/$taskId")
                    }
                )
            }
            composable(Screen.Projects.route) {
                ProjectsScreen(
                    onProjectClick = { projectId ->
                        navController.navigate("project_detail/$projectId")
                    }
                )
            }
            composable(Screen.Statistics.route) {
                StatisticsScreen()
            }
            composable(Screen.History.route) {
                HistoryScreen()
            }
            composable(Screen.Settings.route) {
                SettingsScreen()
            }
            composable(
                route = "task_detail/{taskId}",
                arguments = listOf(navArgument("taskId") { type = NavType.LongType })
            ) {
                TaskDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(
                route = "project_detail/{projectId}",
                arguments = listOf(navArgument("projectId") { type = NavType.LongType })
            ) {
                ProjectDetailScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }
            composable(Screen.Calendar.route) {
                CalendarScreen()
            }
            composable(Screen.Goals.route) {
                GoalsScreen()
            }
            composable(Screen.FocusMode.route) {
                // Focus mode is a fullscreen timer experience
                TimerScreen(
                    isFocusMode = true,
                    onNavigateToFocusMode = {},
                    onNavigateToTasks = {}
                )
            }
        }
    }
}

@Composable
fun PomodoroBottomBar(navController: NavHostController) {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    // Only show bottom bar on main screens
    val showBottomBar = bottomNavItems.any { it.route == currentDestination?.route }

    if (showBottomBar) {
        NavigationBar {
            bottomNavItems.forEach { screen ->
                val selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true
                NavigationBarItem(
                    icon = {
                        Icon(
                            imageVector = if (selected) screen.selectedIcon else screen.icon,
                            contentDescription = screen.title
                        )
                    },
                    label = { Text(screen.title) },
                    selected = selected,
                    onClick = {
                        navController.navigate(screen.route) {
                            popUpTo(navController.graph.findStartDestination().id) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    }
}
