package com.example.pomodoro.util

import org.junit.Assert.*
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun `formatDuration formats hours correctly`() {
        assertEquals("1h 30m", TimeUtils.formatDuration(5400000L))
    }

    @Test
    fun `formatDuration formats minutes correctly`() {
        assertEquals("5m 30s", TimeUtils.formatDuration(330000L))
    }

    @Test
    fun `formatDuration formats seconds only`() {
        assertEquals("45s", TimeUtils.formatDuration(45000L))
    }

    @Test
    fun `formatTimerDisplay formats correctly`() {
        assertEquals("25:00", TimeUtils.formatTimerDisplay(25 * 60 * 1000L))
        assertEquals("00:30", TimeUtils.formatTimerDisplay(30000L))
        assertEquals("01:05", TimeUtils.formatTimerDisplay(65000L))
    }

    @Test
    fun `getStartOfDay returns midnight`() {
        val ms = TimeUtils.getStartOfDay(1699999999000L) // Some timestamp
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = ms
        assertEquals(0, cal.get(java.util.Calendar.HOUR_OF_DAY))
        assertEquals(0, cal.get(java.util.Calendar.MINUTE))
        assertEquals(0, cal.get(java.util.Calendar.SECOND))
    }

    @Test
    fun `getEndOfDay returns 23_59_59_999`() {
        val start = TimeUtils.getStartOfDay(System.currentTimeMillis())
        val end = TimeUtils.getEndOfDay(System.currentTimeMillis())
        assertEquals(24 * 60 * 60 * 1000L - 1, end - start)
    }

    @Test
    fun `isSameDay returns true for same day`() {
        val cal = java.util.Calendar.getInstance()
        cal.set(2024, 0, 15, 10, 30)
        val time1 = cal.timeInMillis
        cal.set(2024, 0, 15, 22, 45)
        val time2 = cal.timeInMillis
        assertTrue(TimeUtils.isSameDay(time1, time2))
    }

    @Test
    fun `isSameDay returns false for different days`() {
        val cal = java.util.Calendar.getInstance()
        cal.set(2024, 0, 15, 23, 59)
        val time1 = cal.timeInMillis
        cal.set(2024, 0, 16, 0, 1)
        val time2 = cal.timeInMillis
        assertFalse(TimeUtils.isSameDay(time1, time2))
    }

    @Test
    fun `minutesToMs converts correctly`() {
        assertEquals(25 * 60 * 1000L, TimeUtils.minutesToMs(25))
        assertEquals(0L, TimeUtils.minutesToMs(0))
    }

    @Test
    fun `msToMinutes converts correctly`() {
        assertEquals(25, TimeUtils.msToMinutes(25 * 60 * 1000L))
        assertEquals(0, TimeUtils.msToMinutes(0L))
    }
}
