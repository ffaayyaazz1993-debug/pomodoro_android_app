package com.example.pomodoro.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.*
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.*
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.providers.AppWidgetProvider
import androidx.glance.layout.*
import androidx.glance.text.*
import com.example.pomodoro.MainActivity
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.domain.timer.TimerPhase
import com.example.pomodoro.domain.timer.TimerState
import com.example.pomodoro.service.receivers.TimerActionReceiver

class PomodoroWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = PomodoroWidget()
}

class PomodoroWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val app = context.applicationContext as PomodoroApplication
        val timerState = app.timerEngine.state.value
        val remainingMs = app.timerEngine.computeRemainingMs()
        val minutes = remainingMs / 60000
        val seconds = (remainingMs % 60000) / 1000

        provideContent {
            PomodoroWidgetContent(
                phase = timerState.phase,
                state = timerState.state,
                timeText = String.format("%02d:%02d", minutes, seconds),
                isRunning = timerState.isRunning
            )
        }
    }
}

@Composable
private fun PomodoroWidgetContent(
    phase: TimerPhase,
    state: TimerState,
    timeText: String,
    isRunning: Boolean
) {
    val phaseText = when (phase) {
        TimerPhase.FOCUS -> "Focus"
        TimerPhase.SHORT_BREAK -> "Short Break"
        TimerPhase.LONG_BREAK -> "Long Break"
        TimerPhase.CUSTOM -> "Custom"
    }

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .padding(16.dp)
            .background(Color(0xFF1A1A2E))
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = phaseText,
            style = TextStyle(
                color = ColorProvider(Color(0xFFE94560)),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        Text(
            text = timeText,
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = 32.sp,
                fontWeight = FontWeight.Light
            )
        )

        Spacer(modifier = GlanceModifier.height(8.dp))

        Row(
            modifier = GlanceModifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isRunning) "▶ Running" else if (state == TimerState.PAUSED) "⏸ Paused" else "⏹ Idle",
                style = TextStyle(
                    color = ColorProvider(Color(0xFF888888)),
                    fontSize = 12.sp
                )
            )
        }
    }
}
