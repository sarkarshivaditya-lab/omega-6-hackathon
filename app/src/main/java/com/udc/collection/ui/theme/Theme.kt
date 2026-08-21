package com.udc.collection.ui.theme

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

// OMEGA 6.0 Brand Colors
val OmegaBlue = Color(0xFF1565C0)
val OmegaBlueDark = Color(0xFF0D47A1)
val OmegaBlueLight = Color(0xFF1E88E5)
val OmegaSurface = Color(0xFFF8FAFE)
val OmegaCard = Color(0xFFFFFFFF)
val OmegaError = Color(0xFFB71C1C)

private val LightColorScheme = lightColorScheme(
    primary = OmegaBlue,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E4FF),
    onPrimaryContainer = Color(0xFF001C3D),
    secondary = Color(0xFF1976D2),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDDE3FF),
    onSecondaryContainer = Color(0xFF001257),
    tertiary = Color(0xFF006687),
    onTertiary = Color.White,
    background = OmegaSurface,
    onBackground = Color(0xFF1A1C1E),
    surface = OmegaCard,
    onSurface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFFE1E9F8),
    onSurfaceVariant = Color(0xFF44474F),
    outline = Color(0xFF74777F),
    error = OmegaError,
    onError = Color.White
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFADC6FF),
    onPrimary = Color(0xFF002E69),
    primaryContainer = Color(0xFF004494),
    onPrimaryContainer = Color(0xFFD6E4FF),
    secondary = Color(0xFFB9C4FF),
    onSecondary = Color(0xFF001E8C),
    background = Color(0xFF1A1C1E),
    onBackground = Color(0xFFE2E2E6),
    surface = Color(0xFF1A1C1E),
    onSurface = Color(0xFFE2E2E6),
    surfaceVariant = Color(0xFF44474F),
    onSurfaceVariant = Color(0xFFC4C6D0),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005)
)

@Composable
fun OmegaTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.primary.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = UDCTypography,
        shapes = UDCShapes,
        content = content
    )
}

@Deprecated("Use OmegaTheme")
@Composable
fun UDCTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) = OmegaTheme(darkTheme, content)
