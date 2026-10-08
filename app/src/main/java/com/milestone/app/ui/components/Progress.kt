package com.milestone.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.milestone.app.ui.theme.MilestoneMotion
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.resolve
import kotlin.math.roundToInt

/**
 * The app's progress bar.
 *
 * Below [MAX_SEGMENTS] steps it renders as discrete blocks, one per milestone, so
 * the bar answers "how many left" and not just "roughly how far" — the same
 * reading as `████████░░`. Above that it falls back to a continuous bar, because
 * thirty slivers communicate nothing.
 *
 * @param progress 0f..1f.
 * @param steps total milestones, or `null` to always draw continuous.
 * @param label spoken description; without one the bar is hidden from screen
 *   readers on the assumption that nearby text already says "7 of 9".
 */
@Composable
fun MilestoneProgressBar(
    progress: Float,
    modifier: Modifier = Modifier,
    steps: Int? = null,
    color: Color = MilestoneTheme.colors.accent,
    trackColor: Color = MilestoneTheme.colors.surfaceSunken,
    height: Dp = 8.dp,
    label: String? = null,
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = MilestoneMotion.settle<Float>().resolve(),
        label = "progress",
    )

    val semantics = if (label != null) {
        Modifier.clearAndSetSemantics {
            contentDescription = label
            progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
        }
    } else {
        Modifier.clearAndSetSemantics { }
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .then(semantics),
    ) {
        val radius = size.height / 2f
        val useSegments = steps != null && steps in 2..MAX_SEGMENTS

        if (!useSegments) {
            drawRoundRect(
                color = trackColor,
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            )
            if (animated > 0f) {
                drawRoundRect(
                    color = color,
                    size = Size(size.width * animated, size.height),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
                )
            }
            return@Canvas
        }

        val count = steps
        val gap = (size.height * SEGMENT_GAP_RATIO).coerceAtMost(size.width / (count * 3f))
        val segmentWidth = (size.width - gap * (count - 1)) / count
        // Round rather than floor: a bar showing 5 of 9 should fill five blocks,
        // and floating-point division would occasionally fill four.
        val filled = (animated * count).roundToInt().coerceIn(0, count)

        repeat(count) { index ->
            drawRoundRect(
                color = if (index < filled) color else trackColor,
                topLeft = Offset(x = index * (segmentWidth + gap), y = 0f),
                size = Size(segmentWidth, size.height),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(radius),
            )
        }
    }
}

/**
 * A circular progress ring, used for life areas and season completion.
 *
 * Starts at twelve o'clock and sweeps clockwise. The track is drawn full-circle
 * underneath so an empty ring still reads as a container waiting to be filled,
 * rather than as a missing element.
 *
 * @param content drawn in the middle — usually the percentage.
 */
@Composable
fun ProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    diameter: Dp = 72.dp,
    strokeWidth: Dp = 6.dp,
    color: Color = MilestoneTheme.colors.accent,
    trackColor: Color = MilestoneTheme.colors.surfaceSunken,
    label: String? = null,
    content: @Composable () -> Unit = {},
) {
    val target = progress.coerceIn(0f, 1f)
    val animated by animateFloatAsState(
        targetValue = target,
        animationSpec = MilestoneMotion.settle<Float>().resolve(),
        label = "ring",
    )

    Box(
        modifier = modifier
            .size(diameter)
            .then(
                if (label != null) {
                    Modifier.clearAndSetSemantics {
                        contentDescription = label
                        progressBarRangeInfo = ProgressBarRangeInfo(target, 0f..1f)
                    }
                } else {
                    Modifier
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(diameter)) {
            val stroke = strokeWidth.toPx()
            // Inset by half the stroke so the ring's outer edge lands on the bounds
            // instead of being clipped.
            val inset = stroke / 2f
            val arcSize = Size(size.width - stroke, size.height - stroke)

            drawArc(
                color = trackColor,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = Offset(inset, inset),
                size = arcSize,
                style = Stroke(width = stroke, cap = StrokeCap.Round),
            )

            if (animated > 0f) {
                drawArc(
                    color = color,
                    startAngle = -90f,
                    sweepAngle = 360f * animated,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = arcSize,
                    style = Stroke(width = stroke, cap = StrokeCap.Round),
                )
            }
        }
        content()
    }
}

/** Below this many steps, a segmented bar is clearer than a continuous one. */
private const val MAX_SEGMENTS = 14
private const val SEGMENT_GAP_RATIO = 0.5f
