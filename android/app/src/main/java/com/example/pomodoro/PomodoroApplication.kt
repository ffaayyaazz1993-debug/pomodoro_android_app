package com.example.pomodoro

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.example.pomodoro.data.local.database.PomodoroDatabase
import com.example.pomodoro.data.preferences.AppPreferences
import com.example.pomodoro.data.repository.SessionRepository
import com.example.pomodoro.data.repository.TaskRepository
import com.example.pomodoro.data.repository.ProjectRepository
import com.example.pomodoro.data.repository.GoalRepository
import com.example.pomodoro.domain.timer.TimerEngine
import com.example.pomodoro.domain.timer.TimerRepository
import com.example.pomodoro.domain.timer.SystemClock

class PomodoroApplication : Application() {

    lateinit var database: PomodoroDatabase
        private set
    lateinit var appPreferences: AppPreferences
        private set
    lateinit var timerEngine: TimerEngine
        private set

    // Repositories
    val taskRepository: TaskRepository by lazy { TaskRepository(database.taskDao()) }
    val projectRepository: ProjectRepository by lazy { ProjectRepository(database.projectDao()) }
    val sessionRepository: SessionRepository by lazy { SessionRepository(database.sessionDao(), database.sessionEventDao()) }
    val goalRepository: GoalRepository by lazy { GoalRepository(database.goalDao()) }
    val timerRepository: TimerRepository by lazy { TimerRepository(database.appStateDao()) }

    override fun onCreate() {
        super.onCreate()
        instance = this

        // Initialize database
        database = PomodoroDatabase.getInstance(this)

        // Initialize preferences
        appPreferences = AppPreferences(this)

        // Initialize timer engine
        timerEngine = TimerEngine(
            clock = SystemClock(),
            timerRepository = timerRepository,
            sessionRepository = sessionRepository,
            preferences = appPreferences
        )

        // Create notification channels
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val notificationManager = getSystemService(NotificationManager::class.java)

        // Timer channel
        val timerChannel = NotificationChannel(
            CHANNEL_TIMER,
            "Timer",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Active Pomodoro timer notifications"
            setShowBadge(false)
        }

        // Completion channel
        val completionChannel = NotificationChannel(
            CHANNEL_COMPLETION,
            "Session Completion",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Notifications when focus or break sessions complete"
            enableVibration(true)
            enableLights(true)
        }

        // Goals channel
        val goalsChannel = NotificationChannel(
            CHANNEL_GOALS,
            "Goals & Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Daily goal achievements and reminders"
        }

        notificationManager.createNotificationChannels(
            listOf(timerChannel, completionChannel, goalsChannel)
        )
    }

    companion object {
        const val CHANNEL_TIMER = "timer_channel"
        const val CHANNEL_COMPLETION = "completion_channel"
        const val CHANNEL_GOALS = "goals_channel"

        lateinit var instance: PomodoroApplication
            private set
    }
}
