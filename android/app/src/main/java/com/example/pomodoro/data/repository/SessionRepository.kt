package com.example.pomodoro.data.repository

import com.example.pomodoro.data.local.dao.SessionDao
import com.example.pomodoro.data.local.dao.SessionEventDao
import com.example.pomodoro.data.local.entities.SessionEntity
import com.example.pomodoro.data.local.entities.SessionEventEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class SessionRepository(
    private val sessionDao: SessionDao,
    private val eventDao: SessionEventDao
) {
    fun getAllSessions(): Flow<List<SessionEntity>> = sessionDao.getAllSessions()
    suspend fun getSessionById(id: Long): SessionEntity? = sessionDao.getSessionById(id)
    suspend fun getSessionBySessionId(sessionId: String): SessionEntity? = sessionDao.getSessionBySessionId(sessionId)
    fun getRecentFocusSessions(limit: Int = 10): Flow<List<SessionEntity>> = sessionDao.getRecentFocusSessions(limit)
    fun getSessionsInRange(startMs: Long, endMs: Long): Flow<List<SessionEntity>> = sessionDao.getSessionsInRange(startMs, endMs)
    fun getSessionsByTask(taskId: Long): Flow<List<SessionEntity>> = sessionDao.getSessionsByTask(taskId)
    fun getSessionsByProject(projectId: Long): Flow<List<SessionEntity>> = sessionDao.getSessionsByProject(projectId)
    fun getCompletedFocusSessionsInRange(startMs: Long, endMs: Long): Flow<List<SessionEntity>> = sessionDao.getCompletedFocusSessionsInRange(startMs, endMs)
    suspend fun countCompletedFocusSessions(startMs: Long, endMs: Long): Int = sessionDao.countCompletedFocusSessions(startMs, endMs)
    suspend fun sumFocusDurationInRange(startMs: Long, endMs: Long): Long = sessionDao.sumFocusDurationInRange(startMs, endMs)
    suspend fun getActiveSession(): SessionEntity? = sessionDao.getActiveSession()

    suspend fun createSession(
        phase: String,
        plannedDurationMs: Long,
        taskId: Long? = null,
        projectId: Long? = null,
        cycleNumber: Int = 1
    ): SessionEntity {
        val session = SessionEntity(
            sessionId = UUID.randomUUID().toString(),
            phase = phase,
            taskId = taskId,
            projectId = projectId,
            startTime = System.currentTimeMillis(),
            plannedDurationMs = plannedDurationMs,
            cycleNumber = cycleNumber
        )
        val id = sessionDao.insert(session)
        return session.copy(id = id)
    }

    suspend fun completeSession(sessionId: Long, status: String, endTime: Long, actualDuration: Long) {
        sessionDao.completeSession(sessionId, status, endTime, actualDuration)
    }

    suspend fun logEvent(sessionId: Long, eventType: String, metadata: String = "") {
        eventDao.insert(
            SessionEventEntity(
                sessionId = sessionId,
                eventType = eventType,
                timestamp = System.currentTimeMillis(),
                metadata = metadata
            )
        )
    }

    fun getEventsForSession(sessionId: Long): Flow<List<SessionEventEntity>> = eventDao.getEventsForSession(sessionId)
    suspend fun countPauses(sessionId: Long): Int = eventDao.countPauses(sessionId)

    suspend fun getCompletedFocusSessionsList(startMs: Long, endMs: Long): List<SessionEntity> =
        sessionDao.getCompletedFocusSessionsList(startMs, endMs)

    suspend fun insertSession(session: SessionEntity): Long = sessionDao.insert(session)
    suspend fun updateSession(session: SessionEntity) = sessionDao.update(session)
}
