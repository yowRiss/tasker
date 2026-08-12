package com.tasker.android.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// ─────────────────────────────────────────────────────────────────
//  Extended colors not covered by Material 3 ColorScheme
// ─────────────────────────────────────────────────────────────────
data class TaskerExtendedColors(
    val background: Color,
    val surface: Color,
    val surfaceAlt: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val accent: Color,
    val accentSubtle: Color,
    val destructive: Color,
    val destructiveSubtle: Color,
    val warningSubtle: Color,
    val successSubtle: Color,
    val warning: Color,
    val success: Color,
    val isLight: Boolean,
)

val LocalTaskerColors = compositionLocalOf<TaskerExtendedColors> {
    error("No TaskerColors provided")
}

private val LightExtendedColors = TaskerExtendedColors(
    background        = Background_Light,
    surface           = Surface_Light,
    surfaceAlt        = SurfaceAlt_Light,
    border            = Border_Light,
    textPrimary       = TextPrimary_Light,
    textSecondary     = TextSecondary_Light,
    textTertiary      = TextTertiary_Light,
    accent            = Accent_Light,
    accentSubtle      = AccentSubtle_Light,
    destructive       = Destructive_Light,
    destructiveSubtle = DestructiveSubtle_Light,
    warningSubtle     = WarningSubtle_Light,
    successSubtle     = SuccessSubtle_Light,
    warning           = Warning_Light,
    success           = Success_Light,
    isLight           = true,
)

private val DarkExtendedColors = TaskerExtendedColors(
    background        = Background_Dark,
    surface           = Surface_Dark,
    surfaceAlt        = SurfaceAlt_Dark,
    border            = Border_Dark,
    textPrimary       = TextPrimary_Dark,
    textSecondary     = TextSecondary_Dark,
    textTertiary      = TextTertiary_Dark,
    accent            = Accent_Dark,
    accentSubtle      = AccentSubtle_Dark,
    destructive       = Destructive_Dark,
    destructiveSubtle = DestructiveSubtle_Dark,
    warningSubtle     = WarningSubtle_Dark,
    successSubtle     = SuccessSubtle_Dark,
    warning           = Warning_Dark,
    success           = Success_Dark,
    isLight           = false,
)

// ─────────────────────────────────────────────────────────────────
//  Material 3 ColorSchemes — mapped from design.md tokens
// ─────────────────────────────────────────────────────────────────
private val LightColorScheme = lightColorScheme(
    primary            = Accent_Light,
    onPrimary          = Color.White,
    primaryContainer   = AccentSubtle_Light,
    onPrimaryContainer = AccentHover_Light,

    secondary          = TextSecondary_Light,
    onSecondary        = Color.White,
    secondaryContainer = SurfaceAlt_Light,
    onSecondaryContainer = TextPrimary_Light,

    tertiary           = Success_Light,
    onTertiary         = Color.White,

    background         = Background_Light,
    onBackground       = TextPrimary_Light,

    surface            = Surface_Light,
    onSurface          = TextPrimary_Light,
    surfaceVariant     = SurfaceAlt_Light,
    onSurfaceVariant   = TextSecondary_Light,

    outline            = Border_Light,
    outlineVariant     = Border_Light.copy(alpha = 0.5f),

    error              = Destructive_Light,
    onError            = Color.White,
    errorContainer     = DestructiveSubtle_Light,
    onErrorContainer   = Destructive_Light,

    scrim              = TextPrimary_Light.copy(alpha = 0.4f),
)

private val DarkColorScheme = darkColorScheme(
    primary            = Accent_Dark,
    onPrimary          = Color.White,
    primaryContainer   = AccentSubtle_Dark,
    onPrimaryContainer = Accent_Dark,

    secondary          = TextSecondary_Dark,
    onSecondary        = Background_Dark,
    secondaryContainer = SurfaceAlt_Dark,
    onSecondaryContainer = TextPrimary_Dark,

    tertiary           = Success_Dark,
    onTertiary         = Background_Dark,

    background         = Background_Dark,
    onBackground       = TextPrimary_Dark,

    surface            = Surface_Dark,
    onSurface          = TextPrimary_Dark,
    surfaceVariant     = SurfaceAlt_Dark,
    onSurfaceVariant   = TextSecondary_Dark,

    outline            = Border_Dark,
    outlineVariant     = Border_Dark.copy(alpha = 0.5f),

    error              = Destructive_Dark,
    onError            = Background_Dark,
    errorContainer     = DestructiveSubtle_Dark,
    onErrorContainer   = Destructive_Dark,

    scrim              = Color.Black.copy(alpha = 0.5f),
)

// ─────────────────────────────────────────────────────────────────
//  TaskerTheme — entry point for all Compose UI
// ─────────────────────────────────────────────────────────────────
@Composable
fun TaskerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme: ColorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val extendedColors = if (darkTheme) DarkExtendedColors else LightExtendedColors

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalTaskerColors provides extendedColors) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography  = TaskerTypography,
            shapes      = TaskerShapes,
            content     = content,
        )
    }
}

/**
 * Convenience accessor for Tasker-specific design tokens within Composables.
 * Usage: val colors = TaskerTheme.colors
 */
object TaskerTheme {
    val colors: TaskerExtendedColors
        @Composable get() = LocalTaskerColors.current
}
