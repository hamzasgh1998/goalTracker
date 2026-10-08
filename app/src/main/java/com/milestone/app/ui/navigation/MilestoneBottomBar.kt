package com.milestone.app.ui.navigation

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.milestone.app.ui.theme.MilestoneMotion
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import com.milestone.app.ui.theme.motionTween
import com.milestone.app.ui.theme.resolve

/**
 * The app's navigation bar.
 *
 * A floating pill inset from the screen edges rather than a full-width Material
 * bar: it keeps the page background visible down both sides, which is what stops
 * the layout feeling boxed in, and it reads as part of this product rather than as
 * the default component.
 *
 * Selection is shown three ways at once — a washed pill, accent colour and a
 * slight scale — so it survives both a glance and a colour-blind reading.
 */
@Composable
fun MilestoneBottomBar(
    current: TopLevelDestination?,
    onSelect: (TopLevelDestination) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MilestoneTheme.colors

    Box(
        modifier = modifier
            .fillMaxWidth()
            // Content scrolls behind the bar, so it is faded into the page
            // background on approach. Without this a white card sliding under a
            // white pill collides at the hairline and looks like a rendering bug.
            .background(
                Brush.verticalGradient(
                    0f to Color.Transparent,
                    0.55f to colors.background,
                    1f to colors.background,
                ),
            )
            .navigationBarsPadding()
            .padding(
                start = Spacing.lg,
                end = Spacing.lg,
                top = Spacing.heading,
                bottom = Spacing.md,
            ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MilestoneShapes.pill)
                .background(colors.surface, MilestoneShapes.pill)
                .border(BorderStroke(1.dp, colors.hairline), MilestoneShapes.pill)
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopLevelDestination.entries.forEach { destination ->
                BottomBarItem(
                    destination = destination,
                    selected = destination == current,
                    onClick = { onSelect(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun BottomBarItem(
    destination: TopLevelDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MilestoneTheme.colors

    val contentColor by animateColorAsState(
        targetValue = if (selected) colors.accent else colors.inkFaint,
        animationSpec = motionTween(),
        label = "navItemColor",
    )
    val containerColor by animateColorAsState(
        targetValue = if (selected) colors.accentWash else colors.surface,
        animationSpec = motionTween(),
        label = "navItemContainer",
    )
    val iconScale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = MilestoneMotion.settle<Float>().resolve(),
        label = "navItemScale",
    )

    Column(
        modifier = modifier
            .clip(MilestoneShapes.pill)
            .background(containerColor, MilestoneShapes.pill)
            .selectable(
                selected = selected,
                // The colour and scale changes are the feedback; a ripple on a
                // pill this small just muddies it.
                indication = null,
                interactionSource = null,
                onClick = onClick,
            )
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            imageVector = destination.icon,
            // Null: the label below is the accessible name, and duplicating it
            // would make a screen reader announce every tab twice.
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier
                .size(ICON_SIZE)
                .scale(iconScale),
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = destination.label,
            style = MilestoneTheme.type.labelSmall,
            color = contentColor,
        )
    }
}

private val ICON_SIZE = 22.dp
