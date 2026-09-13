package com.example.pomodoro.domain.timer

import com.example.pomodoro.data.preferences.AppPreferences
import com.example.pomodoro.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

/**
 * Manages multiple independent timers, one per task.
 * Each task can have its own timer running simultaneously.
 * Timers are independent - pausing one doesn't affect others.
 */
class MultiTimerManager(
    private val clock: Clock,
    private val timerRepository: TimerRepository,
    private val sessionRepository: SessionRepository,
    private val preferences: AppPreferences
) {
    // Map of taskId -> TimerEngine
    private val timers = mutableMapOf<Long, TimerEngine>()

    // A "global" timer for when no task is selected
    private val globalTimer: TimerEngine = TimerEngine(
        clock = clock,
        timerRepository = TimerRepositoryWithKey(timerRepository, GLOBAL_KEY),
        sessionRepository = sessionRepository,
        preferences = preferences
    )

    private val _activeTimerIds = MutableStateFlow<Set<Long>>(emptySet())
    val activeTimerIds: StateFlow<Set<Long>> = _activeTimerIds.asStateFlow()

    companion object {
        const val GLOBAL_KEY = "timer_global"
        const val TASK_KEY_PREFIX = "timer_task_"

        fun keyForTask(taskId: Long): String = "$TASK_KEY_PREFIX$taskId"
        const val GLOBAL_TASK_ID = -1L
    }

    /**
     * Get or create a timer for a specific task.
     */
    fun getTimerForTask(taskId: Long): TimerEngine {
        return timers.getOrPut(taskId) {
            TimerEngine(
                clock = clock,
                timerRepository = TimerRepositoryWithKey(timerRepository, keyForTask(taskId)),
                sessionRepository = sessionRepository,
                preferences = preferences
            )
        }
    }

    /**
     * Get the global (no-task) timer.
     */
    fun getGlobalTimer(): TimerEngine = globalTimer

    /**
     * Get timer for a task, or global timer if taskId is null.
     */
    fun getTimer(taskId: Long? = null): TimerEngine {
        return if (taskId != null) getTimerForTask(taskId) else globalTimer
    }

    /**
     * Start a timer for a specific task.
     */
    suspend fun startTimerForTask(
        taskId: Long,
        phase: TimerPhase = TimerPhase.FOCUS,
        durationMs: Long? = null,
        projectId: Long? = null
    ) {
        val engine = getTimerForTask(taskId)
        engine.start(phase = phase, durationMs = durationMs, taskId = taskId, projectId = projectId)
        _activeTimerIds.value = _activeTimerIds.value + taskId
    }

    /**
     * Start the global (no-task) timer.
     */
    suspend fun startGlobalTimer(
        phase: TimerPhase = TimerPhase.FOCUS,
        durationMs: Long? = null
    ) {
        globalTimer.start(phase = phase, durationMs = durationMs)
        _activeTimerIds.value = _activeTimerIds.value + GLOBAL_TASK_ID
    }

    /**
     * Pause a specific task's timer.
     */
    suspend fun pauseTimerForTask(taskId: Long) {
        timers[taskId]?.pause()
    }

    /**
     * Resume a specific task's timer.
     */
    suspend fun resumeTimerForTask(taskId: Long) {
        timers[taskId]?.resume()
    }

    /**
     * Stop a specific task's timer.
     */
    suspend fun stopTimerForTask(taskId: Long) {
        timers[taskId]?.let { engine ->
            engine.stop()
            _activeTimerIds.value = _activeTimerIds.value - taskId
        }
    }

    /**
     * Complete a specific task's timer.
     */
    suspend fun completeTimerForTask(taskId: Long) {
        timers[taskId]?.complete()
    }

    /**
     * Get all currently active (running or paused) timers with their task IDs.
     */
    fun getActiveTimers(): List<Pair<Long, TimerEngine>> {
        return timers.entries
            .filter { (_, engine) -> engine.state.value.isRunning || engine.state.value.isPaused }
            .map { (taskId, engine) -> taskId to engine }
    }

    /**
     * Get all timers (including idle ones that have been created).
     */
    fun getAllTimers(): Map<Long, TimerEngine> = timers.toMap()

    /**
     * Restore all timers from persisted state.
     */
    suspend fun restoreAll() {
        // Restore global timer
        globalTimer.restore()

        // Note: In production, we'd scan app_state for all timer_task_* keys
        // and restore each one. For now, timers are created on-demand.
    }

    /**
     * Remove a timer (e.g., when task is deleted).
     */
    fun removeTimerForTask(taskId: Long) {
        timers.remove(taskId)
        _activeTimerIds.value = _activeTimerIds.value - taskId
    }

    /**
     * Check if a specific task has an active timer.
     */
    fun isTaskTimerActive(taskId: Long): Boolean {
        val engine = timers[taskId] ?: return false
        return engine.state.value.isRunning || engine.state.value.isPaused
    }

    /**
     * Get count of active timers.
     */
    fun getActiveTimerCount(): Int {
        return timers.values.count { it.state.value.isRunning || it.state.value.isPaused }
    }
}

