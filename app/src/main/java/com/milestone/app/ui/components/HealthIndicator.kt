package com.milestone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import com.milestone.app.domain.model.GoalHealth
import com.milestone.app.ui.theme.MilestoneTheme

/** The palette colour that carries each health state's meaning. */
@Composable
@ReadOnlyComposable
fun GoalHealth.color(): Color = when (this) {
    GoalHealth.Healthy -> MilestoneTheme.colors.success
    GoalHealth.AtRisk -> MilestoneTheme.colors.attention
    GoalHealth.Stalled -> MilestoneTheme.colors.stalled
    GoalHealth.Paused -> MilestoneTheme.colors.paused
}

/** Short label. Paired with [color] so meaning never rests on hue alone. */
val GoalHealth.label: String
    get() = when (this) {
        GoalHealth.Healthy -> "Healthy"
        GoalHealth.AtRisk -> "Needs attention"
        GoalHealth.Stalled -> "Stalled"
        GoalHealth.Paused -> "Paused"
    }

/**
 * A small status dot.
 *
 * Carries a content description because on a dense list this dot is sometimes the
 * only indication of a goal's state, and colour alone is invisible to a screen
 * reader.
 */
@Composable
fun HealthDot(
    health: GoalHealth,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 8.dp,
) {
    val description = health.label
    Box(
        modifier = modifier
            .size(size)
            .background(color = health.color(), shape = CircleShape)
            .clearAndSetSemantics { contentDescription = description },
    )
}

/** Dot plus label, for a goal's detail header. */
@Composable
fun HealthBadge(
    health: GoalHealth,
    modifier: Modifier = Modifier,
    text: String = health.label,
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        HealthDot(health = health)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = text,
            style = MilestoneTheme.type.labelMedium,
            color = MilestoneTheme.colors.inkMuted,
        )
    }
}
