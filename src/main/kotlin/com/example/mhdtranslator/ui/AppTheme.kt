package com.example.mhdtranslator.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

enum class ThemeMode { SYSTEM, LIGHT, DARK }

// ── Light scheme ─────────────────────────────────────────────────────────────
// Clean white surfaces, WBN blue as primary.
private val LightColors = lightColorScheme(
    primary              = Color(0xFF1565C0), // blue 800
    onPrimary            = Color(0xFFFFFFFF),
    primaryContainer     = Color(0xFFD4E3FF),
    onPrimaryContainer   = Color(0xFF001B3D),
    secondary            = Color(0xFF1976D2),
    onSecondary          = Color(0xFFFFFFFF),
    secondaryContainer   = Color(0xFFDCEDFF),
    onSecondaryContainer = Color(0xFF00213B),
    background           = Color(0xFFFAFAFA),
    onBackground         = Color(0xFF1C1B1F),
    surface              = Color(0xFFFFFFFF),
    onSurface            = Color(0xFF1C1B1F),
    surfaceVariant       = Color(0xFFF0F4F8),
    onSurfaceVariant     = Color(0xFF44474E),
    outline              = Color(0xFF74777F),
    outlineVariant       = Color(0xFFCDCFD6),
)

// ── Dark scheme ───────────────────────────────────────────────────────────────
// True-dark background, light-blue primary so contrast is always sufficient.
private val DarkColors = darkColorScheme(
    primary              = Color(0xFF9ECAFF), // light blue, readable on dark
    onPrimary            = Color(0xFF003064),
    primaryContainer     = Color(0xFF004494),
    onPrimaryContainer   = Color(0xFFD1E4FF),
    secondary            = Color(0xFF90CAF9),
    onSecondary          = Color(0xFF003355),
    secondaryContainer   = Color(0xFF004A77),
    onSecondaryContainer = Color(0xFFCDE5FF),
    background           = Color(0xFF111318),
    onBackground         = Color(0xFFE2E2E9),
    surface              = Color(0xFF111318),
    onSurface            = Color(0xFFE2E2E9),
    surfaceVariant       = Color(0xFF1E2329),
    onSurfaceVariant     = Color(0xFFC4C7CF),
    outline              = Color(0xFF8E9099),
    outlineVariant       = Color(0xFF44474E),
)

@Composable
fun AppTheme(themeMode: ThemeMode, content: @Composable () -> Unit) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content     = content,
    )
}
