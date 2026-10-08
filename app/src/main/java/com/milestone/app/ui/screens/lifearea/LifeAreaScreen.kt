package com.milestone.app.ui.screens.lifearea

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.ui.components.EmptyState
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.GoalRowCard
import com.milestone.app.ui.components.IconActionButton
import com.milestone.app.ui.components.ProgressRing
import com.milestone.app.ui.components.SectionHeader
import com.milestone.app.ui.components.TextActionButton
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.theme.MilestoneTextStyles
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * One area of a life, with everything filed under it.
 *
 * The ring is drawn in the area's own colour and sized large, because this screen
 * answers a single question — how is this part of my life going — and the number
 * is the answer.
 */
@Composable
fun LifeAreaScreen(
    onBack: () -> Unit,
    onOpenGoal: (Long) -> Unit,
    onCreateGoal: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: LifeAreaViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MilestoneTheme.colors
    val accent = state.area?.let { Color(it.colorArgb) } ?: colors.accent

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconActionButton(
                icon = MilestoneIcons.ArrowLeft,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(modifier = Modifier.weight(1f))
            if (state.area != null) {
                TextActionButton(
                    text = "Stop tracking",
                    color = colors.inkFaint,
                    onClick = {
                        viewModel.onStopTracking()
                        onBack()
                    },
                )
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                bottom = contentPadding.calculateBottomPadding() + Spacing.section,
            ),
        ) {
            item(key = "header") {
                state.area?.let { area ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        ProgressRing(
                            progress = state.progress,
                            diameter = 96.dp,
                            strokeWidth = 7.dp,
                            color = accent,
                            label = area.name + ": " +
                                (state.progress * 100).toInt() + "% complete",
                        ) {
                            Text(text = area.emoji, style = MilestoneTheme.type.displaySmall)
                        }

                        Spacer(modifier = Modifier.width(Spacing.xl))

                        Column {
                            Eyebrow(text = "Life area")
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = area.name,
                                style = MilestoneTheme.type.displaySmall,
                                color = colors.ink,
                            )
                            Spacer(modifier = Modifier.height(Spacing.xs))
                            Text(
                                text = (state.progress * 100).toInt().toString() + "%",
                                style = MilestoneTextStyles.MetricSmall,
                                color = accent,
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(Spacing.xxl))

                    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.xxl)) {
                        Stat(
                            value = (state.active.size + state.paused.size).toString(),
                            label = if (state.active.size + state.paused.size == 1) {
                                "goal"
                            } else {
                                "goals"
                            },
                        )
                        Stat(
                            value = state.milestonesCompleted.toString() + " / " +
                                state.milestonesTotal,
                            label = "milestones",
                        )
                    }
                }
            }

            if (state.isEmpty && !state.isLoading) {
                item(key = "empty") {
                    EmptyState(
                        glyph = state.area?.emoji ?: "◔",
                        headline = "Nothing here yet.",
                        body = "This part of your life has no goals in it. That might " +
                            "be exactly right, or it might be the gap worth closing.",
                        actionText = "Add a goal here",
                        onAction = onCreateGoal,
                    )
                }
            }

            if (state.active.isNotEmpty()) {
                item(key = "activeHeader") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    SectionHeader(eyebrow = "Active")
                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
                items(items = state.active, key = { "active-" + it.id }) { goal ->
                    GoalRowCard(
                        goal = goal,
                        onClick = { onOpenGoal(goal.id) },
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                }
            }

            if (state.paused.isNotEmpty()) {
                item(key = "pausedHeader") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    SectionHeader(eyebrow = "Paused")
                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
                items(items = state.paused, key = { "paused-" + it.id }) { goal ->
                    GoalRowCard(
                        goal = goal,
                        onClick = { onOpenGoal(goal.id) },
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun Stat(value: String, label: String) {
    Column {
        Text(
            text = value,
            style = MilestoneTheme.type.headlineMedium,
            color = MilestoneTheme.colors.ink,
        )
        Text(
            text = label,
            style = MilestoneTheme.type.labelSmall,
            color = MilestoneTheme.colors.inkFaint,
        )
    }
}
