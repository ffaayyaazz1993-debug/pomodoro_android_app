package com.example.pomodoro.data.local.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.pomodoro.data.local.dao.*
import com.example.pomodoro.data.local.entities.*

@Database(
    entities = [
        ProjectEntity::class,
        TaskEntity::class,
        TagEntity::class,
        TaskTagCrossRef::class,
        SessionEntity::class,
        SessionEventEntity::class,
        GoalEntity::class,
        AppStateEntity::class,
        PresetEntity::class
    ],
    version = 1,
    exportSchema = true
)
abstract class PomodoroDatabase : RoomDatabase() {
    abstract fun taskDao(): TaskDao
    abstract fun projectDao(): ProjectDao
    abstract fun sessionDao(): SessionDao
    abstract fun sessionEventDao(): SessionEventDao
    abstract fun tagDao(): TagDao
    abstract fun goalDao(): GoalDao
    abstract fun appStateDao(): AppStateDao
    abstract fun presetDao(): PresetDao

    companion object {
        private const val DATABASE_NAME = "pomodoro_focus.db"

        @Volatile
        private var INSTANCE: PomodoroDatabase? = null

        fun getInstance(context: Context): PomodoroDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): PomodoroDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                PomodoroDatabase::class.java,
                DATABASE_NAME
            )
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
