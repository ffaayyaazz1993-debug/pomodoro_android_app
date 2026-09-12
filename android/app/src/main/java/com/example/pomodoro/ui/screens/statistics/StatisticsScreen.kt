package com.example.pomodoro.ui.screens.statistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import kotlinx.coroutines.launch
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatisticsScreen() {
    val app = PomodoroApplication.instance
    var selectedPeriod by remember { mutableStateOf("7d") }

    val now = System.currentTimeMillis()
    val calendar = Calendar.getInstance()

    val (startMs, endMs) = when (selectedPeriod) {
        "7d" -> {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            calendar.timeInMillis to now
        }
        "30d" -> {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, -30)
            calendar.timeInMillis to now
        }
        "90d" -> {
            calendar.timeInMillis = now
            calendar.add(Calendar.DAY_OF_YEAR, -90)
            calendar.timeInMillis to now
        }
        else -> {
            calendar.timeInMillis = now
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            calendar.timeInMillis to now
        }
    }

    val pomodoroCount by produceState(initialValue = 0, startMs, endMs) {
        value = app.sessionRepository.countCompletedFocusSessions(startMs, endMs)
    }

    val totalFocusMs by produceState(initialValue = 0L, startMs, endMs) {
        value = app.sessionRepository.sumFocusDurationInRange(startMs, endMs)
    }

    val totalFocusHours = totalFocusMs / 3600000
    val totalFocusMinutes = (totalFocusMs % 3600000) / 60000

    val daysInRange = when (selectedPeriod) {
        "7d" -> 7
        "30d" -> 30
        "90d" -> 90
        else -> 1
    }
    val avgDailyFocus = if (daysInRange > 0) totalFocusMs / daysInRange else 0L
    val avgDailyMinutes = avgDailyFocus / 60000

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Statistics") })
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Period selector
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("7d" to "7 Days", "30d" to "30 Days", "90d" to "90 Days").forEach { (key, label) ->
                        FilterChip(
                            selected = selectedPeriod == key,
                            onClick = { selectedPeriod = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            // Summary cards
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Whatshot,
                        value = "$pomodoroCount",
                        label = "Pomodoros"
                    )
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.Schedule,
                        value = "${totalFocusHours}h ${totalFocusMinutes}m",
                        label = "Focus Time"
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.TrendingUp,
                        value = "${avgDailyMinutes}m",
                        label = "Avg Daily Focus"
                    )
                    StatSummaryCard(
                        modifier = Modifier.weight(1f),
                        icon = Icons.Filled.CalendarToday,
                        value = "$daysInRange",
                        label = "Days"
                    )
                }
            }

            // Simple bar chart visualization
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Focus Time", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        // Generate daily data for visualization
                        val dailyData = remember(startMs, endMs) {
                            val cal = Calendar.getInstance()
                            cal.timeInMillis = startMs
                            val data = mutableListOf<Pair<String, Long>>()
                            while (cal.timeInMillis < endMs) {
                                val dayStart = cal.timeInMillis
                                cal.add(Calendar.DAY_OF_YEAR, 1)
                                val dayEnd = cal.timeInMillis.coerceAtMost(endMs)
                                val dayLabel = java.text.SimpleDateFormat("EEE", Locale.getDefault()).format(Date(dayStart))
                                data.add(dayLabel to (dayEnd - dayStart)) // Placeholder
                                if (cal.timeInMillis >= endMs) break
                            }
                            data
                        }

                        // Simple bar representation
                        dailyData.takeLast(7).forEach { (label, _) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelSmall,
                                    modifier = Modifier.width(32.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                // Placeholder bar - in production, use actual data
                                LinearProgressIndicator(
                                    progress = { 0.5f },
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(16.dp),
                                )
                            }
                        }
                    }
                }
            }

            // Insights
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Insights", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(12.dp))
                        InsightItem(
                            icon = Icons.Outlined.Insights,
                            text = "You completed $pomodoroCount focus sessions in this period."
                        )
                        InsightItem(
                            icon = Icons.Outlined.Timer,
                            text = "Total focus time: ${totalFocusHours}h ${totalFocusMinutes}m."
                        )
                        if (avgDailyMinutes > 0) {
                            InsightItem(
                                icon = Icons.Outlined.TrendingUp,
                                text = "Average daily focus: ${avgDailyMinutes} minutes."
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatSummaryCard(
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
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(8.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun InsightItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    text: String
) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
