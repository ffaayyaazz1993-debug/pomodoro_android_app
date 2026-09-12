package com.example.pomodoro.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pomodoro.PomodoroApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? PomodoroApplication ?: return
        val scope = CoroutineScope(Dispatchers.Main)

        scope.launch {
            // Timer alarm fired - check if timer should complete
            val state = app.timerEngine.state.value
            if (state.isRunning) {
                val remaining = app.timerEngine.computeRemainingMs()
                if (remaining <= 0) {
                    app.timerEngine.complete()
                }
            }
        }
    }
}
