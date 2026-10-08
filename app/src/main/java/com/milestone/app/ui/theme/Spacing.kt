package com.milestone.app.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * The spacing scale. Every gap in the app comes from here, which is most of what
 * makes unrelated screens look like one product.
 *
 * The large steps exist to be used: whitespace is the main tool for hierarchy in
 * this design, so section breaks are [xxl] / [section], not [md].
 */
@Immutable
object Spacing {
    /** Hairline nudges, icon-to-label. */
    val xxs: Dp = 2.dp
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    /** Default gap between related rows. */
    val lg: Dp = 16.dp
    val xl: Dp = 20.dp
    /** Default gap between cards. */
    val xxl: Dp = 24.dp
    /** Between a heading and its content. */
    val heading: Dp = 32.dp
    /** Between major sections of a screen. */
    val section: Dp = 44.dp
    /** Above a screen's first element, under the status bar. */
    val screenTop: Dp = 28.dp

    /** Horizontal page gutter on a phone. */
    val gutter: Dp = 20.dp

    /** Minimum interactive size, per accessibility guidance. */
    val minTouchTarget: Dp = 48.dp
}
