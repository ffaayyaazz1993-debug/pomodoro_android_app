package com.example.pomodoro.ui.screens.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.pomodoro.PomodoroApplication
import com.example.pomodoro.data.preferences.TimerSettings
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen() {
    val app = PomodoroApplication.instance
    val scope = rememberCoroutineScope()
    val settings by app.appPreferences.timerSettings.collectAsState(initial = TimerSettings())

    var showTimerDialog by remember { mutableStateOf(false) }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Timer section
        item {
            Text(
                "Timer",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SettingsItem(
                icon = Icons.Outlined.Timer,
                title = "Timer Durations",
                subtitle = "Focus: ${settings.focusDurationMin}m, Short: ${settings.shortBreakMin}m, Long: ${settings.longBreakMin}m",
                onClick = { showTimerDialog = true }
            )
        }

        item {
            SettingsSwitch(
                icon = Icons.Outlined.PlayArrow,
                title = "Auto-start Focus",
                subtitle = "Automatically start next focus session",
                checked = settings.autoStartFocus,
                onCheckedChange = { new ->
                    scope.launch {
                        app.appPreferences.updateTimerSettings(settings.copy(autoStartFocus = new))
                    }
                }
            )
        }

        item {
            SettingsSwitch(
                icon = Icons.Outlined.Coffee,
                title = "Auto-start Short Break",
                checked = settings.autoStartShortBreak,
                onCheckedChange = { new ->
                    scope.launch {
                        app.appPreferences.updateTimerSettings(settings.copy(autoStartShortBreak = new))
                    }
                }
            )
        }

        item {
            SettingsSwitch(
                icon = Icons.Outlined.BeachAccess,
                title = "Auto-start Long Break",
                checked = settings.autoStartLongBreak,
                onCheckedChange = { new ->
                    scope.launch {
                        app.appPreferences.updateTimerSettings(settings.copy(autoStartLongBreak = new))
                    }
                }
            )
        }

        item {
            SettingsSwitch(
                icon = Icons.Outlined.StayCurrentPortrait,
                title = "Keep Screen On",
                subtitle = "Prevent screen from turning off during focus",
                checked = settings.keepScreenOn,
                onCheckedChange = { new ->
                    scope.launch {
                        app.appPreferences.updateTimerSettings(settings.copy(keepScreenOn = new))
                    }
                }
            )
        }

        // Notifications section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Notifications & Sound",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SettingsSwitch(
                icon = Icons.Outlined.Notifications,
                title = "Sound",
                subtitle = "Play sound when session completes",
                checked = settings.soundEnabled,
                onCheckedChange = { new ->
                    scope.launch {
                        app.appPreferences.updateTimerSettings(settings.copy(soundEnabled = new))
                    }
                }
            )
        }

        item {
            SettingsSwitch(
                icon = Icons.Outlined.Vibration,
                title = "Vibration",
                subtitle = "Vibrate when session completes",
                checked = settings.vibrationEnabled,
                onCheckedChange = { new ->
                    scope.launch {
                        app.appPreferences.updateTimerSettings(settings.copy(vibrationEnabled = new))
                    }
                }
            )
        }

        // Appearance section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Appearance",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SettingsItem(
                icon = Icons.Outlined.Palette,
                title = "Theme",
                subtitle = when (settings.themeMode) {
                    "LIGHT" -> "Light"
                    "DARK" -> "Dark"
                    else -> "System default"
                },
                onClick = { /* Show theme picker */ }
            )
        }

        // Data section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Data",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SettingsItem(
                icon = Icons.Outlined.Upload,
                title = "Export Data",
                subtitle = "Export tasks, projects, and sessions as JSON",
                onClick = { /* Export */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Outlined.Download,
                title = "Import Data",
                subtitle = "Import from JSON backup",
                onClick = { /* Import */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Outlined.Backup,
                title = "Create Backup",
                subtitle = "Save a complete backup of all data",
                onClick = { /* Backup */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Outlined.Restore,
                title = "Restore Backup",
                subtitle = "Restore from a previous backup",
                onClick = { /* Restore */ }
            )
        }

        // About section
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "About",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 8.dp)
            )
        }

        item {
            SettingsItem(
                icon = Icons.Outlined.Info,
                title = "PomodoroFocus",
                subtitle = "Version 1.0.0 • Offline-first productivity",
                onClick = { /* Show about */ }
            )
        }

        item {
            SettingsItem(
                icon = Icons.Outlined.Policy,
                title = "Privacy",
                subtitle = "All data stays on your device. No tracking.",
                onClick = { /* Show privacy */ }
            )
        }
    }

    // Timer settings dialog
    if (showTimerDialog) {
        TimerSettingsDialog(
            currentSettings = settings,
            onDismiss = { showTimerDialog = false },
            onSave = { newSettings ->
                scope.launch {
                    app.appPreferences.updateTimerSettings(newSettings)
                    showTimerDialog = false
                }
            }
        )
    }
}

@Composable
private fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun SettingsSwitch(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String? = null,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.bodyLarge)
                if (subtitle != null) {
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun TimerSettingsDialog(
    currentSettings: TimerSettings,
    onDismiss: () -> Unit,
    onSave: (TimerSettings) -> Unit
) {
    var focusMin by remember { mutableIntStateOf(currentSettings.focusDurationMin) }
    var shortBreakMin by remember { mutableIntStateOf(currentSettings.shortBreakMin) }
    var longBreakMin by remember { mutableIntStateOf(currentSettings.longBreakMin) }
    var longBreakInterval by remember { mutableIntStateOf(currentSettings.longBreakInterval) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Timer Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                NumberStepper("Focus Duration", focusMin, 1, 90) { focusMin = it }
                NumberStepper("Short Break", shortBreakMin, 1, 30) { shortBreakMin = it }
                NumberStepper("Long Break", longBreakMin, 1, 60) { longBreakMin = it }
                NumberStepper("Long Break Every", longBreakInterval, 2, 10) { longBreakInterval = it }
            }
        },
        confirmButton = {
            Button(onClick = {
                onSave(currentSettings.copy(
                    focusDurationMin = focusMin,
                    shortBreakMin = shortBreakMin,
                    longBreakMin = longBreakMin,
                    longBreakInterval = longBreakInterval
                ))
            }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
private fun NumberStepper(
    label: String,
    value: Int,
    min: Int,
    max: Int,
    onValueChange: (Int) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = { if (value > min) onValueChange(value - 1) },
                enabled = value > min
            ) {
                Icon(Icons.Filled.Remove, contentDescription = "Decrease")
            }
            Text(
                "$value min",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(60.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            IconButton(
                onClick = { if (value < max) onValueChange(value + 1) },
                enabled = value < max
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Increase")
            }
        }
    }
}
