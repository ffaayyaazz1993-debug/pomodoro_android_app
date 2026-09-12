package com.example.pomodoro.data.repository

import com.example.pomodoro.data.local.dao.TaskDao
import com.example.pomodoro.data.local.dao.TagDao
import com.example.pomodoro.data.local.entities.TaskEntity
import com.example.pomodoro.data.local.entities.TaskTagCrossRef
import kotlinx.coroutines.flow.Flow

class TaskRepository(
    private val taskDao: TaskDao,
    private val tagDao: TagDao? = null
) {
    fun getAllActiveTasks(): Flow<List<TaskEntity>> = taskDao.getAllActiveTasks()
    suspend fun getTaskById(id: Long): TaskEntity? = taskDao.getTaskById(id)
    fun getTaskByIdFlow(id: Long): Flow<TaskEntity?> = taskDao.getTaskByIdFlow(id)
    fun getTasksByStatus(status: String): Flow<List<TaskEntity>> = taskDao.getTasksByStatus(status)
    fun getTasksByProject(projectId: Long): Flow<List<TaskEntity>> = taskDao.getTasksByProject(projectId)
    fun searchTasks(query: String): Flow<List<TaskEntity>> = taskDao.searchTasks(query)
    fun getTasksByPriority(priority: String): Flow<List<TaskEntity>> = taskDao.getTasksByPriority(priority)
    fun getOverdueTasks(beforeMs: Long): Flow<List<TaskEntity>> = taskDao.getOverdueTasks(beforeMs)
    suspend fun insert(task: TaskEntity): Long = taskDao.insert(task)
    suspend fun update(task: TaskEntity) = taskDao.update(task)
    suspend fun delete(task: TaskEntity) = taskDao.delete(task)
    suspend fun incrementPomodoroCount(taskId: Long) = taskDao.incrementPomodoroCount(taskId)
    suspend fun completeTask(taskId: Long) = taskDao.completeTask(taskId)
    suspend fun updateStatus(taskId: Long, status: String) = taskDao.updateStatus(taskId, status)
    suspend fun archiveTask(taskId: Long) = taskDao.archiveTask(taskId)
    fun getActiveTaskCount(): Flow<Int> = taskDao.getActiveTaskCount()
}
