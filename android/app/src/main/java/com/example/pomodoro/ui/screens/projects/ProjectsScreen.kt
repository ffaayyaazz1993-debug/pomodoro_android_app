package com.example.pomodoro.ui.screens.projects

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
import com.example.pomodoro.data.local.entities.ProjectEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectsScreen(
    onProjectClick: (Long) -> Unit = {}
) {
    val app = PomodoroApplication.instance
    val scope = rememberCoroutineScope()
    val projects by app.projectRepository.getAllActiveProjects().collectAsState(initial = emptyList())
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Projects") },
                actions = {
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Filled.Add, contentDescription = "Create project")
                    }
                }
            )
        }
    ) { padding ->
        if (projects.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Outlined.Folder,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("No projects yet", style = MaterialTheme.typography.bodyLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(onClick = { showCreateDialog = true }) {
                        Text("Create Project")
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(projects, key = { it.id }) { project ->
                    ProjectCard(
                        project = project,
                        onClick = { onProjectClick(project.id) }
                    )
                }
            }
        }
    }

    if (showCreateDialog) {
        CreateProjectDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name, description ->
                scope.launch {
                    app.projectRepository.insert(ProjectEntity(name = name, description = description))
                    showCreateDialog = false
                }
            }
        )
    }
}

@Composable
private fun ProjectCard(
    project: ProjectEntity,
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
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = project.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium
                )
                if (project.description.isNotBlank()) {
                    Text(
                        text = project.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                AssistChip(
                    onClick = {},
                    label = { Text(project.status) },
                    modifier = Modifier.height(24.dp)
                )
            }
            Icon(Icons.Filled.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CreateProjectDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Project") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Project Name") },
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
            }
        },
        confirmButton = {
            Button(
                onClick = { if (name.isNotBlank()) onCreate(name, description) },
                enabled = name.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProjectDetailScreen(
    onNavigateBack: () -> Unit = {}
) {
    val app = PomodoroApplication.instance
    val projects by app.projectRepository.getAllActiveProjects().collectAsState(initial = emptyList())
    val project = projects.firstOrNull()

    if (project == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Project not found")
        }
        return
    }

    val tasks by app.taskRepository.getTasksByProject(project.id).collectAsState(initial = emptyList())

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(project.name) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(project.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        if (project.description.isNotBlank()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(project.description, style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                            Column {
                                Text("${tasks.size}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Tasks", style = MaterialTheme.typography.labelSmall)
                            }
                            Column {
                                Text("${tasks.count { it.status == "COMPLETED" }}", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                                Text("Completed", style = MaterialTheme.typography.labelSmall)
                            }
                        }
                    }
                }
            }
            item {
                Text("Tasks", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            }
            items(tasks, key = { it.id }) { task ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(task.title, style = MaterialTheme.typography.bodyMedium)
                            Text("${task.completedPomodoros}/${task.estimatedPomodoros} 🍅", style = MaterialTheme.typography.labelSmall)
                        }
                        Text(task.status, style = MaterialTheme.typography.labelSmall)
                    }
                }
            }
        }
    }
}
