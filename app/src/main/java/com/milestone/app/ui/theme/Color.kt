package com.milestone.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

/**
 * The app's colour contract.
 *
 * Deliberately *not* Material's `ColorScheme`: the roles here are named for what
 * they mean in this product (ink, hairline, stalled) rather than for elevation
 * levels, which keeps call sites honest about intent. A Material scheme is still
 * derived from this in [MilestoneTheme] so that M3 components inherit the palette.
 */
@Immutable
data class MilestoneColors(
    /** Page backdrop. Warm paper in light, near-black in dark. */
    val background: Color,
    /** Cards and sheets that sit on [background]. */
    val surface: Color,
    /** A quieter fill for inset rows, chips and track backgrounds. */
    val surfaceMuted: Color,
    /** Recessed wells — progress tracks, unfilled rings. */
    val surfaceSunken: Color,
    /** 1dp dividers and card outlines. Never used for text. */
    val hairline: Color,
    /** Primary text. */
    val ink: Color,
    /** Secondary text: supporting copy, metadata labels. */
    val inkMuted: Color,
    /** Tertiary text: dates, counts, eyebrow labels. Still AA at 12sp+. */
    val inkFaint: Color,
    /** The user's chosen accent. Means "active / current / you are here". */
    val accent: Color,
    /** A wash of [accent] for fills behind accent-coloured content. */
    val accentWash: Color,
    /** Text/icon colour that is legible *on top of* [accent]. */
    val onAccent: Color,
    /** Completed. */
    val success: Color,
    /** Needs attention / at risk. */
    val attention: Color,
    /** Stalled. */
    val stalled: Color,
    /** Intentionally paused — absence of signal, not a warning. */
    val paused: Color,
    val isDark: Boolean,
)

/**
 * Raw palette values. Light is a warm, slightly off-white paper stock; dark is a
 * cool near-black. Neither uses pure #FFF or #000 — pure white glares and pure
 * black crushes the shadows that give cards their edge on OLED.
 */
internal object PaletteTokens {
    // Light
    val PaperBackground = Color(0xFFF7F5F1)
    val PaperSurface = Color(0xFFFFFFFF)
    val PaperMuted = Color(0xFFEFEBE4)
    val PaperSunken = Color(0xFFE6E1D8)
    val PaperHairline = Color(0xFFE3DED5)
    val PaperInk = Color(0xFF15130F)
    val PaperInkMuted = Color(0xFF635C51)
    val PaperInkFaint = Color(0xFF8D867B)

    // Dark
    val NightBackground = Color(0xFF0C0D10)
    val NightSurface = Color(0xFF15171C)
    val NightMuted = Color(0xFF1D2027)
    val NightSunken = Color(0xFF24272F)
    val NightHairline = Color(0xFF272B33)
    val NightInk = Color(0xFFF2F0EC)
    val NightInkMuted = Color(0xFFA2A6AE)
    val NightInkFaint = Color(0xFF71767F)

    // Status — desaturated so that four of them can coexist without shouting.
    val SuccessLight = Color(0xFF2F7D5F)
    val SuccessDark = Color(0xFF5CC79A)
    val AttentionLight = Color(0xFF9E6F16)
    val AttentionDark = Color(0xFFE0AE55)
    val StalledLight = Color(0xFFAE4C3C)
    val StalledDark = Color(0xFFE0836F)
}

/** Builds the light colour set for a chosen [accent]. */
fun lightMilestoneColors(accent: Accent): MilestoneColors = MilestoneColors(
    background = PaletteTokens.PaperBackground,
    surface = PaletteTokens.PaperSurface,
    surfaceMuted = PaletteTokens.PaperMuted,
    surfaceSunken = PaletteTokens.PaperSunken,
    hairline = PaletteTokens.PaperHairline,
    ink = PaletteTokens.PaperInk,
    inkMuted = PaletteTokens.PaperInkMuted,
    inkFaint = PaletteTokens.PaperInkFaint,
    accent = accent.light,
    accentWash = accent.light.copy(alpha = 0.10f),
    onAccent = Color.White,
    success = PaletteTokens.SuccessLight,
    attention = PaletteTokens.AttentionLight,
    stalled = PaletteTokens.StalledLight,
    paused = PaletteTokens.PaperInkFaint,
    isDark = false,
)

/** Builds the dark colour set for a chosen [accent]. */
fun darkMilestoneColors(accent: Accent): MilestoneColors = MilestoneColors(
    background = PaletteTokens.NightBackground,
    surface = PaletteTokens.NightSurface,
    surfaceMuted = PaletteTokens.NightMuted,
    surfaceSunken = PaletteTokens.NightSunken,
    hairline = PaletteTokens.NightHairline,
    ink = PaletteTokens.NightInk,
    inkMuted = PaletteTokens.NightInkMuted,
    inkFaint = PaletteTokens.NightInkFaint,
    accent = accent.dark,
    accentWash = accent.dark.copy(alpha = 0.14f),
    // Dark accents are light enough that dark text reads better on them.
    onAccent = PaletteTokens.NightBackground,
    success = PaletteTokens.SuccessDark,
    attention = PaletteTokens.AttentionDark,
    stalled = PaletteTokens.StalledDark,
    paused = PaletteTokens.NightInkFaint,
    isDark = true,
)
