package com.example.pomodoro.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Pomodoro-inspired color palette
val PomodoroRed = Color(0xFFE53935)
val PomodoroRedDark = Color(0xFFC62828)
val PomodoroGreen = Color(0xFF43A047)
val PomodoroGreenDark = Color(0xFF2E7D32)
val PomodoroBlue = Color(0xFF1E88E5)
val PomodoroBlueDark = Color(0xFF1565C0)
val PomodoroOrange = Color(0xFFFB8C00)
val PomodoroOrangeDark = Color(0xFFEF6C00)
val PomodoroPurple = Color(0xFF6750A4)

// Light theme colors
private val LightColorScheme = lightColorScheme(
    primary = PomodoroRed,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = PomodoroGreen,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFC8FFC3),
    onSecondaryContainer = Color(0xFF002204),
    tertiary = PomodoroBlue,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD1E4FF),
    onTertiaryContainer = Color(0xFF001D36),
    background = Color(0xFFFFFBFF),
    onBackground = Color(0xFF201A1A),
    surface = Color(0xFFFFFBFF),
    onSurface = Color(0xFF201A1A),
    surfaceVariant = Color(0xFFF5DDDA),
    onSurfaceVariant = Color(0xFF534341),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    outline = Color(0xFF857371)
)

// Dark theme colors
private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB4AB),
    onPrimary = Color(0xFF690005),
    primaryContainer = PomodoroRedDark,
    onPrimaryContainer = Color(0xFFFFDAD6),
    secondary = Color(0xFFA8F0A0),
    onSecondary = Color(0xFF00390B),
    secondaryContainer = Color(0xFF005314),
    onSecondaryContainer = Color(0xFFC8FFC3),
    tertiary = Color(0xFF9ECAFF),
    onTertiary = Color(0xFF003258),
    tertiaryContainer = PomodoroBlueDark,
    onTertiaryContainer = Color(0xFFD1E4FF),
    background = Color(0xFF201A1A),
    onBackground = Color(0xFFECE0DF),
    surface = Color(0xFF201A1A),
    onSurface = Color(0xFFECE0DF),
    surfaceVariant = Color(0xFF534341),
    onSurfaceVariant = Color(0xFFD8C2BF),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF93000A),
    outline = Color(0xFFA08C8A)
)

@Composable
fun PomodoroTheme(
    themeMode: String = "SYSTEM",
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        "LIGHT" -> false
        "DARK" -> true
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
