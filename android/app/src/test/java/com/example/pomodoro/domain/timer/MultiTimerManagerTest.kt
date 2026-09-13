package com.example.pomodoro.domain.timer

import com.example.pomodoro.data.preferences.AppPreferences
import com.example.pomodoro.data.preferences.TimerSettings
import com.example.pomodoro.data.repository.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Tests for the MultiTimerManager - per-task independent timers.
 */
class MultiTimerManagerTest {

    private lateinit var fakeClock: FakeClock
    private lateinit var multiTimerManager: MultiTimerManager

    @Before
    fun setup() {
        fakeClock = FakeClock(currentTimeMs = 1000000L, elapsedRealtimeMs = 1000000L)
        val timerRepository = TimerEngineTest.TestTimerRepository()
        val sessionRepository = TimerEngineTest.TestSessionRepository()
        val preferences = TimerEngineTest.TestPreferences()

        multiTimerManager = MultiTimerManager(
            clock = fakeClock,
            timerRepository = timerRepository,
            sessionRepository = sessionRepository,
            preferences = preferences
        )
    }

    @Test
    fun `initially no active timers`() {
        assertEquals(0, multiTimerManager.getActiveTimerCount())
        assertTrue(multiTimerManager.getActiveTimers().isEmpty())
    }

    @Test
    fun `starting timer for task creates timer`() = runTest {
        multiTimerManager.startTimerForTask(taskId = 1L, phase = TimerPhase.FOCUS)
        assertEquals(1, multiTimerManager.getActiveTimerCount())
        assertTrue(multiTimerManager.isTaskTimerActive(1L))
    }

    @Test
    fun `multiple tasks can have independent timers`() = runTest {
        multiTimerManager.startTimerForTask(taskId = 1L, phase = TimerPhase.FOCUS)
        multiTimerManager.startTimerForTask(taskId = 2L, phase = TimerPhase.FOCUS)
        multiTimerManager.startTimerForTask(taskId = 3L, phase = TimerPhase.FOCUS)

        assertEquals(3, multiTimerManager.getActiveTimerCount())
        assertTrue(multiTimerManager.isTaskTimerActive(1L))
        assertTrue(multiTimerManager.isTaskTimerActive(2L))
        assertTrue(multiTimerManager.isTaskTimerActive(3L))
    }

    @Test
    fun `pausing one timer does not affect others`() = runTest {
        multiTimerManager.startTimerForTask(taskId = 1L, phase = TimerPhase.FOCUS)
        multiTimerManager.startTimerForTask(taskId = 2L, phase = TimerPhase.FOCUS)

        multiTimerManager.pauseTimerForTask(1L)

        val timer1 = multiTimerManager.getTimerForTask(1L)
        val timer2 = multiTimerManager.getTimerForTask(2L)

        assertEquals(TimerState.PAUSED, timer1.state.value.state)
        assertEquals(TimerState.RUNNING, timer2.state.value.state)
    }

    @Test
    fun `each task timer tracks time independently`() = runTest {
        multiTimerManager.startTimerForTask(taskId = 1L, phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        multiTimerManager.startTimerForTask(taskId = 2L, phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)

        fakeClock.advanceBy(5 * 60 * 1000L) // 5 minutes pass

        // Pause task 1
        multiTimerManager.pauseTimerForTask(1L)

        fakeClock.advanceBy(10 * 60 * 1000L) // 10 more minutes

        val timer1 = multiTimerManager.getTimerForTask(1L)
        val timer2 = multiTimerManager.getTimerForTask(2L)

        // Task 1: paused after 5 min, so 20 min remaining
        assertEquals(20 * 60 * 1000L, timer1.computeRemainingMs())
        // Task 2: ran for 15 min total, so 10 min remaining
        assertEquals(10 * 60 * 1000L, timer2.computeRemainingMs())
    }

    @Test
    fun `stopping a timer removes it from active set`() = runTest {
        multiTimerManager.startTimerForTask(taskId = 1L, phase = TimerPhase.FOCUS)
        multiTimerManager.startTimerForTask(taskId = 2L, phase = TimerPhase.FOCUS)

        multiTimerManager.stopTimerForTask(1L)

        assertEquals(1, multiTimerManager.getActiveTimerCount())
        assertFalse(multiTimerManager.isTaskTimerActive(1L))
        assertTrue(multiTimerManager.isTaskTimerActive(2L))
    }

    @Test
    fun `completing a timer updates state correctly`() = runTest {
        multiTimerManager.startTimerForTask(taskId = 1L, phase = TimerPhase.FOCUS)
        multiTimerManager.completeTimerForTask(1L)

        val timer = multiTimerManager.getTimerForTask(1L)
        assertEquals(TimerState.COMPLETED, timer.state.value.state)
    }

    @Test
    fun `removing a timer cleans up`() {
        multiTimerManager.getTimerForTask(1L) // Create it
        multiTimerManager.removeTimerForTask(1L)

        assertFalse(multiTimerManager.isTaskTimerActive(1L))
        assertEquals(0, multiTimerManager.getActiveTimerCount())
    }

    @Test
    fun `getTimer returns global timer when taskId is null`() {
        val globalTimer = multiTimerManager.getTimer(null)
        assertNotNull(globalTimer)
        assertEquals(globalTimer, multiTimerManager.getGlobalTimer())
    }

    @Test
    fun `getTimer returns task timer when taskId provided`() {
        val taskTimer = multiTimerManager.getTimer(42L)
        assertNotNull(taskTimer)
        assertEquals(taskTimer, multiTimerManager.getTimerForTask(42L))
    }

    @Test
    fun `resuming paused timer works correctly`() = runTest {
        multiTimerManager.startTimerForTask(taskId = 1L, phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        fakeClock.advanceBy(5 * 60 * 1000L)
        multiTimerManager.pauseTimerForTask(1L)
        fakeClock.advanceBy(3 * 60 * 1000L) // 3 min pause
        multiTimerManager.resumeTimerForTask(1L)
        fakeClock.advanceBy(2 * 60 * 1000L)

        val timer = multiTimerManager.getTimerForTask(1L)
        // 25 - 5 (before pause) - 2 (after resume) = 18 min remaining
        assertEquals(18 * 60 * 1000L, timer.computeRemainingMs())
    }

    @Test
    fun `adding time to one timer does not affect others`() = runTest {
        multiTimerManager.startTimerForTask(taskId = 1L, phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)
        multiTimerManager.startTimerForTask(taskId = 2L, phase = TimerPhase.FOCUS, durationMs = 25 * 60 * 1000L)

        val timer1 = multiTimerManager.getTimerForTask(1L)
        val timer2 = multiTimerManager.getTimerForTask(2L)

        timer1.addTime(5)

        assertEquals(30 * 60 * 1000L, timer1.state.value.plannedDurationMs)
        assertEquals(25 * 60 * 1000L, timer2.state.value.plannedDurationMs)
    }

    @Test
    fun `each task timer has independent cycle count`() = runTest {
        multiTimerManager.startTimerForTask(taskId = 1L, phase = TimerPhase.FOCUS)
        multiTimerManager.completeTimerForTask(1L)

        multiTimerManager.startTimerForTask(taskId = 2L, phase = TimerPhase.FOCUS)

        val timer1 = multiTimerManager.getTimerForTask(1L)
        val timer2 = multiTimerManager.getTimerForTask(2L)

        assertEquals(1, timer1.state.value.completedPomodorosInCycle)
        assertEquals(0, timer2.state.value.completedPomodorosInCycle)
    }
}
