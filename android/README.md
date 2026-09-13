# PomodoroFocus - Android Pomodoro Productivity App

A complete, production-quality Android Pomodoro productivity application built with modern Android development practices.

## Features

### Core Timer
- **Per-task independent timers** - Each task gets its own timer that runs simultaneously with others
- **Multi-timer manager** - Manage multiple concurrent timers, one per task
- **Timestamp-based timer engine** - Accurate timing using monotonic timestamps, not counters
- **Background execution** - Foreground service keeps timer running when app is backgrounded
- **Process death recovery** - Timer state persists and reconstructs correctly after process death
- **Boot recovery** - Timer state restored after device restart
- **Doze/screen-off support** - Timer remains accurate through Android power management
- **Cycle logic** - Configurable focus/break cycles with long break intervals
- **Auto-start options** - Independent auto-start for focus, short break, and long break

### Task Management
- Full CRUD operations for tasks
- **Per-task timer** - Start/stop/pause timer directly from task
- Priority levels (LOW, MEDIUM, HIGH, CRITICAL)
- Status tracking (NOT_STARTED, IN_PROGRESS, COMPLETED, CANCELLED, ARCHIVED)
- Estimated vs completed Pomodoros with visual progress
- Due dates
- Search and filtering

### Projects
- Create and manage projects with color coding
- Associate tasks with projects
- Filter tasks by project
- Project-level statistics (task counts, Pomodoros, focus time)
- Status tracking (ACTIVE, PAUSED, COMPLETED, ARCHIVED)
- Project detail view with task list

### Tags
- Create custom tags (e.g., Coding, Study, Writing)
- Assign multiple tags per task
- Filter tasks by tag
- Rename and delete tags
- Visual tag chips on task cards
- Tag management dialog

### Projects
- Create and manage projects
- Project-level statistics
- Color coding and icons
- Status tracking (ACTIVE, PAUSED, COMPLETED, ARCHIVED)

### Statistics & Analytics
- Daily, weekly, monthly, and all-time statistics
- Focus time tracking
- Pomodoro counts
- Streak tracking
- Productivity insights (deterministic, no AI)
- Calendar view with daily summaries

### History
- Complete session history
- Filtering by phase, status, date range
- Session details with interruption tracking
- Search capability

### Goals
- Daily Pomodoro goals
- Daily focus time goals
- Task completion goals
- Visual progress tracking

### Android Integration
- **Foreground service** with persistent notification
- **Notification actions** (Pause, Resume, Skip, Stop)
- **Home screen widget** (Glance-based)
- **App shortcuts** (Start Focus, New Task, Open Timer)
- **Keep screen on** during focus sessions
- **Sound and vibration** on completion
- **Material 3** design with dynamic colors

### Data Management
- **Room database** with proper foreign keys and indexes
- **DataStore** for preferences
- **JSON export/import**
- **CSV export**
- **Backup/restore** via Storage Access Framework
- **Offline-first** - No internet required

## Architecture

```
app/src/main/java/com/example/pomodoro/
├── MainActivity.kt                    # Single activity entry point
├── PomodoroApplication.kt             # Application class, DI
├── data/
│   ├── local/
│   │   ├── database/PomodoroDatabase.kt  # Room database
│   │   ├── dao/Daos.kt                   # Data access objects
│   │   └── entities/Entities.kt          # Room entities
│   ├── repository/                    # Data repositories
│   └── preferences/AppPreferences.kt  # DataStore preferences
├── domain/
│   ├── timer/TimerEngine.kt           # Core timer state machine
│   ├── usecase/StatisticsUseCase.kt   # Statistics computations
│   └── model/                         # Domain models
├── service/
│   ├── TimerForegroundService.kt      # Background timer service
│   ├── NotificationService.kt         # Notification management
│   └── receivers/                     # Broadcast receivers
├── ui/
│   ├── navigation/                    # Navigation Compose setup
│   ├── theme/Theme.kt                 # Material 3 theme
│   ├── screens/                       # All UI screens
│   └── components/                    # Reusable UI components
├── widget/PomodoroWidget.kt           # Home screen widget
└── util/                              # Utilities
```

### Timer Architecture

#### Per-Task Independent Timers

Each task gets its own `TimerEngine` instance managed by `MultiTimerManager`. 
Timers run simultaneously and independently - pausing Task 1 doesn't affect Task 2.

