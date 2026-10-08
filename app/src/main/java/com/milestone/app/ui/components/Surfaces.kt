package com.milestone.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.milestone.app.ui.theme.MilestoneMotion
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import com.milestone.app.ui.theme.resolve

/**
 * The app's card surface.
 *
 * Deliberately *not* Material's `Card`: this uses a hairline border and no
 * elevation shadow. Dozens of drop shadows is what makes a dashboard look cheap,
 * and a 1dp outline separates surfaces just as well in both themes.
 *
 * @param onClick when provided, the whole card becomes one touch target and
 *   scales slightly on press.
 */
@Composable
fun MilestoneCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    shape: Shape = MilestoneShapes.card,
    containerColor: Color = MilestoneTheme.colors.surface,
    borderColor: Color = MilestoneTheme.colors.hairline,
    contentPadding: Dp = Spacing.xl,
    content: @Composable ColumnScope.() -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && onClick != null) PRESSED_SCALE else 1f,
        animationSpec = MilestoneMotion.press<Float>().resolve(),
        label = "cardPress",
    )

    Column(
        modifier = modifier
            .scale(scale)
            .clip(shape)
            .background(color = containerColor, shape = shape)
            .border(BorderStroke(1.dp, borderColor), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        // Null indication: the scale animation above is the press
                        // feedback, and a ripple on top of it reads as noise.
                        indication = null,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(contentPadding),
        content = content,
    )
}

/**
 * A quieter inset surface for rows nested inside a card.
 *
 * Has no border — nesting two outlines looks like a mistake.
 */
@Composable
fun SunkenPanel(
    modifier: Modifier = Modifier,
    shape: Shape = MilestoneShapes.control,
    contentPadding: Dp = Spacing.lg,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(shape)
            .background(color = MilestoneTheme.colors.surfaceMuted, shape = shape)
            .padding(contentPadding),
        content = content,
    )
}

private const val PRESSED_SCALE = 0.985f
