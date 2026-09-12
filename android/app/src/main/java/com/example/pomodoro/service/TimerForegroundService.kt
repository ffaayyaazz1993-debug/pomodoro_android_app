package com.example.pomodoro.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.R
import com.example.pomodoro.domain.timer.TimerPhase
import com.example.pomodoro.domain.timer.TimerState
import com.example.pomodoro.service.receivers.TimerActionReceiver

class TimerForegroundService : Service() {

    private lateinit var wakeLock: PowerManager.WakeLock
    private var isServiceStarted = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "PomodoroFocus::TimerWakeLock"
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> {
                startForeground(NOTIFICATION_ID, buildNotification())
                if (!wakeLock.isHeld) {
                    wakeLock.acquire(25 * 60 * 1000L) // Max 25 min
                }
                isServiceStarted = true
            }
            ACTION_STOP -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                if (wakeLock.isHeld) {
                    wakeLock.release()
                }
                isServiceStarted = false
                stopSelf()
            }
            ACTION_UPDATE -> {
                updateNotification()
            }
        }
        return START_STICKY
    }

    private fun buildNotification(): Notification {
        val app = application as PomodoroApplication
        val timerState = app.timerEngine.state.value

        val title = when (timerState.phase) {
            TimerPhase.FOCUS -> "🎯 Focus"
            TimerPhase.SHORT_BREAK -> "☕ Short Break"
            TimerPhase.LONG_BREAK -> "🌴 Long Break"
            TimerPhase.CUSTOM -> "⏱️ Custom Timer"
        }

        val remainingMs = app.timerEngine.computeRemainingMs()
        val minutes = remainingMs / 60000
        val seconds = (remainingMs % 60000) / 1000
        val timeText = String.format("%d:%02d remaining", minutes, seconds)

        // Build task name
        val taskName = timerState.taskId?.let { taskId ->
            // We'll use a simple approach - the ViewModel provides the name
            "Working..."
        } ?: "No task selected"

        // Notification actions
        val pauseResumeIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = if (timerState.state == TimerState.RUNNING) {
                TimerActionReceiver.ACTION_PAUSE
            } else {
                TimerActionReceiver.ACTION_RESUME
            }
        }
        val pauseResumePendingIntent = PendingIntent.getBroadcast(
            this, 0, pauseResumeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val skipIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = TimerActionReceiver.ACTION_SKIP
        }
        val skipPendingIntent = PendingIntent.getBroadcast(
            this, 1, skipIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerActionReceiver::class.java).apply {
            action = TimerActionReceiver.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getBroadcast(
            this, 2, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Open app intent
        val openIntent = packageManager.getLaunchIntentForPackage(packageName)
        val openPendingIntent = PendingIntent.getActivity(
            this, 3, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pauseResumeLabel = if (timerState.state == TimerState.RUNNING) "Pause" else "Resume"

        val notification = NotificationCompat.Builder(this, PomodoroApplication.CHANNEL_TIMER)
            .setContentTitle(title)
            .setContentText(timeText)
            .setSubText(taskName)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setShowWhen(false)
            .setContentIntent(openPendingIntent)
            .addAction(0, pauseResumeLabel, pauseResumePendingIntent)
            .addAction(0, "Skip", skipPendingIntent)
            .addAction(0, "Stop", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()

        return notification
    }

    private fun updateNotification() {
        if (isServiceStarted) {
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.notify(NOTIFICATION_ID, buildNotification())
        }
    }

    override fun onDestroy() {
        if (wakeLock.isHeld) {
            wakeLock.release()
        }
        super.onDestroy()
    }

    companion object {
        const val ACTION_START = "com.example.pomodoro.action.START_SERVICE"
        const val ACTION_STOP = "com.example.pomodoro.action.STOP_SERVICE"
        const val ACTION_UPDATE = "com.example.pomodoro.action.UPDATE_SERVICE"
        const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_START
            }
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }

        fun update(context: Context) {
            val intent = Intent(context, TimerForegroundService::class.java).apply {
                action = ACTION_UPDATE
            }
            context.startService(intent)
        }
    }
}
