package com.milestone.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.sp

/**
 * Editorial type pairing, built entirely from platform families so the app ships
 * no font binaries and needs no network at runtime.
 *
 * Serif (Noto Serif on Android) carries the display sizes — season names, goal
 * titles, big numbers. It is what stops the app reading as a spreadsheet.
 * Sans (Roboto) carries everything functional: body copy, labels, metadata.
 *
 * Sizes are in sp throughout, so the whole scale grows with the system font
 * setting. Nothing here is hard-coded in dp.
 */
private val Display = FontFamily.Serif
private val Text = FontFamily.SansSerif

/** Trims the extra leading the platform adds above/below big serif text. */
private val TightLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None,
)

val MilestoneTypography = Typography(
    // Reserved for the one biggest thing on a screen: a season name, a finished goal.
    displayLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 44.sp,
        lineHeight = 48.sp,
        letterSpacing = (-1.0).sp,
        lineHeightStyle = TightLineHeight,
    ),
    displayMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.6).sp,
        lineHeightStyle = TightLineHeight,
    ),
    displaySmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.4).sp,
        lineHeightStyle = TightLineHeight,
    ),

    // Screen titles and goal names in lists.
    headlineLarge = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.3).sp,
        lineHeightStyle = TightLineHeight,
    ),
    headlineMedium = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.2).sp,
        lineHeightStyle = TightLineHeight,
    ),
    headlineSmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        lineHeightStyle = TightLineHeight,
    ),

    titleLarge = TextStyle(
        fontFamily = Text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 23.sp,
        letterSpacing = (-0.1).sp,
    ),
    titleMedium = TextStyle(
        fontFamily = Text,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 21.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = Text,
        fontWeight = FontWeight.Medium,
        fontSize = 13.sp,
        lineHeight = 18.sp,
    ),

    bodyLarge = TextStyle(
        fontFamily = Text,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 25.sp,
        letterSpacing = 0.1.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = Text,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 21.sp,
        letterSpacing = 0.1.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = Text,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 17.sp,
    ),

    labelLarge = TextStyle(
        fontFamily = Text,
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = 0.1.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = Text,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.2.sp,
    ),
    labelSmall = TextStyle(
        fontFamily = Text,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 0.3.sp,
    ),
)

/**
 * Styles the Material scale has no slot for.
 *
 * Use these through `MilestoneTheme.type` so they stay discoverable next to the
 * Material ones.
 */
object MilestoneTextStyles {
    /** Small all-caps section marker: "CURRENT SEASON", "TODAY". Wide tracking. */
    val Eyebrow = TextStyle(
        fontFamily = Text,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.6.sp,
    )

    /** Percentages and counts rendered large next to a progress visual. */
    val Metric = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 32.sp,
        letterSpacing = (-0.8).sp,
        lineHeightStyle = TightLineHeight,
    )

    /** The compact percentage inside a progress ring. */
    val MetricSmall = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Medium,
        fontSize = 18.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.4).sp,
        lineHeightStyle = TightLineHeight,
    )

    /** The user's own words, quoted back to them — a goal's "why", a reflection. */
    val Quote = TextStyle(
        fontFamily = Display,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 27.sp,
        letterSpacing = 0.sp,
    )
}
