package com.example.pomodoro.ui.screens.tasks

import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.domain.timer.TimerPhase
import com.example.pomodoro.domain.timer.TimerState
import com.example.pomodoro.service.TimerForegroundService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailScreen(
    taskId: Long = 0L,
    onNavigateBack: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = PomodoroApplication.instance
    val scope = rememberCoroutineScope()
    val multiTimerManager = app.multiTimerManager

    val tasks by app.taskRepository.getAllActiveTasks().collectAsState(initial = emptyList())
    val task = tasks.find { it.id == taskId } ?: tasks.firstOrNull()

    if (task == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Task not found")
        }
        return
    }

    val projects by app.projectRepository.getAllActiveProjects().collectAsState(initial = emptyList())
    val project = projects.find { it.id == task.projectId }
    val sessions by app.sessionRepository.getSessionsByTask(task.id).collectAsState(initial = emptyList())
    val taskTags by app.taskRepository.getTagsForTask(task.id).collectAsState(initial = emptyList())
    val allTags by app.taskRepository.getAllTags().collectAsState(initial = emptyList())

    // Get this task's dedicated timer
    val taskTimer = remember(task.id) { multiTimerManager.getTimerForTask(task.id) }
    val timerState by taskTimer.state.collectAsState()
    val hasActiveTimer = timerState.isRunning || timerState.isPaused

    // Live-updating remaining time for this task's timer
    val remainingMs by produceState(initialValue = taskTimer.computeRemainingMs(), timerState) {
        while (true) {
            value = taskTimer.computeRemainingMs()
            kotlinx.coroutines.delay(500)
        }
    }
    val progress by produceState(initialValue = 0f, timerState) {
        while (true) {
            value = taskTimer.computeProgress()
            kotlinx.coroutines.delay(500)
        }
    }

    val minutes = remainingMs / 60000
    val seconds = (remainingMs % 60000) / 1000

    var showEditDialog by remember { mutableStateOf(false) }
    var showTagDialog by remember { mutableStateOf(false) }

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
                    IconButton(onClick = { showTagDialog = true }) {
                        Icon(Icons.Outlined.Label, contentDescription = "Tags")
                    }
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
            // === TASK'S OWN TIMER ===
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = when {
                        timerState.isRunning -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        timerState.isPaused -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
                        else -> MaterialTheme.colorScheme.surfaceVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "🍅 This Task's Timer",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    // Timer ring
                    Box(
                        modifier = Modifier.size(160.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 10.dp.toPx()
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
                                color = MaterialTheme.colorScheme.primary,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = strokeWidth, cap = StrokeCap.Round),
                                topLeft = topLeft,
                                size = arcSize
                            )
                        }

                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = String.format("%02d:%02d", minutes, seconds),
                                fontSize = 36.sp,
                                fontWeight = FontWeight.Light
                            )
                            Text(
                                text = when (timerState.phase) {
                                    TimerPhase.FOCUS -> "Focus"
                                    TimerPhase.SHORT_BREAK -> "Break"
                                    TimerPhase.LONG_BREAK -> "Long Break"
                                    else -> ""
                                },
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // Timer controls
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            timerState.isRunning -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            taskTimer.pause()
                                            TimerForegroundService.update(context)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.Pause, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Pause")
                                }
                            }
                            timerState.isPaused -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            taskTimer.resume()
                                            TimerForegroundService.update(context)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Resume")
                                }
                            }
                            timerState.isCompleted -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val nextPhase = taskTimer.getNextPhase()
                                            taskTimer.start(
                                                phase = nextPhase,
                                                taskId = task.id,
                                                projectId = task.projectId
                                            )
                                            TimerForegroundService.update(context)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.SkipNext, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Next")
                                }
                            }
                            else -> {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            taskTimer.start(
                                                phase = TimerPhase.FOCUS,
                                                taskId = task.id,
                                                projectId = task.projectId
                                            )
                                            TimerForegroundService.start(context)
                                        }
                                    }
                                ) {
                                    Icon(Icons.Filled.PlayArrow, contentDescription = null)
                                    Spacer(Modifier.width(4.dp))
                                    Text("Start Focus")
                                }
                            }
                        }

                        if (hasActiveTimer) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        taskTimer.stop()
                                        TimerForegroundService.update(context)
                                    }
                                }
                            ) {
                                Text("Stop")
                            }
                        }
                    }

                    // Time adjustment
                    if (hasActiveTimer) {
                        Spacer(Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            TextButton(onClick = { scope.launch { taskTimer.subtractTime(1) } }) {
                                Text("-1m")
                            }
                            TextButton(onClick = { scope.launch { taskTimer.addTime(1) } }) {
                                Text("+1m")
                            }
                            TextButton(onClick = { scope.launch { taskTimer.addTime(5) } }) {
                                Text("+5m")
                            }
                            TextButton(onClick = { scope.launch { taskTimer.skip() } }) {
                                Text("Skip")
                            }
                        }
                    }

                    // Pomodoro count for this task
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${task.completedPomodoros}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text("Completed", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${task.estimatedPomodoros}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Text("Estimated", style = MaterialTheme.typography.labelSmall)
                        }
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "${task.estimatedPomodoros - task.completedPomodoros}",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.tertiary
                            )
                            Text("Remaining", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }

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

            // Tags
            if (taskTags.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Outlined.Label, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    taskTags.forEach { tag ->
                        SuggestionChip(
                            onClick = {},
                            label = { Text(tag.name, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
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

            // Pomodoro progress bar
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Progress", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { task.completedPomodoros.toFloat() / task.estimatedPomodoros.toFloat().coerceAtLeast(1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${task.completedPomodoros} of ${task.estimatedPomodoros} Pomodoros",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
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

    // Tag management dialog
    if (showTagDialog) {
        TagManagementDialog(
            taskTags = taskTags,
            allTags = allTags,
            onDismiss = { showTagDialog = false },
            onAddTag = { tagId ->
                scope.launch {
                    app.taskRepository.addTagToTask(task.id, tagId)
                }
            },
            onRemoveTag = { tagId ->
                scope.launch {
                    app.taskRepository.removeTagFromTask(task.id, tagId)
                }
            },
            onCreateTag = { name ->
                scope.launch {
                    val id = app.taskRepository.createTag(name)
                    if (id != null && id > 0) {
                        app.taskRepository.addTagToTask(task.id, id)
                    }
                }
            }
        )
    }
}

@Composable
private fun TagManagementDialog(
    taskTags: List<com.example.pomodoro.data.local.entities.TagEntity>,
    allTags: List<com.example.pomodoro.data.local.entities.TagEntity>,
    onDismiss: () -> Unit,
    onAddTag: (Long) -> Unit,
    onRemoveTag: (Long) -> Unit,
    onCreateTag: (String) -> Unit
) {
    var newTagName by remember { mutableStateOf("") }
    val taskTagIds = taskTags.map { it.id }.toSet()
    val availableTags = allTags.filter { it.id !in taskTagIds }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Tags") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                // Current tags
                if (taskTags.isNotEmpty()) {
                    Text("Current Tags", style = MaterialTheme.typography.labelMedium)
                    taskTags.forEach { tag ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(tag.name, style = MaterialTheme.typography.bodyMedium)
                            IconButton(onClick = { onRemoveTag(tag.id) }) {
                                Icon(Icons.Filled.Close, contentDescription = "Remove tag", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }

                // Add existing tags
                if (availableTags.isNotEmpty()) {
                    Text("Add Tag", style = MaterialTheme.typography.labelMedium)
                    availableTags.forEach { tag ->
                        TextButton(
                            onClick = { onAddTag(tag.id) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(tag.name)
                        }
                    }
                }

                // Create new tag
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = newTagName,
                        onValueChange = { newTagName = it },
                        label = { Text("New tag") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    IconButton(
                        onClick = {
                            if (newTagName.isNotBlank()) {
                                onCreateTag(newTagName.trim())
                                newTagName = ""
                            }
                        },
                        enabled = newTagName.isNotBlank()
                    ) {
                        Icon(Icons.Filled.Add, contentDescription = "Create tag")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}
