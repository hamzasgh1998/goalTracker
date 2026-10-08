package com.milestone.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * User-selectable accent colours.
 *
 * Each carries a separate light and dark value rather than one hue reused on both
 * backgrounds: a mid-tone that reads well on paper goes muddy on near-black, and a
 * tone bright enough for dark mode glows on white.
 *
 * Stored by [name] in preferences, so renaming an entry is a migration.
 */
enum class Accent(
    val label: String,
    val light: Color,
    val dark: Color,
) {
    Indigo("Indigo", light = Color(0xFF3A5BC7), dark = Color(0xFF8BA6F2)),
    Violet("Violet", light = Color(0xFF6B4BC0), dark = Color(0xFFA78DF0)),
    Pine("Pine", light = Color(0xFF2B7A62), dark = Color(0xFF63C7A6)),
    Amber("Amber", light = Color(0xFFB2662C), dark = Color(0xFFE5A169)),
    Rose("Rose", light = Color(0xFFB4487A), dark = Color(0xFFEC8BB2));

    companion object {
        val Default = Indigo

        fun fromName(value: String?): Accent =
            entries.firstOrNull { it.name == value } ?: Default
    }
}

/** How the app resolves light vs dark. */
enum class ThemeMode(val label: String) {
    System("Follow system"),
    Light("Light"),
    Dark("Dark");

    companion object {
        val Default = System

        fun fromName(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: Default
    }
}
