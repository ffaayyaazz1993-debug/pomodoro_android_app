package com.example.pomodoro.domain.timer

import com.example.pomodoro.data.local.entities.AppStateEntity
import com.example.pomodoro.data.local.dao.AppStateDao
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.util.UUID

/**
 * Timer state machine - the single source of truth for timer state.
 * Uses timestamp-based calculation for accuracy across process death,
 * screen off, Doze, and other Android lifecycle events.
 */
enum class TimerState {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    SKIPPED,
    CANCELLED
}

enum class TimerPhase {
    FOCUS,
    SHORT_BREAK,
    LONG_BREAK,
    CUSTOM
}

data class TimerSnapshot(
    val state: TimerState = TimerState.IDLE,
    val phase: TimerPhase = TimerPhase.FOCUS,
    val sessionId: String = "",
    val taskId: Long? = null,
    val projectId: Long? = null,
    val plannedDurationMs: Long = 25 * 60 * 1000L,
    val startTimestamp: Long = 0L,
    val pauseTimestamp: Long = 0L,
    val accumulatedPauseMs: Long = 0L,
    val cycleNumber: Int = 1,
    val completedPomodorosInCycle: Int = 0,
    val totalCompletedSessions: Int = 0,
    val totalSkippedSessions: Int = 0,
    val totalCancelledSessions: Int = 0
) {
    val isRunning: Boolean get() = state == TimerState.RUNNING
    val isPaused: Boolean get() = state == TimerState.PAUSED
    val isIdle: Boolean get() = state == TimerState.IDLE
    val isCompleted: Boolean get() = state == TimerState.COMPLETED
}

/**
 * Clock abstraction for testability.
 */
interface Clock {
    fun currentTimeMillis(): Long
    fun elapsedRealtime(): Long
}

class SystemClock : Clock {
    override fun currentTimeMillis(): Long = System.currentTimeMillis()
    override fun elapsedRealtime(): Long = android.os.SystemClock.elapsedRealtime()
}

class FakeClock(
    private var currentTimeMs: Long = System.currentTimeMillis(),
    private var elapsedRealtimeMs: Long = android.os.SystemClock.elapsedRealtime()
) : Clock {
    override fun currentTimeMillis(): Long = currentTimeMs
    override fun elapsedRealtime(): Long = elapsedRealtimeMs

    fun advanceBy(ms: Long) {
        currentTimeMs += ms
        elapsedRealtimeMs += ms
    }

    fun setCurrentTime(ms: Long) {
        currentTimeMs = ms
    }
}

/**
 * Repository interface for timer persistence.
 */
open class TimerRepository(val appStateDao: AppStateDao) {
    companion object {
        const val KEY_TIMER_STATE = "timer_state"
    }

    open suspend fun saveTimerState(snapshot: TimerSnapshot) {
        val json = JSONObject().apply {
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
            AppStateEntity(key = KEY_TIMER_STATE, value = json.toString())
        )
    }

