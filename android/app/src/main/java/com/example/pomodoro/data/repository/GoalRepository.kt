package com.example.pomodoro.data.repository

import com.example.pomodoro.data.local.dao.GoalDao
import com.example.pomodoro.data.local.entities.GoalEntity
import kotlinx.coroutines.flow.Flow

class GoalRepository(private val goalDao: GoalDao) {
    fun getActiveGoals(): Flow<List<GoalEntity>> = goalDao.getActiveGoals()
    suspend fun getGoalById(id: Long): GoalEntity? = goalDao.getGoalById(id)
    suspend fun insert(goal: GoalEntity): Long = goalDao.insert(goal)
    suspend fun update(goal: GoalEntity) = goalDao.update(goal)
    suspend fun delete(goal: GoalEntity) = goalDao.delete(goal)
    suspend fun incrementGoal(goalId: Long, increment: Int = 1) = goalDao.incrementGoal(goalId, increment)
    suspend fun getActiveGoalByType(type: String): GoalEntity? = goalDao.getActiveGoalByType(type)
}