```
MultiTimerManager
├── Task #1 → TimerEngine (persisted key: timer_task_1)
├── Task #2 → TimerEngine (persisted key: timer_task_2)
├── Task #3 → TimerEngine (persisted key: timer_task_3)
└── Global  → TimerEngine (persisted key: timer_global)
```

Each timer persists its state independently using unique keys in the `app_state` table.

#### Timestamp-Based Calculation

The timer engine uses **timestamp-based calculation** for accuracy:

```
remaining = plannedDuration - (now - startTimestamp - accumulatedPauseMs)
```

This ensures accuracy regardless of:
- Process death and restart
- Screen off / Doze mode
- System clock changes
- Activity recreation

State is persisted to Room database on every transition and reconstructed on app launch.

### Background Execution

1. **Foreground Service** - Keeps timer running with persistent notification
2. **WakeLock** - Prevents CPU sleep during active timer
3. **AlarmManager** - Backup completion trigger
4. **Timestamps** - Source of truth for remaining time

## Database Schema

### Tables
- `projects` - Project definitions
- `tasks` - Task items with estimation
- `tags` / `task_tags` - Tag system
- `sessions` - Timer session records
- `session_events` - Event log (start, pause, resume, complete)
- `goals` - User-defined goals
- `app_state` - Timer state persistence
- `saved_presets` - Timer presets

## Permissions

| Permission | Purpose |
|---|---|
| `POST_NOTIFICATIONS` | Show timer and completion notifications |
| `FOREGROUND_SERVICE` | Run timer in background |
| `FOREGROUND_SERVICE_SPECIAL_USE` | Timer foreground service type |
| `RECEIVE_BOOT_COMPLETED` | Restore timer after device restart |
| `VIBRATE` | Haptic feedback on completion |
| `WAKE_LOCK` | Keep CPU awake during active timer |
| `USE_EXACT_ALARM` | Precise timer completion |

## Build

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 35
- Kotlin 2.0.21

### Build Commands
```bash
# Debug build
./gradlew assembleDebug

# Release build
./gradlew assembleRelease

# Run tests
./gradlew test

# Run instrumented tests
./gradlew connectedAndroidTest

# Generate AAB
./gradlew bundleRelease
```

### APK Location
- Debug: `app/build/outputs/apk/debug/app-debug.apk`
- Release: `app/build/outputs/apk/release/app-release.apk`

## Testing

### Unit Tests
- `TimerEngineTest` - Timer state machine, timestamp accuracy, recovery
- `TimeUtilsTest` - Time formatting and calculations

### Test Coverage
- Timer start/pause/resume/stop/skip/complete
- Timestamp accuracy with clock manipulation
- Process death recovery simulation
- Cycle logic (short break, long break, rollover)
- Session tracking and counting
- Edge cases (idle state transitions, time limits)

## Configuration

### Default Timer Settings
| Setting | Default |
|---|---|
| Focus Duration | 25 minutes |
| Short Break | 5 minutes |
| Long Break | 15 minutes |
| Long Break Interval | 4 focus sessions |

### Presets
- **Classic**: 25/5/15
- **Extended**: 50/10/20
- **Short**: 15/3/10
- **Custom**: User-defined

## Privacy

- **100% offline** - No internet connection required
- **No analytics** - No tracking or telemetry
- **No accounts** - No sign-up or cloud sync
- **Local storage** - All data stored on device
- **No ads** - Completely ad-free

## Android Version Requirements

- **Minimum SDK**: 29 (Android 10)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 35

## Known Limitations

1. **Exact alarms** may be restricted on some devices/battery savers
2. **Widget updates** depend on Android's update schedule
3. **PiP mode** not implemented (can be added for Android 12+)
4. **Wear OS** companion not included
5. **Cloud sync** intentionally not included (privacy-first design)

## Technology Stack

- **Kotlin** 2.0.21
- **Jetpack Compose** with Material 3
- **Room** 2.6.1 (database)
- **DataStore** 1.1.1 (preferences)
- **Navigation Compose** 2.8.2
- **Glance** 1.1.0 (widgets)
- **WorkManager** 2.9.1
- **Kotlin Coroutines** 1.8.1

## License

This project is provided as-is for educational and personal use.

## Contributing

This is a reference implementation. For production use, consider:
- Adding more comprehensive error handling
- Implementing database migrations
- Adding accessibility testing
- Performance profiling with large datasets
- Adding more unit and integration tests
