package com.example.pomodoro.ui.screens.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.domain.timer.*
import com.example.pomodoro.service.TimerForegroundService
import kotlinx.coroutines.launch

/**
 * Screen showing all active per-task timers simultaneously.
 * Each task has its own independent timer that runs concurrently.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MultiTimerScreen(
    isFocusMode: Boolean = false,
    onNavigateToTask: (Long) -> Unit = {},
    onNavigateToTasks: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as PomodoroApplication
    val scope = rememberCoroutineScope()
    val multiTimerManager = app.multiTimerManager

    val activeTimerIds by multiTimerManager.activeTimerIds.collectAsState()
    val tasks by app.taskRepository.getAllActiveTasks().collectAsState(initial = emptyList())

    // Show active timers
    val activeTimers = remember(activeTimerIds) {
        multiTimerManager.getActiveTimers()
    }

    // Current selected timer for expanded view
    var selectedTaskId by remember { mutableStateOf<Long?>(null) }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Top bar
        if (!isFocusMode) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "Active Timers",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "${activeTimers.size} running",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    // Start new timer button
                    FilledTonalButton(onClick = onNavigateToTasks) {
                        Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                }
            }
        }

        if (activeTimers.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        Icons.Outlined.Timer,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "No Active Timers",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        "Start a Pomodoro for a task to see it here.\nEach task runs its own independent timer.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = onNavigateToTasks) {
                        Icon(Icons.Filled.Add, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Start Timer for Task")
                    }
                }
            }
        } else {
            // List of active timers
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(activeTimers, key = { it.first }) { (taskId, engine) ->
                    val task = tasks.find { it.id == taskId }
                    val timerState by engine.state.collectAsState()

                    // Live-updating remaining time
                    val remainingMs by produceState(initialValue = engine.computeRemainingMs(), timerState) {
                        while (true) {
                            value = engine.computeRemainingMs()
                            kotlinx.coroutines.delay(500)
                        }
                    }

                    val progress by produceState(initialValue = 0f, timerState) {
                        while (true) {
                            value = engine.computeProgress()
                            kotlinx.coroutines.delay(500)
                        }
                    }

                    ActiveTimerCard(
                        taskId = taskId,
                        taskTitle = task?.title ?: "Task #$taskId",
                        taskDescription = task?.description ?: "",
                        projectName = task?.projectId?.let { pid ->
                            // Would need project lookup
                            null
                        },
                        timerState = timerState,
                        remainingMs = remainingMs,
                        progress = progress,
                        isExpanded = selectedTaskId == taskId,
                        onExpand = {
                            selectedTaskId = if (selectedTaskId == taskId) null else taskId
                        },
                        onPause = {
                            scope.launch {
                                multiTimerManager.pauseTimerForTask(taskId)
                                TimerForegroundService.update(context)
                            }
                        },
                        onResume = {
                            scope.launch {
                                multiTimerManager.resumeTimerForTask(taskId)
                                TimerForegroundService.update(context)
                            }
                        },
                        onStop = {
                            scope.launch {
                                multiTimerManager.stopTimerForTask(taskId)
                                TimerForegroundService.update(context)
                            }
                        },
                        onSkip = {
                            scope.launch {
                                engine.skip()
                                TimerForegroundService.update(context)
                            }
                        },
                        onComplete = {
                            scope.launch {
                                multiTimerManager.completeTimerForTask(taskId)
                                TimerForegroundService.update(context)
                            }
                        },
                        onAddTime = { mins ->
                            scope.launch { engine.addTime(mins) }
                        },
                        onNavigateToTask = { onNavigateToTask(taskId) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ActiveTimerCard(
    taskId: Long,
    taskTitle: String,
    taskDescription: String,
    projectName: String?,
    timerState: TimerSnapshot,
    remainingMs: Long,
    progress: Float,
    isExpanded: Boolean,
    onExpand: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onStop: () -> Unit,
    onSkip: () -> Unit,
    onComplete: () -> Unit,
    onAddTime: (Int) -> Unit,
    onNavigateToTask: () -> Unit
) {
    val minutes = remainingMs / 60000
    val seconds = (remainingMs % 60000) / 1000
    val timeText = String.format("%02d:%02d", minutes, seconds)

    val phaseColor = when (timerState.phase) {
        TimerPhase.FOCUS -> MaterialTheme.colorScheme.primary
        TimerPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
        TimerPhase.CUSTOM -> MaterialTheme.colorScheme.primary
    }

    val phaseLabel = when (timerState.phase) {
        TimerPhase.FOCUS -> "Focus"
        TimerPhase.SHORT_BREAK -> "Short Break"
        TimerPhase.LONG_BREAK -> "Long Break"
        TimerPhase.CUSTOM -> "Custom"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (timerState.isRunning) {
                phaseColor.copy(alpha = 0.08f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Phase indicator dot
                Surface(
                    modifier = Modifier.size(10.dp),
                    shape = CircleShape,
                    color = phaseColor
                ) {}

                Spacer(Modifier.width(12.dp))

                // Task info
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = taskTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = phaseLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = phaseColor
                        )
                        if (projectName != null) {
                            Text(
                                text = "• $projectName",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Status badge
                        val statusText = when (timerState.state) {
                            TimerState.RUNNING -> "▶ Running"
                            TimerState.PAUSED -> "⏸ Paused"
                            TimerState.COMPLETED -> "✓ Done"
                            else -> ""
                        }
                        if (statusText.isNotEmpty()) {
                            Text(
                                text = statusText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Expand button
                IconButton(onClick = onExpand) {
                    Icon(
                        if (isExpanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = if (isExpanded) "Collapse" else "Expand"
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Timer display row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Circular progress
                Box(
                    modifier = Modifier.size(64.dp),
                    contentAlignment = Alignment.Center
                ) {
                    androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 6.dp.toPx()
                        val radius = (size.minDimension - strokeWidth) / 2
                        val topLeft = androidx.compose.ui.geometry.Offset(
                            (size.width - radius * 2) / 2,
                            (size.height - radius * 2) / 2
                        )
                        val arcSize = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)

                        // Track
                        drawArc(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            startAngle = -90f,
                            sweepAngle = 360f,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            topLeft = topLeft,
                            size = arcSize
                        )
                        // Progress
                        drawArc(
                            color = phaseColor,
                            startAngle = -90f,
                            sweepAngle = 360f * progress,
                            useCenter = false,
                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round),
                            topLeft = topLeft,
                            size = arcSize
                        )
                    }
                    Text(
                        text = timeText,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.semantics {
                            contentDescription = "$phaseLabel for $taskTitle, $minutes minutes $seconds seconds remaining"
                        }
                    )
                }

                Spacer(Modifier.width(16.dp))

                // Controls
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Main control button
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (timerState.isRunning) {
                            FilledTonalButton(
                                onClick = onPause,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.Pause, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Pause")
                            }
                        } else if (timerState.isPaused) {
                            Button(
                                onClick = onResume,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Resume")
                            }
                        } else if (timerState.isCompleted) {
                            Button(
                                onClick = onComplete,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Filled.SkipNext, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Next")
                            }
                        }

                        IconButton(
                            onClick = onStop,
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer,
                                contentColor = MaterialTheme.colorScheme.onErrorContainer
                            )
                        ) {
                            Icon(Icons.Filled.Stop, contentDescription = "Stop")
                        }
                    }

                    // Secondary controls
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        TextButton(
                            onClick = { onAddTime(1) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+1m", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = { onAddTime(5) },
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("+5m", style = MaterialTheme.typography.labelSmall)
                        }
                        TextButton(
                            onClick = onSkip,
                            modifier = Modifier.weight(1f),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                        ) {
                            Text("Skip", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

            // Expanded details
            if (isExpanded) {
                Spacer(Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Pomodoro", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${timerState.completedPomodorosInCycle + 1}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("Total", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${timerState.totalCompletedSessions}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Column {
                        Text("Planned", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            "${timerState.plannedDurationMs / 60000}m",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onNavigateToTask,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.OpenInNew, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Open Task Details")
                }
            }
        }
    }
}
