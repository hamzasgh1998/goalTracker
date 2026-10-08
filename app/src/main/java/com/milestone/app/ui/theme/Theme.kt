package com.milestone.app.ui.theme

import android.app.Activity
import android.provider.Settings
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * True when animations should be suppressed.
 *
 * Seeded from the platform's own animation scale (which is what the accessibility
 * "Remove animations" toggle actually sets) and overridable in app settings.
 */
val LocalReduceMotion: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { false }

private val LocalMilestoneColors = compositionLocalOf { lightMilestoneColors(Accent.Default) }

/**
 * Entry point for the design system.
 *
 * @param themeMode light/dark/system, from user preferences.
 * @param accent the user's chosen accent colour.
 * @param reduceMotionOverride when non-null, overrides the platform's animation
 *   setting. `null` means "follow the system".
 */
@Composable
fun MilestoneTheme(
    themeMode: ThemeMode = ThemeMode.Default,
    accent: Accent = Accent.Default,
    reduceMotionOverride: Boolean? = null,
    content: @Composable () -> Unit,
) {
    val systemDark = isSystemInDarkTheme()
    val isDark = when (themeMode) {
        ThemeMode.System -> systemDark
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }

    val colors = remember(isDark, accent) {
        if (isDark) darkMilestoneColors(accent) else lightMilestoneColors(accent)
    }

    val context = LocalContext.current
    val systemReduceMotion = remember(context) {
        // 0f means the user has turned animations off platform-wide.
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f,
        ) == 0f
    }
    val reduceMotion = reduceMotionOverride ?: systemReduceMotion

    // Keep the status/navigation bar icons legible against our own background.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window ?: return@SideEffect
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !isDark
                isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    CompositionLocalProvider(
        LocalMilestoneColors provides colors,
        LocalReduceMotion provides reduceMotion,
    ) {
        MaterialTheme(
            colorScheme = colors.toMaterialScheme(),
            typography = MilestoneTypography,
            shapes = MilestoneMaterialShapes,
            content = content,
        )
    }
}

/**
 * Accessor for the design system's own tokens.
 *
 * `MilestoneTheme.colors.ink` reads like `MaterialTheme.colorScheme.onSurface` but
 * resolves to this product's palette.
 */
object MilestoneTheme {
    val colors: MilestoneColors
        @Composable
        @ReadOnlyComposable
        get() = LocalMilestoneColors.current

    val type: Typography
        @Composable
        @ReadOnlyComposable
        get() = MaterialTheme.typography

    /** Styles with no Material equivalent — eyebrow labels, metrics, quotes. */
    val extraType: MilestoneTextStyles
        @Composable
        @ReadOnlyComposable
        get() = MilestoneTextStyles

    val shapes: MilestoneShapes
        @Composable
        @ReadOnlyComposable
        get() = MilestoneShapes

    val spacing: Spacing
        @Composable
        @ReadOnlyComposable
        get() = Spacing
}

/**
 * Projects our palette onto a Material scheme.
 *
 * This exists so that stock M3 components (TextField, Switch, Snackbar, ripples)
 * pick up the right colours without each one being restyled by hand. App-authored
 * UI should read [MilestoneTheme.colors] directly instead.
 */
private fun MilestoneColors.toMaterialScheme() = if (isDark) {
    darkColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentWash,
        onPrimaryContainer = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = surfaceMuted,
        onSecondaryContainer = ink,
        tertiary = success,
        onTertiary = background,
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = inkMuted,
        surfaceContainer = surfaceMuted,
        surfaceContainerHigh = surfaceSunken,
        surfaceContainerHighest = surfaceSunken,
        surfaceContainerLow = surface,
        surfaceContainerLowest = background,
        outline = inkFaint,
        outlineVariant = hairline,
        error = stalled,
        onError = background,
        errorContainer = stalled.copy(alpha = 0.18f),
        onErrorContainer = stalled,
        scrim = Color.Black.copy(alpha = 0.6f),
    )
} else {
    lightColorScheme(
        primary = accent,
        onPrimary = onAccent,
        primaryContainer = accentWash,
        onPrimaryContainer = accent,
        secondary = accent,
        onSecondary = onAccent,
        secondaryContainer = surfaceMuted,
        onSecondaryContainer = ink,
        tertiary = success,
        onTertiary = Color.White,
        background = background,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = surfaceMuted,
        onSurfaceVariant = inkMuted,
        surfaceContainer = surfaceMuted,
        surfaceContainerHigh = surfaceSunken,
        surfaceContainerHighest = surfaceSunken,
        surfaceContainerLow = surface,
        surfaceContainerLowest = Color.White,
        outline = inkFaint,
        outlineVariant = hairline,
        error = stalled,
        onError = Color.White,
        errorContainer = stalled.copy(alpha = 0.12f),
        onErrorContainer = stalled,
        scrim = Color.Black.copy(alpha = 0.4f),
    )
}
