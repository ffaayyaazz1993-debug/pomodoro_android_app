package com.example.pomodoro.service

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.domain.timer.TimerPhase

class NotificationService(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    suspend fun notifySessionComplete(phase: TimerPhase, taskId: Long? = null) {
        val app = context.applicationContext as PomodoroApplication
        val settings = app.appPreferences.getTimerSettingsSnapshot()

        val title = when (phase) {
            TimerPhase.FOCUS -> "🎉 Focus Complete!"
            TimerPhase.SHORT_BREAK -> "☕ Break Over"
            TimerPhase.LONG_BREAK -> "🌴 Long Break Over"
            TimerPhase.CUSTOM -> "⏱️ Timer Complete"
        }

        val body = when (phase) {
            TimerPhase.FOCUS -> "Time for a break! Great work."
            TimerPhase.SHORT_BREAK, TimerPhase.LONG_BREAK -> "Ready to focus again?"
            TimerPhase.CUSTOM -> "Your timer has finished."
        }

        // Play sound
        if (settings.soundEnabled) {
            playCompletionSound()
        }

        // Vibrate
        if (settings.vibrationEnabled) {
            vibrate()
        }

        // Show notification
        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val openPendingIntent = PendingIntent.getActivity(
            context, 10, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, PomodoroApplication.CHANNEL_COMPLETION)
            .setContentTitle(title)
            .setContentText(body)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(openPendingIntent)
            .build()

        notificationManager.notify(NOTIFICATION_COMPLETE_ID, notification)
    }

    suspend fun notifyWarning(minutesRemaining: Int) {
        val notification = NotificationCompat.Builder(context, PomodoroApplication.CHANNEL_TIMER)
            .setContentTitle("⚠️ Time Warning")
            .setContentText("$minutesRemaining minute(s) remaining")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_WARNING_ID, notification)
    }

    suspend fun notifyGoalAchieved(goalDescription: String) {
        val notification = NotificationCompat.Builder(context, PomodoroApplication.CHANNEL_GOALS)
            .setContentTitle("🏆 Goal Achieved!")
            .setContentText(goalDescription)
            .setSmallIcon(android.R.drawable.ic_star_big_on)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_GOAL_ID, notification)
    }

    private fun playCompletionSound() {
        try {
            val toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
            toneGenerator.startTone(ToneGenerator.TONE_PROP_BEEP2, 500)
            // Release after tone completes
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                toneGenerator.release()
            }, 600)
        } catch (_: Exception) { }
    }

    private fun vibrate() {
        try {
            val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        } catch (_: Exception) { }
    }

    fun cancelAll() {
        notificationManager.cancelAll()
    }

    companion object {
        const val NOTIFICATION_COMPLETE_ID = 2001
        const val NOTIFICATION_WARNING_ID = 2002
        const val NOTIFICATION_GOAL_ID = 2003
    }
}
