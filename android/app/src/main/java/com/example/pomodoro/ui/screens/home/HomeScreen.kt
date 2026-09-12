package com.example.pomodoro.ui.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.domain.timer.TimerPhase
import com.example.pomodoro.domain.timer.TimerState
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onNavigateToTimer: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {},
    onNavigateToTaskDetail: (Long) -> Unit = {},
    onNavigateToStatistics: () -> Unit = {}
) {
    val app = PomodoroApplication.instance
    val timerState by app.timerEngine.state.collectAsState()
    val tasks by app.taskRepository.getAllActiveTasks().collectAsState(initial = emptyList())
    val projects by app.projectRepository.getAllActiveProjects().collectAsState(initial = emptyList())

    val todayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val todayEnd = todayStart + 24 * 60 * 60 * 1000L

    val todayPomodoros by produceState(initialValue = 0) {
        value = app.sessionRepository.countCompletedFocusSessions(todayStart, todayEnd)
    }

    val todayFocusMs by produceState(initialValue = 0L) {
        value = app.sessionRepository.sumFocusDurationInRange(todayStart, todayEnd)
    }

    val todayFocusMinutes = todayFocusMs / 60000

    val remainingMs by produceState(initialValue = timerState.plannedDurationMs, timerState) {
        while (true) {
            value = app.timerEngine.computeRemainingMs()
            kotlinx.coroutines.delay(1000)
        }
    }

    val minutes = remainingMs / 60000
    val seconds = (remainingMs % 60000) / 1000

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        item {
            Text(
                text = "Good ${getGreeting()}",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault()).format(Date()),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Timer card
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when (timerState.phase) {
                        TimerPhase.FOCUS -> MaterialTheme.colorScheme.primaryContainer
                        TimerPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondaryContainer
                        TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiaryContainer
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                ),
                onClick = onNavigateToTimer
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = when (timerState.phase) {
                            TimerPhase.FOCUS -> "🎯 Focus"
                            TimerPhase.SHORT_BREAK -> "☕ Short Break"
                            TimerPhase.LONG_BREAK -> "🌴 Long Break"
                            else -> "Ready"
                        },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = String.format("%02d:%02d", minutes, seconds),
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Light
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = when (timerState.state) {
                            TimerState.IDLE -> "Tap to start"
                            TimerState.RUNNING -> "In progress..."
                            TimerState.PAUSED -> "Paused"
                            TimerState.COMPLETED -> "Complete! Tap for next"
                            else -> ""
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Stats row
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Whatshot,
                    value = "$todayPomodoros",
                    label = "Pomodoros"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.Schedule,
                    value = "${todayFocusMinutes}m",
                    label = "Focus Time"
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    icon = Icons.Filled.TaskAlt,
                    value = "${tasks.count { it.status == "COMPLETED" }}",
                    label = "Tasks Done"
                )
            }
        }

        // Active tasks
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Active Tasks",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = onNavigateToTasks) {
                    Text("See all")
                }
            }
        }

        val activeTasks = tasks.filter { it.status != "COMPLETED" && it.status != "ARCHIVED" }.take(5)
        items(activeTasks, key = { it.id }) { task ->
            TaskCard(
                task = task,
                onClick = { onNavigateToTaskDetail(task.id) }
            )
        }

        if (activeTasks.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Outlined.TaskAlt,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No active tasks",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        TextButton(onClick = onNavigateToTasks) {
                            Text("Create a task")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun TaskCard(
    task: com.example.pomodoro.data.local.entities.TaskEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium
                )
                if (task.description.isNotBlank()) {
                    Text(
                        text = task.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "${task.completedPomodoros}/${task.estimatedPomodoros} 🍅",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (task.priority != "MEDIUM") {
                        Spacer(modifier = Modifier.width(8.dp))
                        val priorityColor = when (task.priority) {
                            "HIGH" -> MaterialTheme.colorScheme.error
                            "CRITICAL" -> MaterialTheme.colorScheme.error
                            "LOW" -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                        Text(
                            text = task.priority,
                            style = MaterialTheme.typography.labelSmall,
                            color = priorityColor
                        )
                    }
                }
            }
            Icon(
                Icons.Filled.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun getGreeting(): String {
    val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "morning"
        hour < 17 -> "afternoon"
        else -> "evening"
    }
}
