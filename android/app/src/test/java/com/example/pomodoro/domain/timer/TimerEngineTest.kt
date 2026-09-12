package com.example.pomodoro.domain.timer

import com.example.pomodoro.data.preferences.AppPreferences
import com.example.pomodoro.data.preferences.TimerSettings
import com.example.pomodoro.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Comprehensive tests for the TimerEngine.
 * Uses FakeClock to simulate time passage without waiting.
 */
class TimerEngineTest {

    private lateinit var fakeClock: FakeClock
    private lateinit var timerRepository: TestTimerRepository
    private lateinit var sessionRepository: TestSessionRepository
    private lateinit var preferences: TestPreferences
    private lateinit var timerEngine: TimerEngine

    @Before
    fun setup() {
        fakeClock = FakeClock(currentTimeMs = 1000000L, elapsedRealtimeMs = 1000000L)
        timerRepository = TestTimerRepository()
        sessionRepository = TestSessionRepository()
        preferences = TestPreferences()
        timerEngine = TimerEngine(
            clock = fakeClock,
            timerRepository = timerRepository,
            sessionRepository = sessionRepository,
            preferences = preferences
        )
    }

    // === State Machine Tests ===

    @Test
    fun `initial state is IDLE`() = runTest {
        assertEquals(TimerState.IDLE, timerEngine.state.value.state)
    }

