package com.example.pomodoro.ui.screens.goals

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
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalsScreen() {
    val app = PomodoroApplication.instance
    val goals by app.goalRepository.getActiveGoals().collectAsState(initial = emptyList())

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

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Goals") })
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Daily Pomodoro Goal
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Whatshot, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daily Pomodoros", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "$todayPomodoros",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            " / 8",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = { (todayPomodoros.toFloat() / 8f).coerceAtMost(1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                    if (todayPomodoros >= 8) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("🏆 Goal achieved!", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            // Daily Focus Time Goal
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Schedule, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Daily Focus Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    val focusHours = todayFocusMs / 3600000
                    val focusMinutes = (todayFocusMs % 3600000) / 60000
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            "${focusHours}h ${focusMinutes}m",
                            style = MaterialTheme.typography.displaySmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                        Text(
                            " / 4h 0m",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 4.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    val goalMs = 4 * 3600000L
                    LinearProgressIndicator(
                        progress = { (todayFocusMs.toFloat() / goalMs.toFloat()).coerceAtMost(1f) },
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                    )
                }
            }

            // Streak info
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.LocalFireDepartment, contentDescription = null, tint = MaterialTheme.colorScheme.onTertiaryContainer)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Streak", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        "0 days",
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Complete a Pomodoro every day to build your streak!",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen() {
    val app = PomodoroApplication.instance
    val sessions by app.sessionRepository.getAllSessions().collectAsState(initial = emptyList())

    val calendar = remember { Calendar.getInstance() }
    val currentMonth = remember { mutableIntStateOf(calendar.get(Calendar.MONTH)) }
    val currentYear = remember { mutableIntStateOf(calendar.get(Calendar.YEAR)) }

    val monthNames = listOf("January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("${monthNames[currentMonth.intValue]} ${currentYear.intValue}") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (currentMonth.intValue == 0) {
                            currentMonth.intValue = 11
                            currentYear.intValue--
                        } else {
                            currentMonth.intValue--
                        }
                    }) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous month")
                    }
                },
                actions = {
                    IconButton(onClick = {
                        if (currentMonth.intValue == 11) {
                            currentMonth.intValue = 0
                            currentYear.intValue++
                        } else {
                            currentMonth.intValue++
                        }
                    }) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next month")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp)
        ) {
            // Day headers
            Row(modifier = Modifier.fillMaxWidth()) {
                listOf("Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat").forEach { day ->
                    Text(
                        text = day,
                        modifier = Modifier.weight(1f),
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Calendar grid (simplified)
            val cal = Calendar.getInstance()
            cal.set(currentYear.intValue, currentMonth.intValue, 1)
            val firstDayOfWeek = cal.get(Calendar.DAY_OF_WEEK) - 1
            val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

            var dayCounter = 1
            for (week in 0..5) {
                if (dayCounter > daysInMonth) break
                Row(modifier = Modifier.fillMaxWidth()) {
                    for (dayOfWeek in 0..6) {
                        if ((week == 0 && dayOfWeek < firstDayOfWeek) || dayCounter > daysInMonth) {
                            Spacer(modifier = Modifier.weight(1f).aspectRatio(1f))
                        } else {
                            val day = dayCounter
                            val dayStart = Calendar.getInstance().apply {
                                set(currentYear.intValue, currentMonth.intValue, day, 0, 0, 0)
                                set(Calendar.MILLISECOND, 0)
                            }.timeInMillis
                            val dayEnd = dayStart + 24 * 60 * 60 * 1000L
                            val daySessions = sessions.filter { it.startTime in dayStart until dayEnd && it.phase == "FOCUS" && it.status == "COMPLETED" }

                            Card(
                                modifier = Modifier
                                    .weight(1f)
                                    .aspectRatio(1f)
                                    .padding(2.dp),
                                colors = if (daySessions.isNotEmpty()) {
                                    CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                                } else {
                                    CardDefaults.cardColors()
                                }
                            ) {
                                Box(
                                    modifier = Modifier.fillMaxSize(),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("$day", style = MaterialTheme.typography.bodySmall)
                                        if (daySessions.isNotEmpty()) {
                                            Text(
                                                "${daySessions.size}🍅",
                                                style = MaterialTheme.typography.labelSmall,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }
                                }
                            }
                            dayCounter++
                        }
                    }
                }
            }
        }
    }
}
