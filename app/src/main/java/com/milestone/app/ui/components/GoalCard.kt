package com.milestone.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.theme.MilestoneTextStyles
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * The featured goal card: one per screen at most, used for the season's lead goal.
 *
 * Gets the display type size, the segmented progress bar and the next step spelled
 * out. The point of having two card sizes at all is that Home can state one thing
 * loudly instead of five things evenly — a wall of equal cards is exactly the
 * "boring dashboard" this design is avoiding.
 */
@Composable
fun FeaturedGoalCard(
    goal: GoalUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MilestoneCard(modifier = modifier, onClick = onClick, contentPadding = Spacing.xxl) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = goal.emoji, style = MilestoneTheme.type.headlineLarge)
            Spacer(modifier = Modifier.width(Spacing.md))
            Tag(
                text = goal.areaName,
                color = goal.areaColor,
                containerColor = goal.areaColor.copy(alpha = 0.12f),
            )
            Spacer(modifier = Modifier.weight(1f))
            HealthDot(health = goal.health)
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            text = goal.title,
            style = MilestoneTheme.type.displaySmall,
            color = MilestoneTheme.colors.ink,
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${goal.progressPercent}%",
                style = MilestoneTextStyles.Metric,
                color = MilestoneTheme.colors.ink,
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(
                text = goal.milestoneLabel,
                style = MilestoneTheme.type.bodyMedium,
                color = MilestoneTheme.colors.inkMuted,
                // Nudges the count onto the metric's baseline.
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        MilestoneProgressBar(
            progress = goal.progress,
            steps = goal.totalMilestones,
            color = goal.areaColor,
            label = "${goal.title}: ${goal.progressPercent}% complete",
        )

        if (goal.nextStep != null) {
            Spacer(modifier = Modifier.height(Spacing.xl))
            Hairline()
            Spacer(modifier = Modifier.height(Spacing.lg))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow(text = "Your next step")
                    Spacer(modifier = Modifier.height(Spacing.xs))
                    Text(
                        text = goal.nextStep,
                        style = MilestoneTheme.type.titleMedium,
                        color = MilestoneTheme.colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(modifier = Modifier.width(Spacing.md))
                Icon(
                    imageVector = MilestoneIcons.ChevronRight,
                    contentDescription = null,
                    tint = MilestoneTheme.colors.inkFaint,
                    modifier = Modifier.width(18.dp),
                )
            }
        }
    }
}

/**
 * The standard goal row.
 *
 * Compact enough to scan a list of twelve, but still carries progress, health and
 * area — the three things that decide whether the user opens it.
 */
@Composable
fun GoalRowCard(
    goal: GoalUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showPriority: Boolean = false,
) {
    MilestoneCard(modifier = modifier, onClick = onClick, contentPadding = Spacing.xl) {
        Row(verticalAlignment = Alignment.Top) {
            Text(text = goal.emoji, style = MilestoneTheme.type.headlineMedium)
            Spacer(modifier = Modifier.width(Spacing.md))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    style = MilestoneTheme.type.titleLarge,
                    color = MilestoneTheme.colors.ink,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = goal.areaName,
                        style = MilestoneTheme.type.labelMedium,
                        color = goal.areaColor,
                    )
                    if (showPriority && goal.priority == GoalPriority.Primary) {
                        Text(
                            text = " · Primary",
                            style = MilestoneTheme.type.labelMedium,
                            color = MilestoneTheme.colors.inkFaint,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(Spacing.md))

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${goal.progressPercent}%",
                    style = MilestoneTheme.type.titleMedium,
                    color = MilestoneTheme.colors.ink,
                )
                Spacer(modifier = Modifier.height(Spacing.xs))
                HealthDot(health = goal.health)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        MilestoneProgressBar(
            progress = goal.progress,
            steps = goal.totalMilestones,
            color = goal.areaColor,
            height = 6.dp,
            label = "${goal.title}: ${goal.progressPercent}% complete",
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = goal.milestoneLabel,
                style = MilestoneTheme.type.labelSmall,
                color = MilestoneTheme.colors.inkFaint,
            )
            Text(
                text = goal.healthHeadline,
                style = MilestoneTheme.type.labelSmall,
                color = MilestoneTheme.colors.inkFaint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }
}
