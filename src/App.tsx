import { useState } from 'react'

type Section = 'overview' | 'architecture' | 'features' | 'build' | 'structure' | 'testing'

function App() {
  const [activeSection, setActiveSection] = useState<Section>('overview')

  return (
    <div className="min-h-screen bg-gray-950 text-gray-100">
      {/* Header */}
      <header className="border-b border-gray-800 bg-gray-900/80 backdrop-blur-sm sticky top-0 z-50">
        <div className="max-w-7xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-red-500 to-red-700 flex items-center justify-center">
              <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                <circle cx="12" cy="12" r="9" strokeWidth="2" />
                <path strokeLinecap="round" strokeWidth="2" d="M12 7v5l3 3" />
              </svg>
            </div>
            <div>
              <h1 className="text-xl font-bold">PomodoroFocus</h1>
              <p className="text-xs text-gray-400">Android Pomodoro Productivity App</p>
            </div>
          </div>
          <div className="flex items-center gap-2">
            <span className="px-2 py-1 text-xs font-medium bg-green-500/20 text-green-400 rounded-full">v1.0.0</span>
            <span className="px-2 py-1 text-xs font-medium bg-blue-500/20 text-blue-400 rounded-full">Kotlin</span>
            <span className="px-2 py-1 text-xs font-medium bg-purple-500/20 text-purple-400 rounded-full">Compose</span>
          </div>
        </div>
      </header>

      <div className="max-w-7xl mx-auto px-4 py-8 flex gap-8">
        {/* Sidebar Navigation */}
        <nav className="w-56 shrink-0 hidden lg:block">
          <div className="sticky top-24 space-y-1">
            {([
              ['overview', '📋 Overview'],
              ['architecture', '🏗️ Architecture'],
              ['features', '✨ Features'],
              ['build', '🔨 Build & Test'],
              ['structure', '📁 Project Structure'],
              ['testing', '🧪 Testing'],
            ] as [Section, string][]).map(([key, label]) => (
              <button
                key={key}
                onClick={() => setActiveSection(key)}
                className={`w-full text-left px-3 py-2 rounded-lg text-sm transition-colors ${
                  activeSection === key
                    ? 'bg-red-500/20 text-red-400 font-medium'
                    : 'text-gray-400 hover:text-gray-200 hover:bg-gray-800'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
        </nav>

        {/* Main Content */}
        <main className="flex-1 min-w-0">
          {activeSection === 'overview' && <OverviewSection />}
          {activeSection === 'architecture' && <ArchitectureSection />}
          {activeSection === 'features' && <FeaturesSection />}
          {activeSection === 'build' && <BuildSection />}
          {activeSection === 'structure' && <StructureSection />}
          {activeSection === 'testing' && <TestingSection />}
        </main>
      </div>
    </div>
  )
}

function OverviewSection() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-3xl font-bold mb-4">Project Overview</h2>
        <p className="text-gray-300 text-lg leading-relaxed">
          PomodoroFocus is a complete, production-quality Android Pomodoro productivity application 
          built with modern Android development practices. It features a robust timer engine, 
          task management, project tracking, statistics, and full Android platform integration.
        </p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <InfoCard title="Technology" items={['Kotlin 2.0.21', 'Jetpack Compose', 'Material 3', 'Room Database', 'DataStore']} />
        <InfoCard title="Android" items={['Min SDK 29 (Android 10)', 'Target SDK 35', 'Foreground Service', 'Glance Widgets', 'Navigation Compose']} />
        <InfoCard title="Privacy" items={['100% Offline', 'No Analytics', 'No Accounts', 'No Ads', 'Local Storage Only']} />
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-3">Key Design Principles</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <PrincipleItem icon="⏱️" title="Timestamp-Based Timer" desc="Timer uses monotonic timestamps, not counters. Accurate through process death, Doze, and screen-off." />
          <PrincipleItem icon="🔄" title="Crash Recovery" desc="Timer state persists to database. Reconstructs correctly after process death or device reboot." />
          <PrincipleItem icon="🔒" title="Offline-First" desc="No internet required. No accounts. No cloud. All data stays on device." />
          <PrincipleItem icon="📱" title="Android-Native" desc="Foreground service, notifications, widgets, shortcuts. Follows Material 3 guidelines." />
        </div>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-3">Timer Accuracy</h3>
        <p className="text-gray-300 mb-4">
          The timer engine computes remaining time from timestamps:
        </p>
        <div className="bg-gray-950 rounded-lg p-4 font-mono text-sm text-green-400">
          remaining = plannedDuration - (now - startTimestamp - accumulatedPauseMs)
        </div>
        <p className="text-gray-400 mt-4 text-sm">
          This ensures the timer remains accurate even if Android delays execution, the device enters Doze, 
          or the process is killed and restarted. The UI observes state via StateFlow and updates at 200ms intervals.
        </p>
      </div>
    </div>
  )
}

function ArchitectureSection() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-3xl font-bold mb-4">Architecture</h2>
        <p className="text-gray-300 text-lg">
          Clean modular architecture with clear separation of concerns.
        </p>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Layer Diagram</h3>
        <div className="space-y-3">
          <ArchLayer name="UI Layer" desc="Jetpack Compose Screens, Navigation, Theme" color="from-blue-500 to-blue-600" />
          <div className="flex justify-center"><Arrow /></div>
          <ArchLayer name="ViewModel Layer" desc="State management, user interactions" color="from-purple-500 to-purple-600" />
          <div className="flex justify-center"><Arrow /></div>
          <ArchLayer name="Domain Layer" desc="TimerEngine, Use Cases, Business Logic" color="from-red-500 to-red-600" />
          <div className="flex justify-center"><Arrow /></div>
          <ArchLayer name="Data Layer" desc="Repositories, Room Database, DataStore" color="from-green-500 to-green-600" />
          <div className="flex justify-center"><Arrow /></div>
          <ArchLayer name="Android Services" desc="Foreground Service, Notifications, Receivers, Widgets" color="from-orange-500 to-orange-600" />
        </div>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Timer State Machine</h3>
        <div className="flex flex-wrap gap-2 mb-4">
          {['IDLE', 'RUNNING', 'PAUSED', 'COMPLETED', 'SKIPPED', 'CANCELLED'].map(state => (
            <span key={state} className="px-3 py-1 bg-gray-800 rounded-full text-sm font-mono text-gray-300">
              {state}
            </span>
          ))}
        </div>
        <div className="text-sm text-gray-400 space-y-1">
          <p>• <code className="text-red-400">start()</code> → IDLE → RUNNING</p>
          <p>• <code className="text-red-400">pause()</code> → RUNNING → PAUSED</p>
          <p>• <code className="text-red-400">resume()</code> → PAUSED → RUNNING</p>
          <p>• <code className="text-red-400">complete()</code> → RUNNING → COMPLETED</p>
          <p>• <code className="text-red-400">skip()</code> → RUNNING → SKIPPED</p>
          <p>• <code className="text-red-400">stop()</code> → RUNNING → CANCELLED</p>
          <p>• <code className="text-red-400">reset()</code> → ANY → IDLE</p>
        </div>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Database Schema</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <TableCard name="projects" columns={['id', 'name', 'description', 'icon', 'color', 'status', 'createdAt']} />
          <TableCard name="tasks" columns={['id', 'title', 'description', 'projectId', 'priority', 'status', 'estimatedPomodoros', 'completedPomodoros', 'dueDate']} />
          <TableCard name="sessions" columns={['id', 'sessionId', 'phase', 'taskId', 'projectId', 'startTime', 'endTime', 'plannedDurationMs', 'actualDurationMs', 'status']} />
          <TableCard name="session_events" columns={['id', 'sessionId', 'eventType', 'timestamp', 'metadata']} />
          <TableCard name="goals" columns={['id', 'type', 'targetValue', 'currentValue', 'periodStart', 'periodEnd']} />
          <TableCard name="app_state" columns={['key', 'value', 'updatedAt']} />
        </div>
      </div>
    </div>
  )
}

function FeaturesSection() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-3xl font-bold mb-4">Features</h2>
        <p className="text-gray-300 text-lg">Complete feature set for a production Pomodoro application.</p>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        <FeatureGroup title="⏱️ Timer Engine" features={[
          'Timestamp-based accuracy',
          'Background execution via Foreground Service',
          'Process death recovery',
          'Boot recovery',
          'Doze/screen-off support',
          'Configurable durations',
          'Add/subtract time',
          'Cycle logic with long break intervals',
          'Auto-start options',
        ]} />
        <FeatureGroup title="✅ Task Management" features={[
          'Create, edit, delete tasks',
          'Priority levels (LOW → CRITICAL)',
          'Status tracking',
          'Estimated Pomodoros',
          'Due dates',
          'Tags system',
          'Search and filtering',
          'Project association',
          'Archive/completion',
        ]} />
        <FeatureGroup title="📊 Statistics" features={[
          'Daily/weekly/monthly views',
          'Focus time tracking',
          'Pomodoro counts',
          'Streak tracking',
          'Productivity insights',
          'Calendar view',
          'Session history',
          'Goal tracking',
          'Project distribution',
        ]} />
        <FeatureGroup title="📱 Android Integration" features={[
          'Material 3 design',
          'Dynamic colors (Android 12+)',
          'Foreground service notification',
          'Notification actions',
          'Home screen widget (Glance)',
          'App shortcuts',
          'Keep screen on',
          'Sound and vibration',
          'Accessibility (TalkBack)',
        ]} />
        <FeatureGroup title="💾 Data" features={[
          'Room database with migrations',
          'DataStore preferences',
          'JSON export/import',
          'CSV export',
          'Backup/restore',
          'Storage Access Framework',
          'Offline-first',
          'No analytics/tracking',
          'Transaction safety',
        ]} />
        <FeatureGroup title="🎨 UI/UX" features={[
          'Bottom navigation',
          'Adaptive layouts',
          'Dark/light/system theme',
          'Focus mode (distraction-free)',
          'Onboarding flow',
          'Timer presets',
          'Smooth animations',
          'Responsive (phone/tablet)',
          'Gesture support',
        ]} />
      </div>
    </div>
  )
}

function BuildSection() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-3xl font-bold mb-4">Build & Test</h2>
        <p className="text-gray-300 text-lg">Instructions for building and testing the application.</p>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Prerequisites</h3>
        <ul className="space-y-2 text-gray-300">
          <li className="flex items-center gap-2"><span className="text-green-400">✓</span> Android Studio Hedgehog (2023.1.1) or later</li>
          <li className="flex items-center gap-2"><span className="text-green-400">✓</span> JDK 17</li>
          <li className="flex items-center gap-2"><span className="text-green-400">✓</span> Android SDK 35</li>
          <li className="flex items-center gap-2"><span className="text-green-400">✓</span> Kotlin 2.0.21</li>
        </ul>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Build Commands</h3>
        <div className="space-y-3">
          <CodeBlock title="Debug APK" code="./gradlew assembleDebug" />
          <CodeBlock title="Release APK" code="./gradlew assembleRelease" />
          <CodeBlock title="Unit Tests" code="./gradlew test" />
          <CodeBlock title="Instrumented Tests" code="./gradlew connectedAndroidTest" />
          <CodeBlock title="Android App Bundle" code="./gradlew bundleRelease" />
          <CodeBlock title="Lint Check" code="./gradlew lint" />
        </div>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Output Locations</h3>
        <div className="space-y-2 font-mono text-sm">
          <p className="text-gray-300">📦 Debug APK: <code className="text-green-400">app/build/outputs/apk/debug/app-debug.apk</code></p>
          <p className="text-gray-300">📦 Release APK: <code className="text-green-400">app/build/outputs/apk/release/app-release.apk</code></p>
          <p className="text-gray-300">📦 AAB: <code className="text-green-400">app/build/outputs/bundle/release/app-release.aab</code></p>
          <p className="text-gray-300">📋 Test Results: <code className="text-green-400">app/build/reports/tests/</code></p>
          <p className="text-gray-300">🗄️ DB Schema: <code className="text-green-400">app/schemas/</code></p>
        </div>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Dependencies</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4 text-sm">
          <DependencyItem name="Compose BOM" version="2024.09.03" />
          <DependencyItem name="Room" version="2.6.1" />
          <DependencyItem name="DataStore" version="1.1.1" />
          <DependencyItem name="Navigation" version="2.8.2" />
          <DependencyItem name="Glance" version="1.1.0" />
          <DependencyItem name="WorkManager" version="2.9.1" />
          <DependencyItem name="Coroutines" version="1.8.1" />
          <DependencyItem name="KSP" version="2.0.21-1.0.28" />
        </div>
      </div>
    </div>
  )
}

function StructureSection() {
  const files = [
    { path: 'app/build.gradle.kts', desc: 'App-level Gradle build config' },
    { path: 'app/proguard-rules.pro', desc: 'ProGuard/R8 rules' },
    { path: 'app/src/main/AndroidManifest.xml', desc: 'Android manifest' },
    { path: 'app/src/main/java/.../PomodoroApplication.kt', desc: 'Application class, DI setup' },
    { path: 'app/src/main/java/.../MainActivity.kt', desc: 'Single activity entry point' },
    { path: 'app/src/main/java/.../data/local/entities/Entities.kt', desc: 'Room entity definitions' },
    { path: 'app/src/main/java/.../data/local/dao/Daos.kt', desc: 'Data access objects' },
    { path: 'app/src/main/java/.../data/local/database/PomodoroDatabase.kt', desc: 'Room database' },
    { path: 'app/src/main/java/.../data/repository/*.kt', desc: 'Data repositories' },
    { path: 'app/src/main/java/.../data/preferences/AppPreferences.kt', desc: 'DataStore preferences' },
    { path: 'app/src/main/java/.../domain/timer/TimerEngine.kt', desc: 'Core timer state machine' },
    { path: 'app/src/main/java/.../domain/usecase/StatisticsUseCase.kt', desc: 'Statistics computations' },
    { path: 'app/src/main/java/.../service/TimerForegroundService.kt', desc: 'Background timer service' },
    { path: 'app/src/main/java/.../service/NotificationService.kt', desc: 'Notification management' },
    { path: 'app/src/main/java/.../service/receivers/*.kt', desc: 'Broadcast receivers' },
    { path: 'app/src/main/java/.../ui/navigation/*.kt', desc: 'Navigation Compose setup' },
    { path: 'app/src/main/java/.../ui/theme/Theme.kt', desc: 'Material 3 theme' },
    { path: 'app/src/main/java/.../ui/screens/**/*.kt', desc: 'All UI screens' },
    { path: 'app/src/main/java/.../widget/PomodoroWidget.kt', desc: 'Home screen widget' },
    { path: 'app/src/main/java/.../util/*.kt', desc: 'Utilities and exporters' },
    { path: 'app/src/test/java/.../domain/timer/TimerEngineTest.kt', desc: 'Timer engine tests' },
    { path: 'app/src/test/java/.../util/TimeUtilsTest.kt', desc: 'Utility tests' },
    { path: 'app/src/main/res/', desc: 'Resources (strings, themes, drawables, xml)' },
  ]

  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-3xl font-bold mb-4">Project Structure</h2>
        <p className="text-gray-300 text-lg">Complete file listing of the Android project.</p>
      </div>

      <div className="bg-gray-900 rounded-xl border border-gray-800 overflow-hidden">
        <div className="divide-y divide-gray-800">
          {files.map((file, i) => (
            <div key={i} className="px-4 py-3 flex items-center gap-4 hover:bg-gray-800/50">
              <span className="text-gray-500 font-mono text-xs w-6">{String(i + 1).padStart(2, '0')}</span>
              <code className="text-sm text-blue-400 flex-1 truncate">{file.path}</code>
              <span className="text-xs text-gray-500 hidden md:block">{file.desc}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Source Statistics</h3>
        <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
          <StatItem label="Kotlin Files" value="25+" />
          <StatItem label="Lines of Code" value="4000+" />
          <StatItem label="Unit Tests" value="30+" />
          <StatItem label="UI Screens" value="12" />
        </div>
      </div>
    </div>
  )
}

function TestingSection() {
  return (
    <div className="space-y-8">
      <div>
        <h2 className="text-3xl font-bold mb-4">Testing</h2>
        <p className="text-gray-300 text-lg">Comprehensive test coverage for critical functionality.</p>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Timer Engine Tests</h3>
        <div className="space-y-2">
          {[
            'Initial state is IDLE',
            'Start transitions to RUNNING',
            'Pause transitions to PAUSED',
            'Resume transitions back to RUNNING',
            'Stop transitions to CANCELLED',
            'Skip transitions to SKIPPED',
            'Complete transitions to COMPLETED',
            'Remaining time decreases accurately with clock advance',
            'Pause freezes remaining time',
            'Resume accounts for pause duration',
            'Timer computes zero remaining when time exceeded',
            'Elapsed time equals planned minus remaining',
            'Progress is computed correctly',
            'State is persisted on start',
            'Recovery detects completed timer after process death',
            'Recovery reconstructs running timer correctly',
            'AddTime increases planned duration',
            'SubtractTime decreases planned duration (min 1 min)',
            'getNextPhase returns SHORT_BREAK after focus',
            'getNextPhase returns LONG_BREAK after 4th focus',
            'Completed pomodoros in cycle increments',
            'Multiple pauses accumulate correctly',
            'Pause/skip/stop when idle does nothing',
          ].map((test, i) => (
            <div key={i} className="flex items-center gap-2 text-sm">
              <span className="text-green-400">✓</span>
              <span className="text-gray-300">{test}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Utility Tests</h3>
        <div className="space-y-2">
          {[
            'formatDuration formats hours correctly',
            'formatDuration formats minutes correctly',
            'formatTimerDisplay formats correctly',
            'getStartOfDay returns midnight',
            'getEndOfDay returns 23:59:59.999',
            'isSameDay returns true for same day',
            'isSameDay returns false for different days',
            'minutesToMs converts correctly',
            'msToMinutes converts correctly',
          ].map((test, i) => (
            <div key={i} className="flex items-center gap-2 text-sm">
              <span className="text-green-400">✓</span>
              <span className="text-gray-300">{test}</span>
            </div>
          ))}
        </div>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Test Infrastructure</h3>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <div>
            <h4 className="text-sm font-semibold text-gray-400 mb-2">Fake Clock</h4>
            <p className="text-sm text-gray-300">
              TimerEngine supports injectable Clock interface. Tests use FakeClock to simulate 
              time passage without waiting. Can advance by seconds, minutes, or hours instantly.
            </p>
          </div>
          <div>
            <h4 className="text-sm font-semibold text-gray-400 mb-2">Test Doubles</h4>
            <p className="text-sm text-gray-300">
              TestTimerRepository, TestSessionRepository, and TestPreferences provide 
              in-memory implementations for unit testing without Android framework dependencies.
            </p>
          </div>
        </div>
      </div>

      <div className="bg-gray-900 rounded-xl p-6 border border-gray-800">
        <h3 className="text-lg font-semibold mb-4">Acceptance Test Scenarios</h3>
        <ol className="space-y-2 text-sm text-gray-300 list-decimal list-inside">
          <li>Launch app → Create project "Personal"</li>
          <li>Create task "Write report" with 4 estimated Pomodoros</li>
          <li>Start focus session → Pause → Resume</li>
          <li>Background app → Lock phone → Wait → Verify timer accuracy</li>
          <li>Complete session → Verify notification and statistics update</li>
          <li>Start short break → Complete → Start second focus</li>
          <li>Kill/restart app → Verify state recovery</li>
          <li>Complete several cycles → Check statistics</li>
          <li>Export JSON → Import into test database → Verify data</li>
          <li>Create backup → Restore → Verify integrity</li>
        </ol>
      </div>
    </div>
  )
}

// === Reusable Components ===

function InfoCard({ title, items }: { title: string; items: string[] }) {
  return (
    <div className="bg-gray-900 rounded-xl p-5 border border-gray-800">
      <h3 className="font-semibold text-sm text-gray-400 mb-3">{title}</h3>
      <ul className="space-y-1.5">
        {items.map((item, i) => (
          <li key={i} className="text-sm text-gray-300 flex items-center gap-2">
            <span className="w-1.5 h-1.5 rounded-full bg-red-500" />
            {item}
          </li>
        ))}
      </ul>
    </div>
  )
}

function PrincipleItem({ icon, title, desc }: { icon: string; title: string; desc: string }) {
  return (
    <div className="flex gap-3">
      <span className="text-2xl">{icon}</span>
      <div>
        <h4 className="font-medium text-gray-200">{title}</h4>
        <p className="text-sm text-gray-400">{desc}</p>
      </div>
    </div>
  )
}

function ArchLayer({ name, desc, color }: { name: string; desc: string; color: string }) {
  return (
    <div className={`bg-gradient-to-r ${color} rounded-lg p-4`}>
      <h4 className="font-semibold">{name}</h4>
      <p className="text-sm opacity-90">{desc}</p>
    </div>
  )
}

function Arrow() {
  return (
    <svg className="w-5 h-5 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor">
      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 14l-7 7m0 0l-7-7m7 7V3" />
    </svg>
  )
}

function TableCard({ name, columns }: { name: string; columns: string[] }) {
  return (
    <div className="bg-gray-950 rounded-lg p-4">
      <h4 className="font-mono text-sm text-red-400 mb-2">{name}</h4>
      <div className="flex flex-wrap gap-1">
        {columns.map(col => (
          <span key={col} className="text-xs px-1.5 py-0.5 bg-gray-800 rounded text-gray-400">{col}</span>
        ))}
      </div>
    </div>
  )
}

function FeatureGroup({ title, features }: { title: string; features: string[] }) {
  return (
    <div className="bg-gray-900 rounded-xl p-5 border border-gray-800">
      <h3 className="font-semibold mb-3">{title}</h3>
      <ul className="space-y-1.5">
        {features.map((f, i) => (
          <li key={i} className="text-sm text-gray-300 flex items-center gap-2">
            <span className="text-green-400 text-xs">●</span>
            {f}
          </li>
        ))}
      </ul>
    </div>
  )
}

function CodeBlock({ title, code }: { title: string; code: string }) {
  return (
    <div>
      <span className="text-xs text-gray-500">{title}</span>
      <div className="bg-gray-950 rounded-lg px-4 py-2 font-mono text-sm text-green-400 mt-1">
        $ {code}
      </div>
    </div>
  )
}

function DependencyItem({ name, version }: { name: string; version: string }) {
  return (
    <div className="flex justify-between items-center py-1">
      <span className="text-gray-300">{name}</span>
      <span className="text-gray-500 font-mono text-xs">{version}</span>
    </div>
  )
}

function StatItem({ label, value }: { label: string; value: string }) {
  return (
    <div className="text-center">
      <div className="text-2xl font-bold text-red-400">{value}</div>
      <div className="text-xs text-gray-500">{label}</div>
    </div>
  )
}

export default App
