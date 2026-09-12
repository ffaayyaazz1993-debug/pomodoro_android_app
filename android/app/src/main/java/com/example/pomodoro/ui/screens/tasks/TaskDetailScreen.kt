package com.example.pomodoro.ui.screens.tasks

import androidx.compose.foundation.layout.*
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
import com.example.pomodoro.service.TimerForegroundService
import com.example.pomodoro.domain.timer.TimerPhase
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    onNavigateBack: () -> Unit = {}
) {
    val app = PomodoroApplication.instance
    val scope = rememberCoroutineScope()

    // Get task ID from navigation (simplified - in production, use SavedStateHandle)
    // For now, show a placeholder that demonstrates the screen
    val tasks by app.taskRepository.getAllActiveTasks().collectAsState(initial = emptyList())
    val task = tasks.firstOrNull()

    if (task == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Task not found")
        }
        return
    }

    val projects by app.projectRepository.getAllActiveProjects().collectAsState(initial = emptyList())
    val project = projects.find { it.id == task.projectId }
    val sessions by app.sessionRepository.getSessionsByTask(task.id).collectAsState(initial = emptyList())

    var showEditDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(task.title) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showEditDialog = true }) {
                        Icon(Icons.Outlined.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = {
                        scope.launch {
                            app.taskRepository.completeTask(task.id)
                            onNavigateBack()
                        }
                    }) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = "Complete")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Status & Priority
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                AssistChip(
                    onClick = {},
                    label = { Text(task.status) },
                    leadingIcon = {
                        Icon(
                            when (task.status) {
                                "COMPLETED" -> Icons.Filled.CheckCircle
                                "IN_PROGRESS" -> Icons.Filled.PlayCircle
                                "CANCELLED" -> Icons.Filled.Cancel
                                else -> Icons.Outlined.RadioButtonUnchecked
                            },
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                )
                AssistChip(
                    onClick = {},
                    label = { Text(task.priority) },
                    colors = AssistChipDefaults.assistChipColors(
                        labelColor = when (task.priority) {
                            "HIGH", "CRITICAL" -> MaterialTheme.colorScheme.error
                            "LOW" -> MaterialTheme.colorScheme.outline
                            else -> MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                )
                if (project != null) {
                    AssistChip(
                        onClick = {},
                        label = { Text(project.name) },
                        leadingIcon = {
                            Icon(Icons.Outlined.Folder, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                    )
                }
            }

            // Description
            if (task.description.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Description", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(task.description, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Pomodoro progress
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Pomodoro Progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LinearProgressIndicator(
                            progress = { task.completedPomodoros.toFloat() / task.estimatedPomodoros.toFloat() },
                            modifier = Modifier
                                .weight(1f)
                                .height(8.dp),
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            "${task.completedPomodoros}/${task.estimatedPomodoros}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            scope.launch {
                                app.timerEngine.start(
                                    phase = TimerPhase.FOCUS,
                                    taskId = task.id,
                                    projectId = task.projectId
                                )
                                TimerForegroundService.start(PomodoroApplication.instance)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Pomodoro")
                    }
                }
            }

            // Notes
            if (task.notes.isNotBlank()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Notes", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(task.notes, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            // Session history
            if (sessions.isNotEmpty()) {
                Text("Session History", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                sessions.take(10).forEach { session ->
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(session.phase, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${session.actualDurationMs / 60000}m • ${session.status}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                java.text.SimpleDateFormat("MMM d, HH:mm", java.util.Locale.getDefault())
                                    .format(java.util.Date(session.startTime)),
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
            }
        }
    }
}
