package com.example.pomodoro.ui.screens.onboarding

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.pomodoro.PomodoroApplication
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val app = PomodoroApplication.instance
    val scope = rememberCoroutineScope()
    var currentPage by remember { mutableIntStateOf(0) }

    val pages = listOf(
        OnboardingPage(
            icon = Icons.Filled.Timer,
            title = "Welcome to PomodoroFocus",
            description = "A simple, effective way to boost your productivity using the Pomodoro Technique.",
            color = MaterialTheme.colorScheme.primary
        ),
        OnboardingPage(
            icon = Icons.Filled.Work,
            title = "Focus Sessions",
            description = "Work in focused 25-minute intervals. After each session, take a short 5-minute break. After 4 sessions, enjoy a longer 15-minute break.",
            color = MaterialTheme.colorScheme.secondary
        ),
        OnboardingPage(
            icon = Icons.Filled.Checklist,
            title = "Track Your Tasks",
            description = "Create tasks and projects to organize your work. Estimate how many Pomodoros each task needs and track your progress.",
            color = MaterialTheme.colorScheme.tertiary
        ),
        OnboardingPage(
            icon = Icons.Filled.BarChart,
            title = "See Your Progress",
            description = "View statistics, build streaks, and gain insights into your productivity patterns. All data stays on your device.",
            color = MaterialTheme.colorScheme.primary
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(modifier = Modifier.weight(1f))

        // Icon
        Surface(
            modifier = Modifier.size(120.dp),
            shape = MaterialTheme.shapes.extraLarge,
            color = pages[currentPage].color.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    pages[currentPage].icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = pages[currentPage].color
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Title
        Text(
            text = pages[currentPage].title,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Description
        Text(
            text = pages[currentPage].description,
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.weight(1f))

        // Page indicators
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 32.dp)
        ) {
            pages.forEachIndexed { index, _ ->
                Surface(
                    modifier = Modifier.size(if (index == currentPage) 12.dp else 8.dp),
                    shape = MaterialTheme.shapes.small,
                    color = if (index == currentPage) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.surfaceVariant
                ) {}
            }
        }

        // Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onComplete) {
                Text("Skip")
            }

            if (currentPage < pages.size - 1) {
                Button(onClick = { currentPage++ }) {
                    Text("Next")
                }
            } else {
                Button(onClick = {
                    scope.launch {
                        app.appPreferences.setOnboardingComplete()
                        onComplete()
                    }
                }) {
                    Text("Get Started")
                }
            }
        }
    }
}

private data class OnboardingPage(
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val title: String,
    val description: String,
    val color: androidx.compose.ui.graphics.Color
)
