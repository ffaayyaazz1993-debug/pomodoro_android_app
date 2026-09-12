package com.example.pomodoro.ui.screens.history

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
import com.example.pomodoro.data.local.entities.SessionEntity
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen() {
    val app = PomodoroApplication.instance
    val sessions by app.sessionRepository.getAllSessions().collectAsState(initial = emptyList())
    var selectedFilter by remember { mutableStateOf("ALL") }
    var searchQuery by remember { mutableStateOf("") }

    val filteredSessions = remember(sessions, selectedFilter) {
        when (selectedFilter) {
            "FOCUS" -> sessions.filter { it.phase == "FOCUS" }
            "BREAK" -> sessions.filter { it.phase == "SHORT_BREAK" || it.phase == "LONG_BREAK" }
            "COMPLETED" -> sessions.filter { it.status == "COMPLETED" }
            "CANCELLED" -> sessions.filter { it.status == "CANCELLED" || it.status == "SKIPPED" }
            else -> sessions
        }
    }

    val sdf = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // Group sessions by date
    val groupedSessions = remember(filteredSessions) {
        filteredSessions.groupBy { session ->
            sdf.format(Date(session.startTime))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("History") })
        }
    ) { padding ->
        Column(modifier = Modifier.padding(padding)) {
            // Filter chips
            ScrollableTabRow(
                selectedTabIndex = listOf("ALL", "FOCUS", "BREAK", "COMPLETED", "CANCELLED").indexOf(selectedFilter).coerceAtLeast(0),
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 16.dp
            ) {
                listOf("ALL", "FOCUS", "BREAK", "COMPLETED", "CANCELLED").forEach { filter ->
                    Tab(
                        selected = selectedFilter == filter,
                        onClick = { selectedFilter = filter },
                        text = { Text(filter) }
                    )
                }
            }

            if (filteredSessions.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Outlined.History,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No sessions yet", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Start a Pomodoro to see your history here", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    groupedSessions.forEach { (date, daySessions) ->
                        item {
                            Text(
                                text = date,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        items(daySessions, key = { it.id }) { session ->
                            SessionListItem(session = session, timeFormat = timeFormat)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionListItem(
    session: SessionEntity,
    timeFormat: SimpleDateFormat
) {
    val durationMinutes = session.actualDurationMs / 60000
    val phaseEmoji = when (session.phase) {
        "FOCUS" -> "🎯"
        "SHORT_BREAK" -> "☕"
        "LONG_BREAK" -> "🌴"
        else -> "⏱️"
    }
    val statusColor = when (session.status) {
        "COMPLETED" -> MaterialTheme.colorScheme.primary
        "CANCELLED" -> MaterialTheme.colorScheme.error
        "SKIPPED" -> MaterialTheme.colorScheme.outline
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(phaseEmoji, style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = session.phase.replace("_", " "),
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "${durationMinutes}m",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = session.status,
                        style = MaterialTheme.typography.labelSmall,
                        color = statusColor
                    )
                    if (session.interruptionCount > 0) {
                        Text(
                            text = "${session.interruptionCount} interruptions",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
            Text(
                text = timeFormat.format(Date(session.startTime)),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
