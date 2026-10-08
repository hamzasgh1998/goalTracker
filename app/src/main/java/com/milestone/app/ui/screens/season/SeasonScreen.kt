package com.milestone.app.ui.screens.season

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.ui.components.EmptyState
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.Hairline
import com.milestone.app.ui.components.IconActionButton
import com.milestone.app.ui.components.MilestoneCard
import com.milestone.app.ui.components.MilestoneProgressBar
import com.milestone.app.ui.components.MilestoneTextField
import com.milestone.app.ui.components.PrimaryButton
import com.milestone.app.ui.components.SecondaryButton
import com.milestone.app.ui.components.SelectableChip
import com.milestone.app.ui.components.TextActionButton
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTextStyles
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * The season: what the user has chosen to care about right now.
 *
 * Three tiers, shown at three visual weights, because the hierarchy *is* the
 * feature. Primary goals get full cards; maintenance goals get a single line.
 * If a user promotes a fourth goal to primary the screen says so plainly rather
 * than blocking it — the discipline is theirs to keep, not the app's to enforce.
 */
@Composable
fun SeasonScreen(
    onBack: () -> Unit,
    onOpenGoal: (Long) -> Unit,
    contentPadding: PaddingValues,
    viewModel: SeasonViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MilestoneTheme.colors

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
        }

        val season = state.season
        if (season == null && !state.isLoading) {
            EmptyState(
                glyph = "🌅",
                headline = "No season running.",
                body = "A season is a stretch of time with one idea behind it. " +
                    "Starting one is how you decide what matters for a while.",
                actionText = "Begin a season",
                onAction = viewModel::onStartNewSeason,
            )
        } else if (season != null) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    start = Spacing.gutter,
                    end = Spacing.gutter,
                    bottom = contentPadding.calculateBottomPadding() + Spacing.section,
                ),
            ) {
                item(key = "header") {
                    Eyebrow(text = "This season")
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Text(
                        text = season.title,
                        style = MilestoneTheme.type.displayLarge,
                        color = colors.ink,
                    )
                    Text(
                        text = season.theme,
                        style = MilestoneTheme.type.headlineSmall,
                        color = colors.accent,
                    )

                    if (season.intention.isNotBlank()) {
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        Text(
                            text = season.intention,
                            style = MilestoneTextStyles.Quote,
                            color = colors.inkMuted,
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.xl))

                    MilestoneProgressBar(
                        progress = state.goalProgress,
                        label = "Season progress",
                    )
                    Spacer(modifier = Modifier.height(Spacing.sm))
                    Row(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = (state.goalProgress * 100).toInt().toString() +
                                "% across your primary goals",
                            style = MilestoneTheme.type.labelSmall,
                            color = colors.inkFaint,
                            modifier = Modifier.weight(1f),
                        )
                        state.progress?.daysRemaining?.let { days ->
                            Text(
                                text = days.toString() + " days left",
                                style = MilestoneTheme.type.labelSmall,
                                color = colors.inkFaint,
                            )
                        }
                    }

                    if (state.isOverCommitted) {
                        Spacer(modifier = Modifier.height(Spacing.lg))
                        OverCommittedNote(count = state.primary.size)
                    }
                }

                tierSection(
                    eyebrow = "Primary",
                    note = "The few you actually push on.",
                    goals = state.primary,
                    emptyNote = "Nothing primary yet. Promote a goal below.",
                    onOpenGoal = onOpenGoal,
                    onRetier = viewModel::onStartRetiering,
                    featured = true,
                )

                tierSection(
                    eyebrow = "Secondary",
                    note = "Moving, but not the priority.",
                    goals = state.secondary,
                    emptyNote = null,
                    onOpenGoal = onOpenGoal,
                    onRetier = viewModel::onStartRetiering,
                    featured = false,
                )

                tierSection(
                    eyebrow = "Maintenance",
                    note = "Kept alive, not pushed forward.",
                    goals = state.maintenance,
                    emptyNote = null,
                    onOpenGoal = onOpenGoal,
                    onRetier = viewModel::onStartRetiering,
                    featured = false,
                )

                if (state.outsideSeason.isNotEmpty()) {
                    item(key = "outsideHeader") {
                        Spacer(modifier = Modifier.height(Spacing.section))
                        Eyebrow(text = "Not in this season")
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        Text(
                            text = "Still yours, just not the focus right now.",
                            style = MilestoneTheme.type.bodySmall,
                            color = colors.inkFaint,
                        )
                        Spacer(modifier = Modifier.height(Spacing.lg))
                    }
                    items(items = state.outsideSeason, key = { "outside-" + it.id }) { goal ->
                        OutsideGoalRow(
                            goal = goal,
                            onOpen = { onOpenGoal(goal.id) },
                            onAdd = { viewModel.onAddToSeason(goal.id) },
                        )
                    }
                }

                item(key = "actions") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    Hairline()
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    SecondaryButton(
                        text = "End this season and start the next",
                        leadingIcon = MilestoneIcons.Calendar,
                        onClick = viewModel::onStartNewSeason,
                    )
                    if (state.pastSeasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(Spacing.xxl))
                        Eyebrow(text = "Seasons before this")
                        Spacer(modifier = Modifier.height(Spacing.md))
                        state.pastSeasons.forEach { past ->
                            Row(modifier = Modifier.padding(vertical = Spacing.sm)) {
                                Text(
                                    text = past.title,
                                    style = MilestoneTheme.type.bodyMedium,
                                    color = colors.inkMuted,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = past.theme,
                                    style = MilestoneTheme.type.labelSmall,
                                    color = colors.inkFaint,
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    state.local.retieringGoalId?.let {
        RetierSheet(
            onChoose = viewModel::onPriorityChosen,
            onRemove = { viewModel.onRemoveFromSeason(it) },
            onDismiss = viewModel::onCancelRetiering,
        )
    }

    if (state.local.isStartingSeason) {
        NewSeasonSheet(
            local = state.local,
            onTitleChange = viewModel::onDraftTitleChange,
            onThemeChange = viewModel::onDraftThemeChange,
            onIntentionChange = viewModel::onDraftIntentionChange,
            onConfirm = viewModel::onConfirmNewSeason,
            onDismiss = viewModel::onCancelNewSeason,
        )
    }
}

/** One tier's worth of goals, as a lazy-list section. */
private fun androidx.compose.foundation.lazy.LazyListScope.tierSection(
    eyebrow: String,
    note: String,
    goals: List<GoalUi>,
    emptyNote: String?,
    onOpenGoal: (Long) -> Unit,
    onRetier: (Long) -> Unit,
    featured: Boolean,
) {
    if (goals.isEmpty() && emptyNote == null) return

    item(key = "tier-" + eyebrow) {
        Spacer(modifier = Modifier.height(Spacing.section))
        Eyebrow(text = eyebrow)
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = note,
            style = MilestoneTheme.type.bodySmall,
            color = MilestoneTheme.colors.inkFaint,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        if (goals.isEmpty() && emptyNote != null) {
            Text(
                text = emptyNote,
                style = MilestoneTheme.type.bodyMedium,
                color = MilestoneTheme.colors.inkMuted,
            )
        }
    }

    items(items = goals, key = { eyebrow + "-" + it.id }) { goal ->
        TierGoalRow(
            goal = goal,
            featured = featured,
            onOpen = { onOpenGoal(goal.id) },
            onRetier = { onRetier(goal.id) },
        )
    }
}

@Composable
private fun TierGoalRow(
    goal: GoalUi,
    featured: Boolean,
    onOpen: () -> Unit,
    onRetier: () -> Unit,
) {
    val colors = MilestoneTheme.colors

    if (featured) {
        MilestoneCard(
            modifier = Modifier.padding(bottom = Spacing.md),
            onClick = onOpen,
            contentPadding = Spacing.xl,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = goal.emoji, style = MilestoneTheme.type.headlineMedium)
                Spacer(modifier = Modifier.width(Spacing.md))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = goal.title,
                        style = MilestoneTheme.type.titleLarge,
                        color = colors.ink,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = goal.areaName,
                        style = MilestoneTheme.type.labelMedium,
                        color = goal.areaColor,
                    )
                }
                Text(
                    text = goal.progressPercent.toString() + "%",
                    style = MilestoneTheme.type.titleMedium,
                    color = colors.ink,
                )
                IconActionButton(
                    icon = MilestoneIcons.More,
                    contentDescription = "Change tier for " + goal.title,
                    onClick = onRetier,
                )
            }
            Spacer(modifier = Modifier.height(Spacing.md))
            MilestoneProgressBar(
                progress = goal.progress,
                steps = goal.totalMilestones,
                color = goal.areaColor,
                height = 6.dp,
                label = goal.title + ": " + goal.progressPercent + "% complete",
            )
        }
    } else {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(role = Role.Button, onClick = onOpen)
                    .padding(vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(goal.areaColor, CircleShape),
                )
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(text = goal.emoji, style = MilestoneTheme.type.bodyLarge)
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = goal.title,
                    style = MilestoneTheme.type.bodyLarge,
                    color = colors.ink,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = goal.progressPercent.toString() + "%",
                    style = MilestoneTheme.type.labelMedium,
                    color = colors.inkFaint,
                )
                IconActionButton(
                    icon = MilestoneIcons.More,
                    contentDescription = "Change tier for " + goal.title,
                    onClick = onRetier,
                )
            }
            Hairline()
        }
    }
}

