package com.example.pomodoro.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.service.TimerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val app = context.applicationContext as? PomodoroApplication ?: return
            val scope = CoroutineScope(Dispatchers.Main)

            scope.launch {
                // Restore timer state after boot
                app.timerEngine.restore()
                val state = app.timerEngine.state.value

                if (state.isRunning) {
                    // Check if timer should have completed
                    val remaining = app.timerEngine.computeRemainingMs()
                    if (remaining > 0) {
                        // Timer is still running, restart foreground service
                        TimerForegroundService.start(context)
                    }
                    // If remaining <= 0, restore() already handled completion
                }
            }
        }
    }
}
