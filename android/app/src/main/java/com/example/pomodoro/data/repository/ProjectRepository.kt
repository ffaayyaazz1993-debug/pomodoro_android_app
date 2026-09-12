package com.example.pomodoro.data.repository

import com.example.pomodoro.data.local.dao.ProjectDao
import com.example.pomodoro.data.local.entities.ProjectEntity
import kotlinx.coroutines.flow.Flow

class ProjectRepository(private val projectDao: ProjectDao) {
    fun getAllActiveProjects(): Flow<List<ProjectEntity>> = projectDao.getAllActiveProjects()
    suspend fun getProjectById(id: Long): ProjectEntity? = projectDao.getProjectById(id)
    fun getProjectByIdFlow(id: Long): Flow<ProjectEntity?> = projectDao.getProjectByIdFlow(id)
    fun getProjectsByStatus(status: String): Flow<List<ProjectEntity>> = projectDao.getProjectsByStatus(status)
    fun searchProjects(query: String): Flow<List<ProjectEntity>> = projectDao.searchProjects(query)
    suspend fun insert(project: ProjectEntity): Long = projectDao.insert(project)
    suspend fun update(project: ProjectEntity) = projectDao.update(project)
    suspend fun delete(project: ProjectEntity) = projectDao.delete(project)
    suspend fun updateStatus(projectId: Long, status: String) = projectDao.updateStatus(projectId, status)
}
