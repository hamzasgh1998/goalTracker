package com.milestone.app.ui.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.data.repository.SeasonProgress
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.FeaturedGoalCard
import com.milestone.app.ui.components.GoalRowCard
import com.milestone.app.ui.components.Hairline
import com.milestone.app.ui.components.LifeAreaOrb
import com.milestone.app.ui.components.MilestoneCard
import com.milestone.app.ui.components.MilestoneProgressBar
import com.milestone.app.ui.components.PrimaryButton
import com.milestone.app.ui.components.SectionHeader
import com.milestone.app.ui.components.TextActionButton
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.model.LifeAreaUi
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTextStyles
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * Home answers one question: where am I in my life right now?
 *
 * The layout is a deliberate hierarchy rather than a stack of equal cards —
 * greeting, then the season, then *one* lead goal at display size, then a single
 * next step, and only after that the supporting material. Section gaps are
 * [Spacing.section] so the page has room to breathe, which is what makes the
 * handful of large elements read as considered instead of sparse.
 */
@Composable
fun HomeScreen(
    onOpenGoal: (Long) -> Unit,
    onOpenArea: (Long) -> Unit,
    onCreateGoal: () -> Unit,
    onOpenSeason: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenAchievements: () -> Unit,
    onSeeAllGoals: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MilestoneTheme.colors

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
        contentPadding = PaddingValues(
            start = Spacing.gutter,
            end = Spacing.gutter,
            top = Spacing.screenTop,
            bottom = contentPadding.calculateBottomPadding() + Spacing.section,
        ),
    ) {
        item(key = "greeting") {
            Column(modifier = Modifier.statusBarsPadding()) {
                Text(
                    text = state.greeting,
                    style = MilestoneTheme.type.titleMedium,
                    color = colors.inkMuted,
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = state.motivation,
                    style = MilestoneTheme.type.headlineLarge,
                    color = colors.ink,
                )
            }
        }

        state.season?.let { season ->
            item(key = "season") {
                Spacer(modifier = Modifier.height(Spacing.section))
                SeasonHeader(
                    season = season,
                    goalProgress = state.seasonProgress,
                    onClick = onOpenSeason,
                )
            }
        }

        if (!state.hasAnyGoals && !state.isLoading) {
            item(key = "empty") {
                Spacer(modifier = Modifier.height(Spacing.section))
                MilestoneCard {
                    Text(
                        text = "No chapters yet.",
                        style = MilestoneTheme.type.headlineLarge,
                        color = colors.ink,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                    Text(
                        text = "Your journey starts with one decision about what " +
                            "matters most right now.",
                        style = MilestoneTheme.type.bodyMedium,
                        color = colors.inkMuted,
                    )
                    Spacer(modifier = Modifier.height(Spacing.xl))
                    PrimaryButton(
                        text = "Create your first goal",
                        trailingIcon = MilestoneIcons.Plus,
                        onClick = onCreateGoal,
                    )
                }
            }
        }

        // The lead goal, at display size. Everything else on this screen is
        // smaller than it on purpose.
        state.focusGoals.firstOrNull()?.let { lead ->
            item(key = "lead-${lead.id}") {
                Spacer(modifier = Modifier.height(Spacing.heading))
                FeaturedGoalCard(goal = lead, onClick = { onOpenGoal(lead.id) })
            }
        }

        state.nextStep?.let { next ->
            item(key = "nextStep") {
                Spacer(modifier = Modifier.height(Spacing.section))
                NextStepBlock(next = next, onContinue = { onOpenGoal(next.goalId) })
            }
        }

        // Nudges sit below the season and the next step on purpose: a weekly
        // prompt is useful, but it must not be the first thing a user sees when
        // the question this screen answers is "where am I right now".
        if (state.showReflectionPrompt) {
            item(key = "reflectionPrompt") {
                Spacer(modifier = Modifier.height(Spacing.heading))
                ReflectionPromptCard(
                    onOpen = onOpenReflection,
                    onDismiss = viewModel::onDismissReflectionPrompt,
                )
            }
        }

        if (state.unseenAchievements.isNotEmpty()) {
            item(key = "achievement") {
                Spacer(modifier = Modifier.height(Spacing.heading))
                val achievement = state.unseenAchievements.first()
                AchievementBanner(
                    glyph = achievement.definition.glyph,
                    title = achievement.definition.title,
                    description = achievement.definition.description,
                    onClick = {
                        viewModel.onAchievementSeen(achievement.definition.key)
                        onOpenAchievements()
                    },
                )
            }
        }

        val otherFocus = state.focusGoals.drop(1)
        if (otherFocus.isNotEmpty()) {
            item(key = "alsoThisSeason") {
                Spacer(modifier = Modifier.height(Spacing.section))
                SectionHeader(eyebrow = "Also this season")
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
            items(items = otherFocus, key = { "focus-${it.id}" }) { goal ->
                GoalRowCard(
                    goal = goal,
                    onClick = { onOpenGoal(goal.id) },
                    modifier = Modifier.padding(bottom = Spacing.md),
                )
            }
        }

        if (state.areas.isNotEmpty()) {
            item(key = "areas") {
                Spacer(modifier = Modifier.height(Spacing.section))
                SectionHeader(
                    eyebrow = "Your life areas",
                    trailing = {
                        TextActionButton(text = "All goals", onClick = onSeeAllGoals)
                    },
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                LifeAreaStrip(areas = state.areas, onOpenArea = onOpenArea)
            }
        }

        if (state.supportingGoals.isNotEmpty()) {
            item(key = "supporting") {
                Spacer(modifier = Modifier.height(Spacing.section))
                SectionHeader(eyebrow = "Keeping going")
                Spacer(modifier = Modifier.height(Spacing.lg))
            }
            items(items = state.supportingGoals, key = { "supporting-${it.id}" }) { goal ->
                SupportingGoalRow(goal = goal, onClick = { onOpenGoal(goal.id) })
            }
        }
    }
}

/**
 * The season block: the largest type on the screen.
 *
 * Shows both numbers that matter and keeps them distinct — the bar is how much of
 * the *work* is done, the days figure is how much *time* is left. Conflating them
 * is the usual mistake and makes a season feel like a deadline.
 */
@Composable
private fun SeasonHeader(
    season: SeasonProgress,
    goalProgress: Float,
    onClick: () -> Unit,
) {
    val colors = MilestoneTheme.colors

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MilestoneShapes.card)
            // The whole block is the target, not just the chevron: it is the
            // largest thing on the screen and people aim at the title.
            .clickable(role = Role.Button, onClick = onClick)
            // Keeps the first and last lines out of the clip's rounded corners:
            // without it the corner radius bites the "C" of CURRENT SEASON and
            // the leading digit of "53 days left".
            .padding(vertical = Spacing.sm),
    ) {
        Eyebrow(text = "Current season")
        Spacer(modifier = Modifier.height(Spacing.md))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = season.season.title,
                style = MilestoneTheme.type.displayMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            Icon(
                imageVector = MilestoneIcons.ChevronRight,
                // Null: the Column carries the click and its text is the label,
                // so describing the chevron too would announce it twice.
                contentDescription = null,
                tint = colors.inkFaint,
                modifier = Modifier
                    .size(Spacing.minTouchTarget)
                    .padding(Spacing.md),
            )
        }

        Text(
            text = season.season.theme,
            style = MilestoneTheme.type.headlineSmall,
            color = colors.accent,
        )

        if (season.season.intention.isNotBlank()) {
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = season.season.intention,
                style = MilestoneTextStyles.Quote,
                color = colors.inkMuted,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        Row(verticalAlignment = Alignment.CenterVertically) {
            MilestoneProgressBar(
                progress = goalProgress,
                modifier = Modifier.weight(1f),
                label = "Season progress: ${(goalProgress * 100).toInt()} percent",
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(
                text = "${(goalProgress * 100).toInt()}%",
                style = MilestoneTheme.type.titleMedium,
                color = colors.ink,
            )
        }

        season.daysRemaining?.let { days ->
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = if (days == 0) "Last day of the season" else "$days days left",
                style = MilestoneTheme.type.labelSmall,
                color = colors.inkFaint,
            )
        }
    }
}