@Composable
private fun OutsideGoalRow(goal: GoalUi, onOpen: () -> Unit, onAdd: () -> Unit) {
    val colors = MilestoneTheme.colors

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onOpen)
                .padding(vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(text = goal.emoji, style = MilestoneTheme.type.bodyLarge)
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = goal.title,
                style = MilestoneTheme.type.bodyLarge,
                color = colors.inkMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            TextActionButton(text = "Add", leadingIcon = MilestoneIcons.Plus, onClick = onAdd)
        }
        Hairline()
    }
}

@Composable
private fun OverCommittedNote(count: Int) {
    val colors = MilestoneTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colors.attention.copy(alpha = 0.10f), MilestoneShapes.control)
            .padding(Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = MilestoneIcons.Goals,
            contentDescription = null,
            tint = colors.attention,
            modifier = Modifier.size(18.dp),
        )
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            // Observation, not a scolding, and no way to "fix" it is forced.
            text = count.toString() + " primary goals. Three is usually where this " +
                "stops working.",
            style = MilestoneTheme.type.bodySmall,
            color = colors.inkMuted,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RetierSheet(
    onChoose: (GoalPriority) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MilestoneTheme.colors.surface,
        shape = MilestoneShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.section),
        ) {
            Text(
                text = "How much attention?",
                style = MilestoneTheme.type.headlineMedium,
                color = MilestoneTheme.colors.ink,
            )
            Spacer(modifier = Modifier.height(Spacing.xl))

            GoalPriority.entries.forEach { priority ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(role = Role.Button) { onChoose(priority) }
                            .padding(vertical = Spacing.lg),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = priority.label,
                                style = MilestoneTheme.type.titleMedium,
                                color = MilestoneTheme.colors.ink,
                            )
                            Text(
                                text = when (priority) {
                                    GoalPriority.Primary -> "A goal you push on this season."
                                    GoalPriority.Secondary -> "Moving, but not the focus."
                                    GoalPriority.Maintenance -> "Kept alive, not pushed."
                                },
                                style = MilestoneTheme.type.bodySmall,
                                color = MilestoneTheme.colors.inkFaint,
                            )
                        }
                        Icon(
                            imageVector = MilestoneIcons.ChevronRight,
                            contentDescription = null,
                            tint = MilestoneTheme.colors.inkFaint,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Hairline()
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextActionButton(
                    text = "Take out of this season",
                    color = MilestoneTheme.colors.inkMuted,
                    onClick = onRemove,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun NewSeasonSheet(
    local: SeasonLocalState,
    onTitleChange: (String) -> Unit,
    onThemeChange: (String) -> Unit,
    onIntentionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MilestoneTheme.colors.surface,
        shape = MilestoneShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.section)
                .imePadding(),
        ) {
            Text(
                text = "Start a new season",
                style = MilestoneTheme.type.headlineLarge,
                color = MilestoneTheme.colors.ink,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Your current season closes. Nothing is lost — goals stay " +
                    "yours, and you choose which ones come with you.",
                style = MilestoneTheme.type.bodyMedium,
                color = MilestoneTheme.colors.inkMuted,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            MilestoneTextField(
                value = local.draftTitle,
                onValueChange = onTitleChange,
                label = "Season",
                imeAction = ImeAction.Next,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            Eyebrow(text = "Theme")
            Spacer(modifier = Modifier.height(Spacing.md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                local.themeOptions.forEach { option ->
                    SelectableChip(
                        text = option,
                        selected = local.draftTheme == option,
                        onClick = { onThemeChange(option) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xl))

            MilestoneTextField(
                value = local.draftIntention,
                onValueChange = onIntentionChange,
                label = "What is this season for?",
                placeholder = "Optional",
                maxLines = 3,
                minHeight = 88.dp,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            PrimaryButton(
                text = "Begin " + local.draftTitle.ifBlank { "this season" },
                enabled = local.draftTitle.isNotBlank() && local.draftTheme.isNotBlank(),
                onClick = onConfirm,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextActionButton(
                    text = "Not yet",
                    color = MilestoneTheme.colors.inkFaint,
                    onClick = onDismiss,
                )
            }
        }
    }
}
