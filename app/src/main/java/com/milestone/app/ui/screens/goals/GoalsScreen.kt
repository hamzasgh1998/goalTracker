package com.milestone.app.ui.screens.goals

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.ui.components.EmptyState
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.GoalRowCard
import com.milestone.app.ui.components.MilestoneCard
import com.milestone.app.ui.components.SelectableChip
import com.milestone.app.ui.components.TextActionButton
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * The full list of goals, split into Active / Paused / Past Chapters.
 *
 * Past chapters are reachable here as a filter rather than buried in settings:
 * revisiting a closed goal is a normal thing to want, and keeping it one tap away
 * is what makes archiving feel like filing rather than deleting.
 */
@Composable
fun GoalsScreen(
    onOpenGoal: (Long) -> Unit,
    onCreateGoal: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: GoalsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MilestoneTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        Column(
            modifier = Modifier
                .statusBarsPadding()
                .padding(horizontal = Spacing.gutter)
                .padding(top = Spacing.screenTop),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Eyebrow(text = "Everything you're building")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = "Goals",
                        style = MilestoneTheme.type.displayMedium,
                        color = colors.ink,
                    )
                }
                TextActionButton(
                    text = "New",
                    leadingIcon = MilestoneIcons.Plus,
                    onClick = onCreateGoal,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                GoalFilter.entries.forEach { filter ->
                    val count = when (filter) {
                        GoalFilter.Active -> state.activeCount
                        GoalFilter.Paused -> state.pausedCount
                        GoalFilter.PastChapters -> state.closedCount
                    }
                    SelectableChip(
                        text = "${filter.label} $count",
                        selected = state.filter == filter,
                        onClick = { viewModel.onFilterChange(filter) },
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        if (state.goals.isEmpty() && !state.isLoading) {
            Box(modifier = Modifier.fillMaxSize()) {
                EmptyStateFor(filter = state.filter, onCreateGoal = onCreateGoal)
            }
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                bottom = contentPadding.calculateBottomPadding() + Spacing.section,
            ),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            items(items = state.goals, key = { it.id }) { goal ->
                if (state.filter == GoalFilter.PastChapters) {
                    ClosedChapterCard(goal = goal, onClick = { onOpenGoal(goal.id) })
                } else {
                    GoalRowCard(
                        goal = goal,
                        onClick = { onOpenGoal(goal.id) },
                        showPriority = true,
                    )
                }
            }
        }
    }
}

/**
 * A closed goal, shown reflectively rather than as a failure.
 *
 * Leads with the reason the user gave and how far they actually got, because
 * "1 of 5 milestones, wrong timing" is a useful memory, while a red X is not.
 */
@Composable
private fun ClosedChapterCard(goal: GoalUi, onClick: () -> Unit) {
    val colors = MilestoneTheme.colors

    MilestoneCard(
        onClick = onClick,
        containerColor = colors.surfaceMuted,
        contentPadding = Spacing.xl,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = goal.emoji, style = MilestoneTheme.type.headlineMedium)
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    style = MilestoneTheme.type.titleLarge,
                    color = colors.inkMuted,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = goal.closingReason?.label ?: "Completed",
                    style = MilestoneTheme.type.labelMedium,
                    color = if (goal.closingReason == null) colors.success else colors.inkFaint,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = "Got to ${goal.completedMilestones} of ${goal.totalMilestones} milestones",
            style = MilestoneTheme.type.labelSmall,
            color = colors.inkFaint,
        )
    }
}

@Composable
private fun EmptyStateFor(filter: GoalFilter, onCreateGoal: () -> Unit) {
    when (filter) {
        GoalFilter.Active -> EmptyState(
            glyph = "🌱",
            headline = "No chapters yet.",
            body = "Your journey starts with one decision about what matters most " +
                "right now.",
            actionText = "Create your first goal",
            onAction = onCreateGoal,
        )

        GoalFilter.Paused -> EmptyState(
            glyph = "⏸",
            headline = "Nothing set down.",
            body = "When something isn't the right fight today, pausing it is a " +
                "decision, not a defeat.",
        )

        GoalFilter.PastChapters -> EmptyState(
            glyph = "📖",
            headline = "No past chapters.",
            body = "Goals you finish or close will be kept here, so you can look " +
                "back on them.",
        )
    }
}
