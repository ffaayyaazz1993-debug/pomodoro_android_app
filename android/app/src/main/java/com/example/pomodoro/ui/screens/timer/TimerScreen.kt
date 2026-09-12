package com.example.pomodoro.ui.screens.timer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.domain.timer.TimerPhase
import com.example.pomodoro.domain.timer.TimerState
import com.example.pomodoro.service.TimerForegroundService
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimerScreen(
    isFocusMode: Boolean = false,
    onNavigateToFocusMode: () -> Unit = {},
    onNavigateToTasks: () -> Unit = {}
) {
    val context = LocalContext.current
    val app = context.applicationContext as PomodoroApplication
    val scope = rememberCoroutineScope()
    val timerState by app.timerEngine.state.collectAsState()
    val remainingMs by produceState(initialValue = timerState.plannedDurationMs, timerState) {
        while (true) {
            value = app.timerEngine.computeRemainingMs()
            kotlinx.coroutines.delay(200)
        }
    }
    val progress by produceState(initialValue = 0f, timerState) {
        while (true) {
            value = app.timerEngine.computeProgress()
            kotlinx.coroutines.delay(200)
        }
    }

    val minutes = remainingMs / 60000
    val seconds = (remainingMs % 60000) / 1000
    val timeText = String.format("%02d:%02d", minutes, seconds)

    val phaseColor = when (timerState.phase) {
        TimerPhase.FOCUS -> MaterialTheme.colorScheme.primary
        TimerPhase.SHORT_BREAK -> MaterialTheme.colorScheme.secondary
        TimerPhase.LONG_BREAK -> MaterialTheme.colorScheme.tertiary
        TimerPhase.CUSTOM -> MaterialTheme.colorScheme.primary
    }

    val phaseText = when (timerState.phase) {
        TimerPhase.FOCUS -> "FOCUS"
        TimerPhase.SHORT_BREAK -> "SHORT BREAK"
        TimerPhase.LONG_BREAK -> "LONG BREAK"
        TimerPhase.CUSTOM -> "CUSTOM"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isFocusMode) 32.dp else 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Phase indicator
        if (!isFocusMode) {
            Text(
                text = phaseText,
                style = MaterialTheme.typography.titleMedium,
                color = phaseColor,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Timer ring
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(if (isFocusMode) 320.dp else 280.dp)
        ) {
            // Background circle
            val trackColor = MaterialTheme.colorScheme.surfaceVariant
            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                val strokeWidth = 12.dp.toPx()
                val radius = (size.minDimension - strokeWidth) / 2
                drawArc(
                    color = trackColor,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    ),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
                drawArc(
                    color = phaseColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                    topLeft = Offset(
                        (size.width - radius * 2) / 2,
                        (size.height - radius * 2) / 2
                    ),
                    size = androidx.compose.ui.geometry.Size(radius * 2, radius * 2)
                )
            }

            // Time display
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = timeText,
                    fontSize = if (isFocusMode) 64.sp else 52.sp,
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics {
                        contentDescription = "$phaseText, $minutes minutes ${seconds} seconds remaining"
                    }
                )
                if (isFocusMode) {
                    Text(
                        text = phaseText,
                        style = MaterialTheme.typography.titleMedium,
                        color = phaseColor,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Cycle info
        if (!isFocusMode) {
            val settings = remember { kotlinx.coroutines.runBlocking { app.appPreferences.getTimerSettingsSnapshot() } }
            Text(
                text = "Pomodoro ${timerState.completedPomodorosInCycle + 1} of ${settings.longBreakInterval}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // Primary action button
        val primaryButtonLabel = when (timerState.state) {
            TimerState.IDLE -> "START"
            TimerState.RUNNING -> "PAUSE"
            TimerState.PAUSED -> "RESUME"
            TimerState.COMPLETED -> "NEXT"
            TimerState.SKIPPED, TimerState.CANCELLED -> "START"
        }

        Button(
            onClick = {
                scope.launch {
                    when (timerState.state) {
                        TimerState.IDLE -> {
                            app.timerEngine.start(
                                phase = timerState.phase,
                                taskId = timerState.taskId,
                                projectId = timerState.projectId
                            )
                            TimerForegroundService.start(context)
                        }
                        TimerState.RUNNING -> {
                            app.timerEngine.pause()
                            TimerForegroundService.update(context)
                        }
                        TimerState.PAUSED -> {
                            app.timerEngine.resume()
                            TimerForegroundService.update(context)
                        }
                        TimerState.COMPLETED -> {
                            val nextPhase = app.timerEngine.getNextPhase()
                            app.timerEngine.start(
                                phase = nextPhase,
                                taskId = timerState.taskId,
                                projectId = timerState.projectId
                            )
                            TimerForegroundService.update(context)
                        }
                        TimerState.SKIPPED, TimerState.CANCELLED -> {
                            app.timerEngine.start(
                                phase = timerState.phase,
                                taskId = timerState.taskId,
                                projectId = timerState.projectId
                            )
                            TimerForegroundService.start(context)
                        }
                    }
                }
            },
            modifier = Modifier
                .size(if (isFocusMode) 80.dp else 72.dp),
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(containerColor = phaseColor)
        ) {
            val icon = when (timerState.state) {
                TimerState.IDLE -> Icons.Filled.PlayArrow
                TimerState.RUNNING -> Icons.Filled.Pause
                TimerState.PAUSED -> Icons.Filled.PlayArrow
                TimerState.COMPLETED -> Icons.Filled.SkipNext
                TimerState.SKIPPED, TimerState.CANCELLED -> Icons.Filled.PlayArrow
            }
            Icon(
                imageVector = icon,
                contentDescription = primaryButtonLabel,
                modifier = Modifier.size(if (isFocusMode) 40.dp else 36.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Secondary controls
        if (!isFocusMode && timerState.state != TimerState.IDLE) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Reset
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            app.timerEngine.reset()
                            TimerForegroundService.stop(context)
                        }
                    }
                ) {
                    Icon(Icons.Outlined.RestartAlt, contentDescription = "Reset", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Reset")
                }

                // Skip
                OutlinedButton(
                    onClick = {
                        scope.launch {
                            app.timerEngine.skip()
                            TimerForegroundService.update(context)
                        }
                    }
                ) {
                    Icon(Icons.Outlined.SkipNext, contentDescription = "Skip", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Skip")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Time adjustment
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = {
                    scope.launch { app.timerEngine.subtractTime(1) }
                }) {
                    Text("-1m")
                }
                TextButton(onClick = {
                    scope.launch { app.timerEngine.addTime(1) }
                }) {
                    Text("+1m")
                }
                TextButton(onClick = {
                    scope.launch { app.timerEngine.addTime(5) }
                }) {
                    Text("+5m")
                }
            }
        }

        // Focus mode exit hint
        if (isFocusMode) {
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Swipe back or press back to exit",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun Canvas(modifier: Modifier, onDraw: androidx.compose.ui.graphics.drawscope.DrawScope.() -> Unit) {
    androidx.compose.foundation.Canvas(modifier = modifier, onDraw = onDraw)
}
