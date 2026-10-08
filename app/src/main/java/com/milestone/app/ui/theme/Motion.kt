package com.milestone.app.ui.theme

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.unit.IntOffset

/**
 * Motion tokens.
 *
 * Two rules hold the whole system together: nothing lasts longer than ~420ms, and
 * only *celebration* is allowed to overshoot. Everything else settles without
 * bounce, which is the difference between "premium" and "toy".
 *
 * Every spec is requested through [MilestoneMotion] rather than built inline, so
 * that honouring the user's reduce-motion preference is a single branch
 * ([durationScale]) instead of an audit of every animation in the app.
 */
object MilestoneMotion {

    /** Snappy, for things the finger is directly driving: press, ripple, toggle. */
    const val DurationQuick = 140

    /** Default for layout and colour changes. */
    const val DurationStandard = 260

    /** Screen transitions and progress sweeps. */
    const val DurationSlow = 420

    /** Decelerating — the right curve for content arriving. */
    val EaseOut: Easing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)

    /** Accelerating — for content leaving. */
    val EaseIn: Easing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)

    /** Symmetric, for things that move from A to B and stay on screen. */
    val EaseInOut: Easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)

    /** Settles with no overshoot. The default for progress bars and rings. */
    fun <T> settle(): SpringFactory<T> = SpringFactory(
        dampingRatio = 0.95f,
        stiffness = Spring.StiffnessMediumLow,
    )

    /** A single, restrained overshoot. Milestone completion, achievement unlock. */
    fun <T> celebrate(): SpringFactory<T> = SpringFactory(
        dampingRatio = 0.55f,
        stiffness = 380f,
    )

    /** Firm and fast, for press feedback on nodes and cards. */
    fun <T> press(): SpringFactory<T> = SpringFactory(
        dampingRatio = 0.8f,
        stiffness = Spring.StiffnessHigh,
    )

    /** Carries spring parameters so a visibility threshold can be applied later. */
    data class SpringFactory<T>(val dampingRatio: Float, val stiffness: Float)
}

/**
 * Resolves a spring for a float value, collapsing to an instant change when the
 * user has asked for reduced motion.
 */
@Composable
@ReadOnlyComposable
fun MilestoneMotion.SpringFactory<Float>.resolve(): AnimationSpec<Float> =
    if (LocalReduceMotion.current) {
        tween(durationMillis = 0)
    } else {
        spring(dampingRatio = dampingRatio, stiffness = stiffness)
    }

/** As [resolve], for the offsets used by enter/exit transitions. */
@Composable
@ReadOnlyComposable
fun MilestoneMotion.SpringFactory<IntOffset>.resolveOffset(): FiniteAnimationSpec<IntOffset> =
    if (LocalReduceMotion.current) {
        tween(durationMillis = 0)
    } else {
        spring(
            dampingRatio = dampingRatio,
            stiffness = stiffness,
            visibilityThreshold = IntOffset.VisibilityThreshold,
        )
    }

/**
 * A duration, scaled to zero when reduced motion is on.
 *
 * Prefer this over branching at each call site; it keeps the animation code
 * identical in both modes and means a missed branch degrades to "slightly fast"
 * rather than "ignores the preference".
 */
@Composable
@ReadOnlyComposable
fun durationScale(base: Int): Int = if (LocalReduceMotion.current) 0 else base

/** Convenience tween that respects reduced motion. */
@Composable
@ReadOnlyComposable
fun <T> motionTween(
    durationMillis: Int = MilestoneMotion.DurationStandard,
    delayMillis: Int = 0,
    easing: Easing = MilestoneMotion.EaseOut,
): FiniteAnimationSpec<T> = tween(
    durationMillis = durationScale(durationMillis),
    delayMillis = durationScale(delayMillis),
    easing = easing,
)
