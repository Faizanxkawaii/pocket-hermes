package com.pockethermes.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkPurple = Color(0xFF1A0A2E)
private val MediumPurple = Color(0xFF3D1F6D)
private val LightPurple = Color(0xFF7B2FBE)
private val AccentPurple = Color(0xFFBB86FC)
private val SurfaceDark = Color(0xFF121212)
private val OnSurfaceDark = Color(0xFFE0E0E0)

private val DarkColorScheme = darkColorScheme(
    primary = AccentPurple,
    secondary = LightPurple,
    tertiary = MediumPurple,
    background = DarkPurple,
    surface = SurfaceDark,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = OnSurfaceDark,
    onSurface = OnSurfaceDark
)

private val LightColorScheme = lightColorScheme(
    primary = MediumPurple,
    secondary = LightPurple,
    tertiary = AccentPurple,
    background = Color(0xFFF5F0FF),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = DarkPurple,
    onSurface = DarkPurple
)

@Composable
fun PocketHermesTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content
    )
}
