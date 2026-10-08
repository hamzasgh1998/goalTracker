package com.milestone.app.ui.screens.achievements

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.data.repository.AchievementState
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.IconActionButton
import com.milestone.app.ui.components.MilestoneCard
import com.milestone.app.ui.components.MilestoneProgressBar
import com.milestone.app.ui.components.SectionHeader
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * The achievement catalogue, earned and unearned.
 *
 * Locked entries are shown rather than hidden, but they state the *act* that earns
 * them ("Resume a paused goal") instead of teasing a reward. The point is that
 * they describe a way of working the product believes in, not a collection to
 * complete — so nothing here is a progress bar toward a trophy.
 */
@Composable
fun AchievementsScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: AchievementsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MilestoneTheme.colors

    // Opening the screen counts as seeing them; the Home banner clears.
    LaunchedEffect(Unit) { viewModel.onScreenSeen() }

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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                bottom = contentPadding.calculateBottomPadding() + Spacing.section,
            ),
        ) {
            item(key = "header") {
                Eyebrow(text = "Earned along the way")
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Achievements",
                    style = MilestoneTheme.type.displayMedium,
                    color = colors.ink,
                )
                Spacer(modifier = Modifier.height(Spacing.xl))

                MilestoneProgressBar(
                    progress = if (state.total == 0) {
                        0f
                    } else {
                        state.earnedCount.toFloat() / state.total
                    },
                    steps = state.total,
                    label = state.earnedCount.toString() + " of " +
                        state.total + " achievements earned",
                )
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = state.earnedCount.toString() + " of " + state.total,
                    style = MilestoneTheme.type.labelSmall,
                    color = colors.inkFaint,
                )
            }

            if (state.earned.isNotEmpty()) {
                item(key = "earnedHeader") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    SectionHeader(eyebrow = "Earned")
                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
                items(items = state.earned, key = { it.definition.key }) { achievement ->
                    AchievementRow(
                        achievement = achievement,
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                }
            }

            if (state.locked.isNotEmpty()) {
                item(key = "lockedHeader") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    SectionHeader(eyebrow = "Still ahead")
                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
                items(items = state.locked, key = { it.definition.key }) { achievement ->
                    AchievementRow(
                        achievement = achievement,
                        modifier = Modifier.padding(bottom = Spacing.md),
                    )
                }
            }
        }
    }
}

@Composable
private fun AchievementRow(
    achievement: AchievementState,
    modifier: Modifier = Modifier,
) {
    val colors = MilestoneTheme.colors
    val unlocked = achievement.isUnlocked

    MilestoneCard(
        modifier = modifier,
        contentPadding = Spacing.xl,
        containerColor = if (unlocked) colors.surface else colors.background,
        borderColor = colors.hairline,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        color = if (unlocked) colors.accentWash else colors.surfaceMuted,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = achievement.definition.glyph,
                    style = MilestoneTheme.type.headlineMedium,
                    // Locked glyphs are dimmed rather than replaced with a padlock:
                    // the shape of what is coming stays legible.
                    modifier = Modifier.alpha(if (unlocked) 1f else LOCKED_ALPHA),
                )
            }

            Spacer(modifier = Modifier.width(Spacing.lg))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = achievement.definition.title,
                        style = MilestoneTheme.type.titleLarge,
                        color = if (unlocked) colors.ink else colors.inkMuted,
                    )
                    if (achievement.isNew) {
                        Spacer(modifier = Modifier.width(Spacing.sm))
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .background(colors.accent, CircleShape),
                        )
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (unlocked) {
                        achievement.definition.description
                    } else {
                        achievement.definition.hint
                    },
                    style = MilestoneTheme.type.bodySmall,
                    color = colors.inkFaint,
                )
            }

            achievement.unlockedAt?.let { at ->
                Spacer(modifier = Modifier.width(Spacing.sm))
                Text(
                    text = DATE_FORMAT.format(at.atZone(ZoneId.systemDefault()).toLocalDate()),
                    style = MilestoneTheme.type.labelSmall,
                    color = colors.inkFaint,
                )
            }
        }
    }
}

private const val LOCKED_ALPHA = 0.35f
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
