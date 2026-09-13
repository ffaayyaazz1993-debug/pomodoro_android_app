package com.example.pomodoro.ui.screens.tasks

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.data.local.entities.TaskEntity
import com.example.pomodoro.domain.timer.MultiTimerManager
import com.example.pomodoro.domain.timer.TimerPhase
import com.example.pomodoro.domain.timer.TimerState
import com.example.pomodoro.service.TimerForegroundService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    onTaskClick: (Long) -> Unit = {}
) {
    val app = PomodoroApplication.instance
    val scope = rememberCoroutineScope()
    val tasks by app.taskRepository.getAllActiveTasks().collectAsState(initial = emptyList())
    val projects by app.projectRepository.getAllActiveProjects().collectAsState(initial = emptyList())
    val allTags by remember { app.taskRepository.getAllTags() }.collectAsState(initial = emptyList())
    val multiTimerManager = app.multiTimerManager
    val activeTimerIds by multiTimerManager.activeTimerIds.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var selectedFilter by remember { mutableStateOf("ALL") }
    var selectedProjectFilter by remember { mutableStateOf<Long?>(null) }
    var selectedTagFilter by remember { mutableStateOf<Long?>(null) }

    val filteredTasks = remember(tasks, searchQuery, selectedFilter, selectedProjectFilter) {
        var result = tasks.filter { !it.isArchived }
        if (searchQuery.isNotBlank()) {
            result = result.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
            }
        }
        if (selectedProjectFilter != null) {
            result = result.filter { it.projectId == selectedProjectFilter }
        }
        when (selectedFilter) {
            "ACTIVE" -> result.filter { it.status == "NOT_STARTED" || it.status == "IN_PROGRESS" }
            "COMPLETED" -> result.filter { it.status == "COMPLETED" }
            "HIGH" -> result.filter { it.priority == "HIGH" || it.priority == "CRITICAL" }
            "IN_PROGRESS" -> result.filter { it.status == "IN_PROGRESS" }
            else -> result
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Tasks") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create task")
                    }
                }
            )
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter chips
            ScrollableTabRow(
                selectedTabIndex = listOf("ALL", "ACTIVE", "IN_PROGRESS", "COMPLETED", "HIGH").indexOf(selectedFilter).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                listOf("ALL", "ACTIVE", "IN_PROGRESS", "COMPLETED", "HIGH").forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = { Text(filter) }
                    )
                }
            }

            // Search
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                placeholder = { Text("Search tasks...") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                singleLine = true,
                trailingIcon = {
                    if (searchQuery.isNotBlank()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Clear")
                        }
                    }
                }
            )

            // Project filter
            if (projects.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedProjectFilter == null,
                        onClick = { selectedProjectFilter = null },
                        label = { Text("All Projects") }
                    )
                    projects.take(3).forEach { project ->
                        FilterChip(
                            selected = selectedProjectFilter == project.id,
                            onClick = {
                                selectedProjectFilter = if (selectedProjectFilter == project.id) null else project.id
                            },
                            label = { Text(project.name, maxLines = 1) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            // Task list
            if (filteredTasks.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.Inbox,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = if (searchQuery.isNotBlank()) "No tasks found" else "No tasks yet",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (searchQuery.isBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Button(onClick = { showCreateDialog = true }) {
                                Text("Create Task")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTasks, key = { it.id }) { task ->
                        TaskListItem(
                            task = task,
                            projects = projects,
                            hasActiveTimer = activeTimerIds.contains(task.id),
                            timerState = if (activeTimerIds.contains(task.id)) {
                                multiTimerManager.getTimerForTask(task.id).state.value
                            } else null,
                            onClick = { onTaskClick(task.id) },
                            onStartTimer = {
                                scope.launch {
                                    multiTimerManager.startTimerForTask(
                                        taskId = task.id,
                                        phase = TimerPhase.FOCUS,
                                        projectId = task.projectId
                                    )
                                    TimerForegroundService.start(app)
                                }
                            },
                            onStopTimer = {
                                scope.launch {
                                    multiTimerManager.stopTimerForTask(task.id)
                                    TimerForegroundService.update(app)
                                }
                            },
                            onComplete = {
                                scope.launch {
                                    app.taskRepository.completeTask(task.id)
                                }
                            },
                            onArchive = {
                                scope.launch {
                                    app.taskRepository.archiveTask(task.id)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Create task dialog
    if (showCreateDialog) {
        CreateTaskDialog(
            projects = projects,
            onDismiss = { showCreateDialog = false },
            onCreate = { title, description, projectId, priority, estimatedPomodoros ->
                scope.launch {
                    app.taskRepository.insert(
                        TaskEntity(
                            title = title,
                            description = description,
                            projectId = projectId,
                            priority = priority,
                            estimatedPomodoros = estimatedPomodoros
                        )
                    )
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
private fun TaskListItem(
    task: TaskEntity,
    projects: List<com.example.pomodoro.data.local.entities.ProjectEntity>,
    hasActiveTimer: Boolean,
    timerState: com.example.pomodoro.domain.timer.TimerSnapshot?,
    onClick: () -> Unit,
    onStartTimer: () -> Unit,
    onStopTimer: () -> Unit,
    onComplete: () -> Unit,
    onArchive: () -> Unit
) {
    val project = projects.find { it.id == task.projectId }
    val isCompleted = task.status == "COMPLETED"

    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = if (hasActiveTimer) {
            CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
            )
        } else if (isCompleted) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
        } else {
            CardDefaults.cardColors()
        }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Checkbox
                Checkbox(
                    checked = isCompleted,
                    onCheckedChange = { if (it) onComplete() }
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        color = if (isCompleted) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                    )
                    if (task.description.isNotBlank()) {
                        Text(
                            text = task.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pomodoro count
                        Text(
                            text = "${task.completedPomodoros}/${task.estimatedPomodoros} 🍅",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        // Priority badge
                        if (task.priority != "MEDIUM") {
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
                        // Project name
                        if (project != null) {
                            Text(
                                text = project.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Timer status / action button
                if (hasActiveTimer && timerState != null) {
                    // Show active timer indicator
                    val remainingMs = timerState.plannedDurationMs // Would need live update
                    val minutes = remainingMs / 60000
                    val seconds = (remainingMs % 60000) / 1000

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Surface(
                            color = MaterialTheme.colorScheme.primary,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier.padding(4.dp)
                        ) {
                            Text(
                                text = String.format("%d:%02d", minutes, seconds),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimary,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        if (timerState.isPaused) {
                            Text("Paused", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    IconButton(onClick = onStopTimer) {
                        Icon(Icons.Filled.Stop, contentDescription = "Stop timer", tint = MaterialTheme.colorScheme.error)
                    }
                } else {
                    // Start timer button
                    IconButton(onClick = onStartTimer) {
                        Icon(
                            Icons.Filled.PlayArrow,
                            contentDescription = "Start timer for this task",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Active timer bar
            if (hasActiveTimer && timerState != null) {
                Spacer(Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = {
                        val elapsed = timerState.plannedDurationMs - (timerState.plannedDurationMs) // simplified
                        0.5f // Would compute from actual remaining
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
private fun CreateTaskDialog(
    projects: List<com.example.pomodoro.data.local.entities.ProjectEntity>,
    onDismiss: () -> Unit,
    onCreate: (String, String, Long?, String, Int) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var selectedPriority by remember { mutableStateOf("MEDIUM") }
    var estimatedPomodoros by remember { mutableIntStateOf(1) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                if (projects.isNotEmpty()) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = it }
                    ) {
                        OutlinedTextField(
                            value = projects.find { it.id == selectedProjectId }?.name ?: "No project",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier.menuAnchor(),
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) }
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("No project") },
                                onClick = {
                                    selectedProjectId = null
                                    expanded = false
                                }
                            )
                            projects.forEach { project ->
                                DropdownMenuItem(
                                    text = { Text(project.name) },
                                    onClick = {
                                        selectedProjectId = project.id
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
                // Priority
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("LOW", "MEDIUM", "HIGH", "CRITICAL").forEach { priority ->
                        FilterChip(
                            selected = selectedPriority == priority,
                            onClick = { selectedPriority = priority },
                            label = { Text(priority) }
                        )
                    }
                }
                // Estimated pomodoros
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Estimated: ", style = MaterialTheme.typography.bodyMedium)
                    IconButton(onClick = { if (estimatedPomodoros > 1) estimatedPomodoros-- }) {
                        Icon(Icons.Filled.Remove, contentDescription = "Decrease")
                    }
                    Text("$estimatedPomodoros 🍅", style = MaterialTheme.typography.titleMedium)
                    IconButton(onClick = { if (estimatedPomodoros < 20) estimatedPomodoros++ }) {
                        Icon(Icons.Filled.Add, contentDescription = "Increase")
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { if (title.isNotBlank()) onCreate(title, description, selectedProjectId, selectedPriority, estimatedPomodoros) },
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
