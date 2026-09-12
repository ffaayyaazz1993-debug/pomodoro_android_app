package com.example.pomodoro.service.receivers

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.service.TimerForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TimerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as PomodoroApplication
        val scope = CoroutineScope(Dispatchers.Main)

        when (intent.action) {
            ACTION_PAUSE -> scope.launch {
                app.timerEngine.pause()
                TimerForegroundService.update(context)
            }
            ACTION_RESUME -> scope.launch {
                app.timerEngine.resume()
                TimerForegroundService.update(context)
            }
            ACTION_SKIP -> scope.launch {
                app.timerEngine.skip()
                TimerForegroundService.update(context)
            }
            ACTION_STOP -> scope.launch {
                app.timerEngine.stop()
                TimerForegroundService.stop(context)
            }
        }
    }

    companion object {
        const val ACTION_PAUSE = "com.example.pomodoro.action.PAUSE"
        const val ACTION_RESUME = "com.example.pomodoro.action.RESUME"
        const val ACTION_SKIP = "com.example.pomodoro.action.SKIP"
        const val ACTION_STOP = "com.example.pomodoro.action.STOP"
    }
}
