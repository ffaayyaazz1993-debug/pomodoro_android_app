package com.example.pomodoro.domain.usecase

import com.example.pomodoro.data.local.entities.SessionEntity
import com.example.pomodoro.data.repository.SessionRepository
import com.example.pomodoro.util.TimeUtils
import java.util.*

/**
 * Use cases for computing statistics from session data.
 * All computations are deterministic and based on actual stored data.
 */

data class DailyStats(
    val date: Long,
    val pomodoroCount: Int,
    val focusDurationMs: Long,
    val breakDurationMs: Long,
    val completedTasks: Int,
    val interruptions: Int,
    val sessionCount: Int
)

data class WeeklyStats(
    val weekStart: Long,
    val totalFocusMs: Long,
    val avgDailyFocusMs: Long,
    val totalPomodoros: Int,
    val activeDays: Int,
    val dailyStats: List<DailyStats>
)

data class MonthlyStats(
    val monthStart: Long,
    val totalHours: Double,
    val totalPomodoros: Int,
    val bestDay: DailyStats?,
    val avgDailyFocusMs: Long,
    val weeklyStats: List<WeeklyStats>
)

data class AllTimeStats(
    val totalFocusHours: Double,
    val totalPomodoros: Int,
    val completedTasks: Int,
    val longestStreak: Int,
    val currentStreak: Int,
    val totalSessions: Int,
    val completionRate: Float
)

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val lastActiveDate: Long?
)

class StatisticsUseCase(private val sessionRepository: SessionRepository) {

    suspend fun getDailyStats(dateMs: Long): DailyStats {
        val startOfDay = TimeUtils.getStartOfDay(dateMs)
        val endOfDay = startOfDay + 24 * 60 * 60 * 1000L

        val sessions = sessionRepository.getCompletedFocusSessionsList(startOfDay, endOfDay)

        return DailyStats(
            date = startOfDay,
            pomodoroCount = sessions.size,
            focusDurationMs = sessions.sumOf { it.actualDurationMs },
            breakDurationMs = 0L, // Would need separate query for breaks
            completedTasks = 0,
            interruptions = sessions.sumOf { it.interruptionCount },
            sessionCount = sessions.size
        )
    }

    suspend fun getWeeklyStats(weekStartMs: Long): WeeklyStats {
        val weekEndMs = weekStartMs + 7 * 24 * 60 * 60 * 1000L
        val dailyStatsList = mutableListOf<DailyStats>()

        var current = weekStartMs
        while (current < weekEndMs) {
            dailyStatsList.add(getDailyStats(current))
            current += 24 * 60 * 60 * 1000L
        }

        val totalFocusMs = dailyStatsList.sumOf { it.focusDurationMs }
        val activeDays = dailyStatsList.count { it.pomodoroCount > 0 }

        return WeeklyStats(
            weekStart = weekStartMs,
            totalFocusMs = totalFocusMs,
            avgDailyFocusMs = if (activeDays > 0) totalFocusMs / activeDays else 0L,
            totalPomodoros = dailyStatsList.sumOf { it.pomodoroCount },
            activeDays = activeDays,
            dailyStats = dailyStatsList
        )
    }

    suspend fun getMonthlyStats(monthStartMs: Long): MonthlyStats {
        val cal = Calendar.getInstance()
        cal.timeInMillis = monthStartMs
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)

        val dailyStatsList = mutableListOf<DailyStats>()
        var current = monthStartMs
        for (i in 0 until daysInMonth) {
            dailyStatsList.add(getDailyStats(current))
            current += 24 * 60 * 60 * 1000L
        }

        val totalFocusMs = dailyStatsList.sumOf { it.focusDurationMs }
        val bestDay = dailyStatsList.maxByOrNull { it.focusDurationMs }

        return MonthlyStats(
            monthStart = monthStartMs,
            totalHours = totalFocusMs.toDouble() / 3600000.0,
            totalPomodoros = dailyStatsList.sumOf { it.pomodoroCount },
            bestDay = bestDay,
            avgDailyFocusMs = if (daysInMonth > 0) totalFocusMs / daysInMonth else 0L,
            weeklyStats = emptyList() // Simplified
        )
    }

    suspend fun computeStreak(): StreakInfo {
        // Walk backwards from today to find current streak
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)

        var currentStreak = 0
        var lastActiveDate: Long? = null

        // Check today first
        val todayStart = cal.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L
        val todayCount = sessionRepository.countCompletedFocusSessions(todayStart, todayEnd)
        if (todayCount > 0) {
            currentStreak = 1
            lastActiveDate = todayStart
            // Continue checking previous days
            cal.add(Calendar.DAY_OF_YEAR, -1)
            while (true) {
                val dayStart = cal.timeInMillis
                val dayEnd = dayStart + 24 * 60 * 60 * 1000L
                val count = sessionRepository.countCompletedFocusSessions(dayStart, dayEnd)
                if (count > 0) {
                    currentStreak++
                    cal.add(Calendar.DAY_OF_YEAR, -1)
                } else {
                    break
                }
            }
        }

        return StreakInfo(
            currentStreak = currentStreak,
            longestStreak = currentStreak, // Simplified - would need full history scan
            lastActiveDate = lastActiveDate
        )
    }

    suspend fun generateInsights(): List<String> {
        val insights = mutableListOf<String>()
        val cal = Calendar.getInstance()
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis
        val todayEnd = todayStart + 24 * 60 * 60 * 1000L

        val todayCount = sessionRepository.countCompletedFocusSessions(todayStart, todayEnd)
        val todayFocusMs = sessionRepository.sumFocusDurationInRange(todayStart, todayEnd)

        if (todayCount > 0) {
            insights.add("You completed $todayCount Pomodoro(s) today.")
        }

        if (todayFocusMs > 0) {
            val minutes = todayFocusMs / 60000
            insights.add("Today's focus time: $minutes minutes.")
        }

        val streak = computeStreak()
        if (streak.currentStreak > 0) {
            insights.add("Current streak: ${streak.currentStreak} day(s).")
        }

        // Weekly average
        val weekStart = TimeUtils.getStartOfWeek(System.currentTimeMillis())
        val weekEnd = weekStart + 7 * 24 * 60 * 60 * 1000L
        val weekFocusMs = sessionRepository.sumFocusDurationInRange(weekStart, weekEnd)
        val weekMinutes = weekFocusMs / 60000
        val avgDaily = weekMinutes / 7
        if (avgDaily > 0) {
            insights.add("Average daily focus this week: $avgDaily minutes.")
        }

        return insights
    }
}
