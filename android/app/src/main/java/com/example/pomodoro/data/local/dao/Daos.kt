package com.example.pomodoro.data.local.dao

import androidx.room.*
import com.example.pomodoro.data.local.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks WHERE isArchived = 0 ORDER BY createdAt DESC")
    fun getAllActiveTasks(): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE id = :id")
    suspend fun getTaskById(id: Long): TaskEntity?

    @Query("SELECT * FROM tasks WHERE id = :id")
    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?>

    @Query("SELECT * FROM tasks WHERE status = :status ORDER BY createdAt DESC")
    fun getTasksByStatus(status: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE projectId = :projectId AND isArchived = 0 ORDER BY createdAt DESC")
    fun getTasksByProject(projectId: Long): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE title LIKE '%' || :query || '%' OR description LIKE '%' || :query || '%'")
    fun searchTasks(query: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE priority = :priority AND isArchived = 0 ORDER BY createdAt DESC")
    fun getTasksByPriority(priority: String): Flow<List<TaskEntity>>

    @Query("SELECT * FROM tasks WHERE dueDate IS NOT NULL AND dueDate <= :beforeMs AND status != 'COMPLETED' AND status != 'CANCELLED' ORDER BY dueDate ASC")
    fun getOverdueTasks(beforeMs: Long): Flow<List<TaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(task: TaskEntity): Long

    @Update
    suspend fun update(task: TaskEntity)

    @Delete
    suspend fun delete(task: TaskEntity)

    @Query("UPDATE tasks SET completedPomodoros = completedPomodoros + 1 WHERE id = :taskId")
    suspend fun incrementPomodoroCount(taskId: Long)

    @Query("UPDATE tasks SET status = 'COMPLETED', completedAt = :completedAt WHERE id = :taskId")
    suspend fun completeTask(taskId: Long, completedAt: Long = System.currentTimeMillis())

    @Query("UPDATE tasks SET status = :status WHERE id = :taskId")
    suspend fun updateStatus(taskId: Long, status: String)

    @Query("UPDATE tasks SET isArchived = 1 WHERE id = :taskId")
    suspend fun archiveTask(taskId: Long)

    @Query("SELECT COUNT(*) FROM tasks WHERE isArchived = 0")
    fun getActiveTaskCount(): Flow<Int>
}

@Dao
interface ProjectDao {
    @Query("SELECT * FROM projects WHERE status != 'ARCHIVED' ORDER BY createdAt DESC")
    fun getAllActiveProjects(): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE id = :id")
    suspend fun getProjectById(id: Long): ProjectEntity?

    @Query("SELECT * FROM projects WHERE id = :id")
    fun getProjectByIdFlow(id: Long): Flow<ProjectEntity?>

    @Query("SELECT * FROM projects WHERE status = :status ORDER BY createdAt DESC")
    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>>

    @Query("SELECT * FROM projects WHERE name LIKE '%' || :query || '%'")
    fun searchProjects(query: String): Flow<List<ProjectEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("UPDATE projects SET status = :status WHERE id = :projectId")
    suspend fun updateStatus(projectId: Long, status: String)
}

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY startTime DESC")
    fun getAllSessions(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSessionById(id: Long): SessionEntity?

    @Query("SELECT * FROM sessions WHERE sessionId = :sessionId")
    suspend fun getSessionBySessionId(sessionId: String): SessionEntity?

    @Query("SELECT * FROM sessions WHERE phase = 'FOCUS' AND status = 'COMPLETED' ORDER BY startTime DESC LIMIT :limit")
    fun getRecentFocusSessions(limit: Int = 10): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE startTime >= :startMs AND startTime < :endMs ORDER BY startTime DESC")
    fun getSessionsInRange(startMs: Long, endMs: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE taskId = :taskId ORDER BY startTime DESC")
    fun getSessionsByTask(taskId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY startTime DESC")
    fun getSessionsByProject(projectId: Long): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE phase = 'FOCUS' AND status = 'COMPLETED' AND startTime >= :startMs AND startTime < :endMs")
    fun getCompletedFocusSessionsInRange(startMs: Long, endMs: Long): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM sessions WHERE phase = 'FOCUS' AND status = 'COMPLETED' AND startTime >= :startMs AND startTime < :endMs")
    suspend fun countCompletedFocusSessions(startMs: Long, endMs: Long): Int

    @Query("SELECT COALESCE(SUM(actualDurationMs), 0) FROM sessions WHERE phase = 'FOCUS' AND status = 'COMPLETED' AND startTime >= :startMs AND startTime < :endMs")
    suspend fun sumFocusDurationInRange(startMs: Long, endMs: Long): Long

    @Query("SELECT * FROM sessions WHERE status = 'IN_PROGRESS' LIMIT 1")
    suspend fun getActiveSession(): SessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("UPDATE sessions SET status = :status, endTime = :endTime, actualDurationMs = :actualDuration WHERE id = :id")
    suspend fun completeSession(id: Long, status: String, endTime: Long, actualDuration: Long)

    @Query("SELECT * FROM sessions WHERE startTime >= :startMs AND startTime < :endMs AND phase = 'FOCUS' AND status = 'COMPLETED' ORDER BY startTime DESC")
    suspend fun getCompletedFocusSessionsList(startMs: Long, endMs: Long): List<SessionEntity>
}

@Dao
interface SessionEventDao {
    @Query("SELECT * FROM session_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    fun getEventsForSession(sessionId: Long): Flow<List<SessionEventEntity>>

    @Query("SELECT * FROM session_events WHERE sessionId = :sessionId ORDER BY timestamp ASC")
    suspend fun getEventsForSessionList(sessionId: Long): List<SessionEventEntity>

    @Insert
    suspend fun insert(event: SessionEventEntity): Long

    @Insert
    suspend fun insertAll(events: List<SessionEventEntity>)

    @Query("SELECT COUNT(*) FROM session_events WHERE sessionId = :sessionId AND eventType = 'SESSION_PAUSED'")
    suspend fun countPauses(sessionId: Long): Int

    @Query("SELECT * FROM session_events WHERE eventType = :eventType AND timestamp >= :startMs AND timestamp < :endMs ORDER BY timestamp DESC")
    fun getEventsByTypeInRange(eventType: String, startMs: Long, endMs: Long): Flow<List<SessionEventEntity>>
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :id")
    suspend fun getTagById(id: Long): TagEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(tag: TagEntity): Long

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT t.* FROM tags t INNER JOIN task_tags tt ON t.id = tt.tagId WHERE tt.taskId = :taskId")
    fun getTagsForTask(taskId: Long): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTaskTagCrossRef(crossRef: TaskTagCrossRef)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId AND tagId = :tagId")
    suspend fun deleteTaskTagCrossRef(taskId: Long, tagId: Long)

    @Query("DELETE FROM task_tags WHERE taskId = :taskId")
    suspend fun deleteAllTaskTags(taskId: Long)
}

@Dao
interface GoalDao {
    @Query("SELECT * FROM goals WHERE isActive = 1 ORDER BY type ASC")
    fun getActiveGoals(): Flow<List<GoalEntity>>

    @Query("SELECT * FROM goals WHERE id = :id")
    suspend fun getGoalById(id: Long): GoalEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Delete
    suspend fun delete(goal: GoalEntity)

    @Query("UPDATE goals SET currentValue = currentValue + :increment WHERE id = :goalId")
    suspend fun incrementGoal(goalId: Long, increment: Int = 1)

    @Query("SELECT * FROM goals WHERE type = :type AND isActive = 1 LIMIT 1")
    suspend fun getActiveGoalByType(type: String): GoalEntity?
}

@Dao
interface AppStateDao {
    @Query("SELECT * FROM app_state WHERE `key` = :key")
    suspend fun getValue(key: String): AppStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(state: AppStateEntity)

    @Query("DELETE FROM app_state WHERE `key` = :key")
    suspend fun delete(key: String)

    @Query("SELECT * FROM app_state")
    suspend fun getAll(): List<AppStateEntity>
}

@Dao
interface PresetDao {
    @Query("SELECT * FROM saved_presets ORDER BY name ASC")
    fun getAllPresets(): Flow<List<PresetEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(preset: PresetEntity): Long

    @Delete
    suspend fun delete(preset: PresetEntity)
}