/**
 * A TimerRepository wrapper that uses a specific key for persistence.
 * This allows each task to have its own persisted timer state.
 */
class TimerRepositoryWithKey(
    private val delegate: TimerRepository,
    private val key: String
) : TimerRepository(delegate.appStateDao) {

    override suspend fun saveTimerState(snapshot: TimerSnapshot) {
        val json = org.json.JSONObject().apply {
            put("state", snapshot.state.name)
            put("phase", snapshot.phase.name)
            put("sessionId", snapshot.sessionId)
            put("taskId", snapshot.taskId ?: -1L)
            put("projectId", snapshot.projectId ?: -1L)
            put("plannedDurationMs", snapshot.plannedDurationMs)
            put("startTimestamp", snapshot.startTimestamp)
            put("pauseTimestamp", snapshot.pauseTimestamp)
            put("accumulatedPauseMs", snapshot.accumulatedPauseMs)
            put("cycleNumber", snapshot.cycleNumber)
            put("completedPomodorosInCycle", snapshot.completedPomodorosInCycle)
            put("totalCompletedSessions", snapshot.totalCompletedSessions)
            put("totalSkippedSessions", snapshot.totalSkippedSessions)
            put("totalCancelledSessions", snapshot.totalCancelledSessions)
        }
        appStateDao.insertOrUpdate(
            com.example.pomodoro.data.local.entities.AppStateEntity(
                key = key,
                value = json.toString()
            )
        )
    }

    override suspend fun loadTimerState(): TimerSnapshot? {
        val entity = appStateDao.getValue(key) ?: return null
        return try {
            val json = org.json.JSONObject(entity.value)
            TimerSnapshot(
                state = TimerState.valueOf(json.getString("state")),
                phase = TimerPhase.valueOf(json.getString("phase")),
                sessionId = json.getString("sessionId"),
                taskId = json.optLong("taskId", -1L).takeIf { it >= 0 },
                projectId = json.optLong("projectId", -1L).takeIf { it >= 0 },
                plannedDurationMs = json.getLong("plannedDurationMs"),
                startTimestamp = json.getLong("startTimestamp"),
                pauseTimestamp = json.getLong("pauseTimestamp"),
                accumulatedPauseMs = json.getLong("accumulatedPauseMs"),
                cycleNumber = json.getInt("cycleNumber"),
                completedPomodorosInCycle = json.getInt("completedPomodorosInCycle"),
                totalCompletedSessions = json.getInt("totalCompletedSessions"),
                totalSkippedSessions = json.getInt("totalSkippedSessions"),
                totalCancelledSessions = json.getInt("totalCancelledSessions")
            )
        } catch (e: Exception) {
            null
        }
    }

    override suspend fun clearTimerState() {
        appStateDao.delete(key)
    }
}
