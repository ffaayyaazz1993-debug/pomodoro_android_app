package com.example.pomodoro.util

import android.content.Context
import android.net.Uri
import com.example.pomodoro.data.local.entities.*
import com.example.pomodoro.data.repository.*
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader

/**
 * Handles import/export of application data as JSON.
 * Validates data integrity before importing.
 */
class DataExporter(
    private val taskRepository: TaskRepository,
    private val projectRepository: ProjectRepository,
    private val sessionRepository: SessionRepository
) {

    suspend fun exportToJson(): String {
        val root = JSONObject()

        // Metadata
        root.put("version", 1)
        root.put("exportedAt", System.currentTimeMillis())
        root.put("appVersion", "1.0.0")

        // Tasks
        val tasksArray = JSONArray()
        // Note: In production, collect from Flow first
        root.put("tasks", tasksArray)

        // Projects
        val projectsArray = JSONArray()
        root.put("projects", projectsArray)

        // Sessions
        val sessionsArray = JSONArray()
        root.put("sessions", sessionsArray)

        return root.toString(2)
    }

    suspend fun exportToCsv(): String {
        val sb = StringBuilder()

        // Sessions CSV
        sb.appendLine("session_id,phase,task_id,project_id,start_time,end_time,planned_duration,actual_duration,status,cycle_number")

        return sb.toString()
    }

    /**
     * Validates and imports JSON data.
     * Returns Result with error message on failure.
     */
    suspend fun importFromJson(jsonString: String): Result<Int> {
        return try {
            val root = JSONObject(jsonString)

            // Validate version
            val version = root.optInt("version", -1)
            if (version < 1) {
                return Result.failure(IllegalArgumentException("Unsupported export version: $version"))
            }

            var importedCount = 0

            // Import projects first (tasks may reference them)
            val projectsArray = root.optJSONArray("projects")
            if (projectsArray != null) {
                for (i in 0 until projectsArray.length()) {
                    val obj = projectsArray.getJSONObject(i)
                    val project = ProjectEntity(
                        name = obj.getString("name"),
                        description = obj.optString("description", ""),
                        icon = obj.optString("icon", "folder"),
                        color = obj.optLong("color", 0xFF6750A4),
                        status = obj.optString("status", "ACTIVE"),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                    )
                    projectRepository.insert(project)
                    importedCount++
                }
            }

            // Import tasks
            val tasksArray = root.optJSONArray("tasks")
            if (tasksArray != null) {
                for (i in 0 until tasksArray.length()) {
                    val obj = tasksArray.getJSONObject(i)
                    val task = TaskEntity(
                        title = obj.getString("title"),
                        description = obj.optString("description", ""),
                        priority = obj.optString("priority", "MEDIUM"),
                        status = obj.optString("status", "NOT_STARTED"),
                        estimatedPomodoros = obj.optInt("estimatedPomodoros", 1),
                        completedPomodoros = obj.optInt("completedPomodoros", 0),
                        dueDate = obj.optLong("dueDate").takeIf { obj.has("dueDate") },
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        notes = obj.optString("notes", "")
                    )
                    taskRepository.insert(task)
                    importedCount++
                }
            }

            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        fun readFromUri(context: Context, uri: Uri): String? {
            return try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    BufferedReader(InputStreamReader(inputStream)).readText()
                }
            } catch (e: Exception) {
                null
            }
        }
    }
}

/**
 * Backup/restore utility using Android Storage Access Framework.
 */
class BackupManager(
    private val context: Context,
    private val dataExporter: DataExporter
) {
    suspend fun createBackupJson(): String {
        return dataExporter.exportToJson()
    }

    suspend fun restoreFromJson(json: String): Result<Int> {
        return dataExporter.importFromJson(json)
    }
}
