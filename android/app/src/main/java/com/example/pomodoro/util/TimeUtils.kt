package com.example.pomodoro.util

import java.text.SimpleDateFormat
import java.util.*

object TimeUtils {

    fun formatDuration(ms: Long): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return when {
            hours > 0 -> String.format("%dh %dm", hours, minutes)
            minutes > 0 -> String.format("%dm %ds", minutes, seconds)
            else -> String.format("%ds", seconds)
        }
    }

    fun formatTimerDisplay(ms: Long): String {
        val totalSeconds = ms / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    fun formatTime(timestamp: Long): String {
        return SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDate(timestamp: Long): String {
        return SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(timestamp))
    }

    fun formatDateTime(timestamp: Long): String {
        return SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
    }

    fun getStartOfDay(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getEndOfDay(timestamp: Long): Long {
        return getStartOfDay(timestamp) + 24 * 60 * 60 * 1000L - 1
    }

    fun getStartOfWeek(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_WEEK, cal.firstDayOfWeek)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun getStartOfMonth(timestamp: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestamp
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    fun isSameDay(timestamp1: Long, timestamp2: Long): Boolean {
        return getStartOfDay(timestamp1) == getStartOfDay(timestamp2)
    }

    fun minutesToMs(minutes: Int): Long = minutes * 60 * 1000L
    fun msToMinutes(ms: Long): Int = (ms / 60000).toInt()
    fun hoursToMs(hours: Int): Long = hours * 3600 * 1000L
    fun msToHours(ms: Long): Int = (ms / 3600000).toInt()
}

object DateUtils {
    fun getDayName(timestamp: Long): String {
        return SimpleDateFormat("EEEE", Locale.getDefault()).format(Date(timestamp))
    }

    fun getShortDayName(timestamp: Long): String {
        return SimpleDateFormat("EEE", Locale.getDefault()).format(Date(timestamp))
    }

    fun getMonthName(timestamp: Long): String {
        return SimpleDateFormat("MMMM", Locale.getDefault()).format(Date(timestamp))
    }

    fun getDaysBetween(startMs: Long, endMs: Long): Int {
        return ((endMs - startMs) / (24 * 60 * 60 * 1000L)).toInt()
    }
}