/**
 * The one action the user is invited to take.
 *
 * Set apart with an accent wash and no competing controls. Exactly one step is
 * shown, never a list — the whole reason this block exists is that a list of
 * thirty tasks is what the user already has elsewhere and does not act on.
 */
@Composable
private fun NextStepBlock(next: NextStepUi, onContinue: () -> Unit) {
    val colors = MilestoneTheme.colors

    Column(modifier = Modifier.fillMaxWidth()) {
        Eyebrow(text = "Today")
        Spacer(modifier = Modifier.height(Spacing.lg))

        MilestoneCard(
            containerColor = colors.accentWash,
            borderColor = colors.accent.copy(alpha = 0.22f),
            onClick = onContinue,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = next.goalEmoji, style = MilestoneTheme.type.headlineMedium)
                Spacer(modifier = Modifier.width(Spacing.md))
                Text(
                    text = next.goalTitle,
                    style = MilestoneTheme.type.labelMedium,
                    color = colors.inkMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.lg))

            Text(
                text = next.milestoneTitle,
                style = MilestoneTheme.type.headlineLarge,
                color = colors.ink,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            PrimaryButton(
                text = "Continue journey",
                trailingIcon = MilestoneIcons.ArrowRight,
                onClick = onContinue,
            )
        }
    }
}