    @Test
    fun `start transitions to RUNNING`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS)
        assertEquals(TimerState.RUNNING, timerEngine.state.value.state)
    }

    @Test
    fun `pause transitions to PAUSED`() = runTest {
        timerEngine.start()
        timerEngine.pause()
        assertEquals(TimerState.PAUSED, timerEngine.state.value.state)
    }

    @Test
    fun `resume transitions back to RUNNING`() = runTest {
        timerEngine.start()
        timerEngine.pause()
        timerEngine.resume()
        assertEquals(TimerState.RUNNING, timerEngine.state.value.state)
    }

    @Test
    fun `stop transitions to CANCELLED`() = runTest {
        timerEngine.start()
        timerEngine.stop()
        assertEquals(TimerState.CANCELLED, timerEngine.state.value.state)
    }

    @Test
    fun `skip transitions to SKIPPED`() = runTest {
        timerEngine.start()
        timerEngine.skip()
        assertEquals(TimerState.SKIPPED, timerEngine.state.value.state)
    }

    @Test
    fun `complete transitions to COMPLETED`() = runTest {
        timerEngine.start()
        timerEngine.complete()
        assertEquals(TimerState.COMPLETED, timerEngine.state.value.state)
    }

    // === Timestamp Accuracy Tests ===

    @Test
    fun `remaining time decreases accurately with clock advance`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        fakeClock.advanceBy(5 * 60 * 1000L) // 5 minutes later
        val remaining = timerEngine.computeRemainingMs()
        assertEquals(20 * 60 * 1000L, remaining)
    }

    @Test
    fun `pause freezes remaining time`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        fakeClock.advanceBy(10 * 60 * 1000L)
        timerEngine.pause()
        fakeClock.advanceBy(5 * 60 * 1000L) // Time passes while paused
        val remaining = timerEngine.computeRemainingMs()
        assertEquals(15 * 60 * 1000L, remaining) // Should still be 15 min
    }

    @Test
    fun `resume accounts for pause duration`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        fakeClock.advanceBy(5 * 60 * 1000L)
        timerEngine.pause()
        fakeClock.advanceBy(10 * 60 * 1000L) // 10 min pause
        timerEngine.resume()
        fakeClock.advanceBy(5 * 60 * 1000L) // 5 more min running
        val remaining = timerEngine.computeRemainingMs()
        // 25 - 5 (before pause) - 5 (after resume) = 15 min remaining
        assertEquals(15 * 60 * 1000L, remaining)
    }

    @Test
    fun `timer computes zero remaining when time exceeded`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        fakeClock.advanceBy(30 * 60 * 1000L) // 30 minutes later
        val remaining = timerEngine.computeRemainingMs()
        assertEquals(0L, remaining)
    }

    @Test
    fun `elapsed time equals planned minus remaining`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        fakeClock.advanceBy(10 * 60 * 1000L)
        val elapsed = timerEngine.computeElapsedMs()
        assertEquals(10 * 60 * 1000L, elapsed)
    }

    @Test
    fun `progress is computed correctly`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        fakeClock.advanceBy(12 * 60 * 1000L + 30 * 1000L) // 12:30 elapsed
        val progress = timerEngine.computeProgress()
        assertEquals(0.5f, progress, 0.01f)
    }

    // === Process Death Recovery Tests ===

    @Test
    fun `state is persisted on start`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        assertNotNull(timerRepository.savedState)
        assertEquals(TimerState.RUNNING, timerRepository.savedState?.state)
    }

    @Test
    fun `state is persisted on pause`() = runTest {
        timerEngine.start()
        timerEngine.pause()
        assertEquals(TimerState.PAUSED, timerRepository.savedState?.state)
    }

    @Test
    fun `recovery detects completed timer after process death`() = runTest {
        // Simulate: start timer, then time passes beyond duration
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        fakeClock.advanceBy(30 * 60 * 1000L) // 30 min later

        // Simulate process restart - restore should detect completion
        timerEngine.restore()
        // State should reflect that the timer has completed
        val state = timerEngine.state.value
        assertTrue(state.isCompleted || state.isRunning && timerEngine.computeRemainingMs() <= 0)
    }

    @Test
    fun `recovery reconstructs running timer correctly`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        fakeClock.advanceBy(10 * 60 * 1000L) // 10 min elapsed

        // Simulate process restart
        timerEngine.restore()
        val remaining = timerEngine.computeRemainingMs()
        assertEquals(15 * 60 * 1000L, remaining)
    }

    // === Add/Subtract Time Tests ===

    @Test
    fun `addTime increases planned duration`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        timerEngine.addTime(5)
        assertEquals(30 * 60 * 1000L, timerEngine.state.value.plannedDurationMs)
    }

    @Test
    fun `subtractTime decreases planned duration`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        timerEngine.subtractTime(5)
        assertEquals(20 * 60 * 1000L, timerEngine.state.value.plannedDurationMs)
    }

    @Test
    fun `subtractTime does not go below 1 minute`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        timerEngine.subtractTime(30)
        assertEquals(60000L, timerEngine.state.value.plannedDurationMs)
    }

    // === Cycle Logic Tests ===

    @Test
    fun `getNextPhase returns SHORT_BREAK after focus`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS)
        timerEngine.complete()
        assertEquals(TimerPhase.SHORT_BREAK, timerEngine.getNextPhase())
    }

    @Test
    fun `getNextPhase returns LONG_BREAK after 4th focus`() = runTest {
        // Simulate 4 completed focus sessions
        for (i in 1..4) {
            timerEngine.start(phase = TimerPhase.FOCUS)
            timerEngine.complete()
            if (i < 4) {
                timerEngine.start(phase = TimerPhase.SHORT_BREAK)
                timerEngine.complete()
            }
        }
        assertEquals(TimerPhase.LONG_BREAK, timerEngine.getNextPhase())
    }

    @Test
    fun `getNextPhase returns FOCUS after break`() = runTest {
        timerEngine.start(phase = TimerPhase.SHORT_BREAK)
        timerEngine.complete()
        assertEquals(TimerPhase.FOCUS, timerEngine.getNextPhase())
    }

    // === Session Tracking Tests ===

    @Test
    fun `completed pomodoros in cycle increments`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS)
        timerEngine.complete()
        assertEquals(1, timerEngine.state.value.completedPomodorosInCycle)
    }

    @Test
    fun `total completed sessions increments`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS)
        timerEngine.complete()
        assertEquals(1, timerEngine.state.value.totalCompletedSessions)
    }

    @Test
    fun `skipped sessions increments skip counter`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS)
        timerEngine.skip()
        assertEquals(1, timerEngine.state.value.totalSkippedSessions)
    }

    @Test
    fun `cancelled sessions increments cancel counter`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS)
        timerEngine.stop()
        assertEquals(1, timerEngine.state.value.totalCancelledSessions)
    }

    // === Edge Cases ===

    @Test
    fun `pause when idle does nothing`() = runTest {
        timerEngine.pause()
        assertEquals(TimerState.IDLE, timerEngine.state.value.state)
    }

    @Test
    fun `resume when idle does nothing`() = runTest {
        timerEngine.resume()
        assertEquals(TimerState.IDLE, timerEngine.state.value.state)
    }

    @Test
    fun `skip when idle does nothing`() = runTest {
        timerEngine.skip()
        assertEquals(TimerState.IDLE, timerEngine.state.value.state)
    }

    @Test
    fun `multiple pauses accumulate correctly`() = runTest {
        timerEngine.start(phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        fakeClock.advanceBy(5 * 60 * 1000L)
        timerEngine.pause()
        fakeClock.advanceBy(2 * 60 * 1000L) // 2 min pause
        timerEngine.resume()
        fakeClock.advanceBy(3 * 60 * 1000L)
        timerEngine.pause()
        fakeClock.advanceBy(3 * 60 * 1000L) // 3 min pause
        timerEngine.resume()
        fakeClock.advanceBy(2 * 60 * 1000L)

        // Total elapsed = 5 + 3 + 2 = 10 min
        // Total paused = 2 + 3 = 5 min
        // Remaining = 25 - 10 = 15 min
        val remaining = timerEngine.computeRemainingMs()
        assertEquals(15 * 60 * 1000L, remaining)
    }

    // === Test Doubles ===

    class TestTimerRepository : TimerRepository(
        appStateDao = object : com.example.pomodoro.data.local.dao.AppStateDao {
            override suspend fun getValue(key: String) = null
            override suspend fun insertOrUpdate(state: com.example.pomodoro.data.local.entities.AppStateEntity) {}
            override suspend fun delete(key: String) {}
            override suspend fun getAll() = emptyList<com.example.pomodoro.data.local.entities.AppStateEntity>()
        }
    ) {
        var savedState: TimerSnapshot? = null
        override suspend fun saveTimerState(snapshot: TimerSnapshot) {
            savedState = snapshot
        }
        override suspend fun loadTimerState(): TimerSnapshot? = savedState
        override suspend fun clearTimerState() { savedState = null }
    }

    class TestSessionRepository : SessionRepository(
        sessionDao = object : com.example.pomodoro.data.local.dao.SessionDao {
            override fun getAllSessions() = MutableStateFlow(emptyList<com.example.pomodoro.data.local.entities.SessionEntity>()).asStateFlow()
            override suspend fun getSessionById(id: Long) = null
            override suspend fun getSessionBySessionId(sessionId: String) = null
            override fun getRecentFocusSessions(limit: Int) = MutableStateFlow(emptyList<com.example.pomodoro.data.local.entities.SessionEntity>()).asStateFlow()
            override fun getSessionsInRange(startMs: Long, endMs: Long) = MutableStateFlow(emptyList<com.example.pomodoro.data.local.entities.SessionEntity>()).asStateFlow()
            override fun getSessionsByTask(taskId: Long) = MutableStateFlow(emptyList<com.example.pomodoro.data.local.entities.SessionEntity>()).asStateFlow()
            override fun getSessionsByProject(projectId: Long) = MutableStateFlow(emptyList<com.example.pomodoro.data.local.entities.SessionEntity>()).asStateFlow()
            override fun getCompletedFocusSessionsInRange(startMs: Long, endMs: Long) = MutableStateFlow(emptyList<com.example.pomodoro.data.local.entities.SessionEntity>()).asStateFlow()
            override suspend fun countCompletedFocusSessions(startMs: Long, endMs: Long) = 0
            override suspend fun sumFocusDurationInRange(startMs: Long, endMs: Long) = 0L
            override suspend fun getActiveSession() = null
            override suspend fun insert(session: com.example.pomodoro.data.local.entities.SessionEntity) = 1L
            override suspend fun update(session: com.example.pomodoro.data.local.entities.SessionEntity) {}
            override suspend fun delete(session: com.example.pomodoro.data.local.entities.SessionEntity) {}
            override suspend fun completeSession(id: Long, status: String, endTime: Long, actualDuration: Long) {}
            override suspend fun getCompletedFocusSessionsList(startMs: Long, endMs: Long) = emptyList<com.example.pomodoro.data.local.entities.SessionEntity>()
        },
        eventDao = object : com.example.pomodoro.data.local.dao.SessionEventDao {
            override fun getEventsForSession(sessionId: Long) = MutableStateFlow(emptyList<com.example.pomodoro.data.local.entities.SessionEventEntity>()).asStateFlow()
            override suspend fun getEventsForSessionList(sessionId: Long) = emptyList<com.example.pomodoro.data.local.entities.SessionEventEntity>()
            override suspend fun insert(event: com.example.pomodoro.data.local.entities.SessionEventEntity) = 1L
            override suspend fun insertAll(events: List<com.example.pomodoro.data.local.entities.SessionEventEntity>) {}
            override suspend fun countPauses(sessionId: Long) = 0
            override fun getEventsByTypeInRange(eventType: String, startMs: Long, endMs: Long) = MutableStateFlow(emptyList<com.example.pomodoro.data.local.entities.SessionEventEntity>()).asStateFlow()
        }
    )

    class TestPreferences : AppPreferences(
        context = android.app.Application() // This would need mocking in real tests
    ) {
        override suspend fun getTimerSettingsSnapshot(): TimerSettings = TimerSettings()
    }
}
