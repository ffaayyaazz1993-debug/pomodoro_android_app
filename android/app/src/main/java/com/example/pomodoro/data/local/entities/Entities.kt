package com.example.pomodoro.data.local.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "projects",
    indices = [Index("name"), Index("status")]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val description: String = "",
    val icon: String = "folder",
    val color: Long = 0xFF6750A4,
    val status: String = "ACTIVE", // ACTIVE, PAUSED, COMPLETED, ARCHIVED
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null
)

@Entity(
    tableName = "tasks",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("projectId"), Index("status"), Index("priority"), Index("dueDate")]
)
data class TaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val description: String = "",
    val projectId: Long? = null,
    val priority: String = "MEDIUM", // LOW, MEDIUM, HIGH, CRITICAL
    val status: String = "NOT_STARTED", // NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED, ARCHIVED
    val estimatedPomodoros: Int = 1,
    val completedPomodoros: Int = 0,
    val dueDate: Long? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val completedAt: Long? = null,
    val notes: String = "",
    val isArchived: Boolean = false
)

@Entity(
    tableName = "tags",
    indices = [Index("name", unique = true)]
)
data class TagEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "task_tags",
    primaryKeys = ["taskId", "tagId"],
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("taskId"), Index("tagId")]
)
data class TaskTagCrossRef(
    val taskId: Long,
    val tagId: Long
)

@Entity(
    tableName = "sessions",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index("taskId"), Index("projectId"), Index("phase"), Index("startTime"), Index("status")]
)
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: String, // UUID for uniqueness
    val phase: String, // FOCUS, SHORT_BREAK, LONG_BREAK, CUSTOM
    val taskId: Long? = null,
    val projectId: Long? = null,
    val startTime: Long,
    val endTime: Long? = null,
    val plannedDurationMs: Long,
    val actualDurationMs: Long = 0,
    val pausedDurationMs: Long = 0,
    val status: String = "IN_PROGRESS", // IN_PROGRESS, COMPLETED, CANCELLED, SKIPPED, ABANDONED, MANUALLY_COMPLETED
    val interruptionCount: Int = 0,
    val notes: String = "",
    val cycleNumber: Int = 1
)

@Entity(
    tableName = "session_events",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId"), Index("eventType"), Index("timestamp")]
)
data class SessionEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val eventType: String, // SESSION_STARTED, SESSION_PAUSED, SESSION_RESUMED, SESSION_COMPLETED, SESSION_SKIPPED, SESSION_CANCELLED
    val timestamp: Long = System.currentTimeMillis(),
    val metadata: String = "" // JSON for extra data
)

@Entity(
    tableName = "goals")
data class GoalEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val type: String, // DAILY_POMODOROS, DAILY_FOCUS_TIME, DAILY_TASKS, WEEKLY_POMODOROS, WEEKLY_FOCUS_TIME
    val targetValue: Int,
    val currentValue: Int = 0,
    val periodStart: Long,
    val periodEnd: Long,
    val isActive: Boolean = true
)

@Entity(
    tableName = "app_state")
data class AppStateEntity(
    @PrimaryKey val key: String,
    val value: String,
    val updatedAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "saved_presets")
data class PresetEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val focusDurationMin: Int,
    val shortBreakMin: Int,
    val longBreakMin: Int,
    val longBreakInterval: Int,
    val isDefault: Boolean = false
)