/** Horizontally scrolling row of life-area rings. */
@Composable
private fun LifeAreaStrip(areas: List<LifeAreaUi>, onOpenArea: (Long) -> Unit) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        // Trailing padding so the last ring can scroll clear of the gutter.
        contentPadding = PaddingValues(end = Spacing.gutter),
    ) {
        items(items = areas, key = { it.id }) { area ->
            LifeAreaOrb(area = area, onClick = { onOpenArea(area.id) })
        }
    }
}

/** A minimal row for maintenance goals: name, area dot, percentage. */
@Composable
private fun SupportingGoalRow(goal: GoalUi, onClick: () -> Unit) {
    val colors = MilestoneTheme.colors

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(goal.areaColor, CircleShape),
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(
                text = goal.emoji,
                style = MilestoneTheme.type.bodyLarge,
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = goal.title,
                style = MilestoneTheme.type.bodyLarge,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = "${goal.progressPercent}%",
                style = MilestoneTheme.type.labelMedium,
                color = colors.inkFaint,
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Icon(
                imageVector = MilestoneIcons.ChevronRight,
                contentDescription = null,
                tint = colors.inkFaint,
                modifier = Modifier.size(16.dp),
            )
        }
        Hairline()
    }
}

@Composable
private fun ReflectionPromptCard(onOpen: () -> Unit, onDismiss: () -> Unit) {
    MilestoneCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = MilestoneIcons.Pen,
                contentDescription = null,
                tint = MilestoneTheme.colors.accent,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Eyebrow(text = "Your week")
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = "Look back on this week?",
            style = MilestoneTheme.type.headlineMedium,
            color = MilestoneTheme.colors.ink,
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Text(
            text = "Five short questions. Takes two minutes.",
            style = MilestoneTheme.type.bodyMedium,
            color = MilestoneTheme.colors.inkMuted,
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(verticalAlignment = Alignment.CenterVertically) {
            TextActionButton(text = "Reflect", onClick = onOpen)
            Spacer(modifier = Modifier.width(Spacing.sm))
            TextActionButton(
                text = "Not now",
                color = MilestoneTheme.colors.inkFaint,
                onClick = onDismiss,
            )
        }
    }
}

@Composable
private fun AchievementBanner(
    glyph: String,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    MilestoneCard(
        onClick = onClick,
        containerColor = MilestoneTheme.colors.surfaceMuted,
        borderColor = MilestoneTheme.colors.hairline,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = glyph, style = MilestoneTheme.type.headlineLarge)
            Spacer(modifier = Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow(text = "Unlocked")
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = title,
                    style = MilestoneTheme.type.titleLarge,
                    color = MilestoneTheme.colors.ink,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MilestoneTheme.type.bodySmall,
                    color = MilestoneTheme.colors.inkMuted,
                )
            }
            Icon(
                imageVector = MilestoneIcons.ChevronRight,
                contentDescription = null,
                tint = MilestoneTheme.colors.inkFaint,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}
