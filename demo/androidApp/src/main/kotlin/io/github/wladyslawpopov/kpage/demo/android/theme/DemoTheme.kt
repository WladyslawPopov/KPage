package io.github.wladyslawpopov.kpage.demo.android.theme

import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import io.github.wladyslawpopov.kpage.demo.domain.settings.ThemeMode

// The accent of the app instead of the purple default of Material 3.
private val AccentLight = Color(0xFF007AFF)
private val AccentDark = Color(0xFF0A84FF)
private const val ACCENT_CONTAINER_ALPHA = 0.15f

@Composable
fun DemoTheme(mode: ThemeMode, content: @Composable () -> Unit) {
    val isDark = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    // The system bars are drawn by the system: their icons must contrast with the theme of the app,
    // which may differ from the theme of the system.
    val window = LocalActivity.current?.window
    val view = LocalView.current
    SideEffect {
        if (window != null) {
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (isDark) {
            darkColorScheme(
                primary = AccentDark,
                onPrimary = Color.White,
                primaryContainer = AccentDark.copy(alpha = ACCENT_CONTAINER_ALPHA),
                onPrimaryContainer = AccentDark,
            )
        } else {
            lightColorScheme(
                primary = AccentLight,
                onPrimary = Color.White,
                primaryContainer = AccentLight.copy(alpha = ACCENT_CONTAINER_ALPHA),
                onPrimaryContainer = AccentLight,
            )
        },
        content = content,
    )
}