    open suspend fun loadTimerState(): TimerSnapshot? {
        val entity = appStateDao.getValue(KEY_TIMER_STATE) ?: return null
        return try {
            val json = JSONObject(entity.value)
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

    open suspend fun clearTimerState() {
        appStateDao.delete(KEY_TIMER_STATE)
    }
}

/**
 * Core timer engine - manages timer lifecycle using timestamps.
 * The remaining time is always computed from timestamps, never from a counter.
 */
class TimerEngine(
    private val clock: Clock,
    private val timerRepository: TimerRepository,
    private val sessionRepository: com.example.pomodoro.data.repository.SessionRepository,
    private val preferences: com.example.pomodoro.data.preferences.AppPreferences
) {
    private val _state = MutableStateFlow(TimerSnapshot())
    val state: StateFlow<TimerSnapshot> = _state.asStateFlow()

    private var sessionDbId: Long = -1L

    init {
        // Will be initialized via restore()
    }

    suspend fun restore() {
        val saved = timerRepository.loadTimerState()
        if (saved != null) {
            // Reconstruct state from timestamps
            val reconstructed = reconstructFromTimestamps(saved)
            _state.value = reconstructed

            // If the timer should have completed while we were away
            if (reconstructed.state == TimerState.RUNNING) {
                val remaining = computeRemainingMs(reconstructed)
                if (remaining <= 0) {
                    // Timer completed while app was away
                    completeInternal(reconstructed)
                }
            }
        }
    }

    private fun reconstructFromTimestamps(saved: TimerSnapshot): TimerSnapshot {
        return when (saved.state) {
            TimerState.RUNNING -> {
                val remaining = computeRemainingMs(saved)
                if (remaining <= 0) {
                    saved.copy(state = TimerState.COMPLETED)
                } else {
                    saved // Still running
                }
            }
            else -> saved
        }
    }

    fun computeRemainingMs(snapshot: TimerSnapshot = _state.value): Long {
        return when (snapshot.state) {
            TimerState.IDLE -> snapshot.plannedDurationMs
            TimerState.RUNNING -> {
                val now = clock.currentTimeMillis()
                val elapsed = now - snapshot.startTimestamp - snapshot.accumulatedPauseMs
                (snapshot.plannedDurationMs - elapsed).coerceAtLeast(0)
            }
            TimerState.PAUSED -> {
                val elapsedBeforePause = snapshot.pauseTimestamp - snapshot.startTimestamp - snapshot.accumulatedPauseMs
                (snapshot.plannedDurationMs - elapsedBeforePause).coerceAtLeast(0)
            }
            TimerState.COMPLETED, TimerState.SKIPPED, TimerState.CANCELLED -> 0
        }
    }

    fun computeElapsedMs(snapshot: TimerSnapshot = _state.value): Long {
        return snapshot.plannedDurationMs - computeRemainingMs(snapshot)
    }

    fun computeProgress(snapshot: TimerSnapshot = _state.value): Float {
        if (snapshot.plannedDurationMs == 0L) return 0f
        return computeElapsedMs(snapshot).toFloat() / snapshot.plannedDurationMs.toFloat()
    }

    suspend fun start(
        phase: TimerPhase = TimerPhase.FOCUS,
        durationMs: Long? = null,
        taskId: Long? = null,
        projectId: Long? = null
    ) {
        val settings = preferences.getTimerSettingsSnapshot()
        val plannedDuration = durationMs ?: when (phase) {
            TimerPhase.FOCUS -> settings.focusDurationMin * 60 * 1000L
            TimerPhase.SHORT_BREAK -> settings.shortBreakMin * 60 * 1000L
            TimerPhase.LONG_BREAK -> settings.longBreakMin * 60 * 1000L
            TimerPhase.CUSTOM -> settings.focusDurationMin * 60 * 1000L
        }

        val currentCycle = _state.value.cycleNumber
        val completedInCycle = _state.value.completedPomodorosInCycle

        val sessionId = UUID.randomUUID().toString()

        // Create session in database
        val session = sessionRepository.createSession(
            phase = phase.name,
            plannedDurationMs = plannedDuration,
            taskId = taskId,
            projectId = projectId,
            cycleNumber = currentCycle
        )
        sessionDbId = session.id

        // Log start event
        sessionRepository.logEvent(session.id, "SESSION_STARTED")

        val snapshot = TimerSnapshot(
            state = TimerState.RUNNING,
            phase = phase,
            sessionId = sessionId,
            taskId = taskId,
            projectId = projectId,
            plannedDurationMs = plannedDuration,
            startTimestamp = clock.currentTimeMillis(),
            pauseTimestamp = 0L,
            accumulatedPauseMs = 0L,
            cycleNumber = currentCycle,
            completedPomodorosInCycle = completedInCycle,
            totalCompletedSessions = _state.value.totalCompletedSessions,
            totalSkippedSessions = _state.value.totalSkippedSessions,
            totalCancelledSessions = _state.value.totalCancelledSessions
        )

        _state.value = snapshot
        timerRepository.saveTimerState(snapshot)
    }

    suspend fun pause() {
        val current = _state.value
        if (current.state != TimerState.RUNNING) return

        val now = clock.currentTimeMillis()
        val paused = current.copy(
            state = TimerState.PAUSED,
            pauseTimestamp = now
        )

        _state.value = paused
        timerRepository.saveTimerState(paused)

        if (sessionDbId > 0) {
            sessionRepository.logEvent(sessionDbId, "SESSION_PAUSED")
        }
    }

    suspend fun resume() {
        val current = _state.value
        if (current.state != TimerState.PAUSED) return

        val now = clock.currentTimeMillis()
        val pauseDuration = now - current.pauseTimestamp
        val resumed = current.copy(
            state = TimerState.RUNNING,
            pauseTimestamp = 0L,
            accumulatedPauseMs = current.accumulatedPauseMs + pauseDuration
        )

        _state.value = resumed
        timerRepository.saveTimerState(resumed)

        if (sessionDbId > 0) {
            sessionRepository.logEvent(sessionDbId, "SESSION_RESUMED")
        }
    }

    suspend fun stop() {
        val current = _state.value
        if (current.state == TimerState.IDLE) return

        val now = clock.currentTimeMillis()
        val elapsed = computeElapsedMs(current)

        // Mark session as cancelled
        if (sessionDbId > 0) {
            sessionRepository.completeSession(sessionDbId, "CANCELLED", now, elapsed)
            sessionRepository.logEvent(sessionDbId, "SESSION_CANCELLED")
        }

        val cancelled = current.copy(
            state = TimerState.CANCELLED,
            totalCancelledSessions = current.totalCancelledSessions + 1
        )

        _state.value = cancelled
        timerRepository.saveTimerState(cancelled)
    }

    suspend fun reset() {
        val current = _state.value
        val resetState = TimerSnapshot(
            cycleNumber = current.cycleNumber,
            completedPomodorosInCycle = current.completedPomodorosInCycle,
            totalCompletedSessions = current.totalCompletedSessions,
            totalSkippedSessions = current.totalSkippedSessions,
            totalCancelledSessions = current.totalCancelledSessions
        )
        _state.value = resetState
        timerRepository.saveTimerState(resetState)
    }

    suspend fun skip() {
        val current = _state.value
        if (current.state == TimerState.IDLE) return

        val now = clock.currentTimeMillis()
        val elapsed = computeElapsedMs(current)

        if (sessionDbId > 0) {
            sessionRepository.completeSession(sessionDbId, "SKIPPED", now, elapsed)
            sessionRepository.logEvent(sessionDbId, "SESSION_SKIPPED")
        }

        val skipped = current.copy(
            state = TimerState.SKIPPED,
            totalSkippedSessions = current.totalSkippedSessions + 1
        )

        _state.value = skipped
        timerRepository.saveTimerState(skipped)
    }

    suspend fun addTime(minutes: Int) {
        val current = _state.value
        if (current.state != TimerState.RUNNING && current.state != TimerState.PAUSED) return

        val extended = current.copy(
            plannedDurationMs = current.plannedDurationMs + (minutes * 60 * 1000L)
        )
        _state.value = extended
        timerRepository.saveTimerState(extended)
    }

    suspend fun subtractTime(minutes: Int) {
        val current = _state.value
        if (current.state != TimerState.RUNNING && current.state != TimerState.PAUSED) return

        val newDuration = (current.plannedDurationMs - (minutes * 60 * 1000L)).coerceAtLeast(60000L)
        val reduced = current.copy(plannedDurationMs = newDuration)
        _state.value = reduced
        timerRepository.saveTimerState(reduced)
    }

    suspend fun complete() {
        completeInternal(_state.value)
    }

    private suspend fun completeInternal(snapshot: TimerSnapshot) {
        val now = clock.currentTimeMillis()
        val elapsed = computeElapsedMs(snapshot)

        // Complete session in database
        if (sessionDbId > 0) {
            sessionRepository.completeSession(sessionDbId, "COMPLETED", now, elapsed)
            sessionRepository.logEvent(sessionDbId, "SESSION_COMPLETED")
        }

        // Update task pomodoro count if this was a focus session
        if (snapshot.phase == TimerPhase.FOCUS && snapshot.taskId != null) {
            try {
                val app = com.example.pomodoro.PomodoroApplication.instance
                app.taskRepository.incrementPomodoroCount(snapshot.taskId)
            } catch (_: Exception) { }
        }

        val isFocus = snapshot.phase == TimerPhase.FOCUS
        val newCompletedInCycle = if (isFocus) snapshot.completedPomodorosInCycle + 1 else snapshot.completedPomodorosInCycle
        val newTotalCompleted = snapshot.totalCompletedSessions + 1

        val completed = snapshot.copy(
            state = TimerState.COMPLETED,
            completedPomodorosInCycle = newCompletedInCycle,
            totalCompletedSessions = newTotalCompleted
        )

        _state.value = completed
        timerRepository.saveTimerState(completed)
    }

    /**
     * Determine the next phase based on cycle logic.
     */
    fun getNextPhase(): TimerPhase {
        val current = _state.value
        val settings = preferences.getTimerSettingsSnapshot() // This is sync in this context

        return when {
            current.phase == TimerPhase.FOCUS -> {
                if (current.completedPomodorosInCycle % settings.longBreakInterval == 0) {
                    TimerPhase.LONG_BREAK
                } else {
                    TimerPhase.SHORT_BREAK
                }
            }
            else -> TimerPhase.FOCUS
        }
    }

    suspend fun switchPhase(phase: TimerPhase, durationMs: Long? = null) {
        stop()
        start(phase = phase, durationMs = durationMs, taskId = _state.value.taskId, projectId = _state.value.projectId)
    }

    suspend fun restart() {
        val current = _state.value
        stop()
        start(
            phase = current.phase,
            durationMs = current.plannedDurationMs,
            taskId = current.taskId,
            projectId = current.projectId
        )
    }

    fun getSessionDbId(): Long = sessionDbId
    fun setSessionDbId(id: Long) { sessionDbId = id }
}
