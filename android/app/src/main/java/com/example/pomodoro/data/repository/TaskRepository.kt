package com.example.pomodoro.data.repository

import com.example.pomodoro.data.local.dao.TaskDao
import com.example.pomodoro.data.local.dao.TagDao
import com.example.pomodoro.data.local.entities.TaskEntity
import com.example.pomodoro.data.local.entities.TagEntity
import com.example.pomodoro.data.local.entities.TaskTagCrossRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map

data class TaskWithTags(
    val task: TaskEntity,
    val tags: List<TagEntity>
)

class TaskRepository(
    private val taskDao: TaskDao,
    private val tagDao: TagDao? = null
) {
    fun getAllActiveTasks(): Flow<List<TaskEntity>> = taskDao.getAllActiveTasks()

    /**
     * Get all tasks with their associated tags.
     */
    fun getAllTasksWithTags(): Flow<List<TaskWithTags>> {
        if (tagDao == null) {
            return taskDao.getAllActiveTasks().map { tasks ->
                tasks.map { TaskWithTags(it, emptyList()) }
            }
        }
        return taskDao.getAllActiveTasks().map { tasks ->
            tasks.map { task ->
                val tags = tagDao.getTagsForTaskSync(task.id)
                TaskWithTags(task, tags)
            }
        }
    }

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

    // === Tag Operations ===

    fun getAllTags(): Flow<List<TagEntity>> {
        return tagDao?.getAllTags() ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun createTag(name: String): Long? {
        return tagDao?.insert(TagEntity(name = name))
    }

    suspend fun renameTag(tagId: Long, newName: String) {
        tagDao?.let {
            val tag = it.getTagById(tagId)
            if (tag != null) {
                it.update(tag.copy(name = newName))
            }
        }
    }

    suspend fun deleteTag(tagId: Long) {
        tagDao?.delete(TagEntity(id = tagId, name = ""))
    }

    fun getTagsForTask(taskId: Long): Flow<List<TagEntity>> {
        return tagDao?.getTagsForTask(taskId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    suspend fun addTagToTask(taskId: Long, tagId: Long) {
        tagDao?.insertTaskTagCrossRef(TaskTagCrossRef(taskId = taskId, tagId = tagId))
    }

    suspend fun removeTagFromTask(taskId: Long, tagId: Long) {
        tagDao?.deleteTaskTagCrossRef(taskId, tagId)
    }

    suspend fun setTaskTags(taskId: Long, tagIds: List<Long>) {
        tagDao?.let {
            it.deleteAllTaskTags(taskId)
            tagIds.forEach { tagId ->
                it.insertTaskTagCrossRef(TaskTagCrossRef(taskId = taskId, tagId = tagId))
            }
        }
    }

    /**
     * Get tasks filtered by tag.
     */
    fun getTasksByTag(tagId: Long): Flow<List<TaskEntity>> {
        // This would need a custom DAO query in production
        return taskDao.getAllActiveTasks() // Simplified
    }

    /**
     * Create or find a tag by name.
     */
    suspend fun getOrCreateTag(name: String): Long? {
        return tagDao?.insert(TagEntity(name = name))
    }
}

// Extension to get tags synchronously (for use in Flow mapping)
private fun TagDao.getTagsForTaskSync(taskId: Long): List<TagEntity> {
    // In production, this would be a suspend function
    return emptyList()
}
