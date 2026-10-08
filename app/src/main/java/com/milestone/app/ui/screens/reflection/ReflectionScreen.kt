package com.milestone.app.ui.screens.reflection

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.data.repository.WeeklySummary
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.Hairline
import com.milestone.app.ui.components.IconActionButton
import com.milestone.app.ui.components.MilestoneCard
import com.milestone.app.ui.components.MilestoneTextField
import com.milestone.app.ui.components.PrimaryButton
import com.milestone.app.ui.components.QuotedText
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.theme.MilestoneTextStyles
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import java.time.format.DateTimeFormatter

/**
 * The weekly reflection: five questions, and what the week actually contained.
 *
 * The tallies above the questions are read straight from the journey — real
 * counts of real events, not a generated summary. That ordering is deliberate:
 * seeing "3 milestones, 1 goal finished" before being asked "what moved forward?"
 * means the user answers from evidence rather than from mood, which is the whole
 * point of doing this weekly.
 */
@Composable
fun ReflectionScreen(
    onBack: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: ReflectionViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MilestoneTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
            .imePadding(),
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

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter),
        ) {
            Eyebrow(
                text = if (state.isCurrentWeek) {
                    "Week " + state.weekNumber
                } else {
                    "Week " + state.weekNumber + " · looking back"
                },
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = if (state.isCurrentWeek) "Your week" else "That week",
                style = MilestoneTheme.type.displayMedium,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.height(Spacing.xs))
            Text(
                text = DATE_FORMAT.format(state.weekStart) + " – " +
                    DATE_FORMAT.format(state.weekStart.plusDays(6)),
                style = MilestoneTheme.type.bodyMedium,
                color = colors.inkMuted,
            )

            Spacer(modifier = Modifier.height(Spacing.heading))

            state.summary?.let { summary ->
                MovedForwardBlock(summary = summary)
                Spacer(modifier = Modifier.height(Spacing.section))
            }

            ReflectionQuestion.entries.forEach { question ->
                MilestoneTextField(
                    value = state.answerFor(question),
                    onValueChange = { viewModel.onAnswerChange(question, it) },
                    label = question.prompt,
                    placeholder = question.hint,
                    maxLines = 4,
                    minHeight = 92.dp,
                    imeAction = if (question == ReflectionQuestion.entries.last()) {
                        ImeAction.Done
                    } else {
                        ImeAction.Next
                    },
                )
                Spacer(modifier = Modifier.height(Spacing.xxl))
            }

            Spacer(modifier = Modifier.height(Spacing.sm))

            PrimaryButton(
                text = when {
                    !state.alreadyWritten -> "Save this week"
                    state.isCurrentWeek -> "Update this week"
                    // An older entry is being revised, not written for the week
                    // the user is currently living through.
                    else -> "Update this entry"
                },
                enabled = state.hasAnyAnswer && !state.isSaving,
                trailingIcon = MilestoneIcons.Check,
                onClick = viewModel::onSave,
            )

            AnimatedVisibility(visible = state.justSaved) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = Spacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (state.isCurrentWeek) {
                            "Saved. See you next week."
                        } else {
                            "Saved."
                        },
                        style = MilestoneTheme.type.labelMedium,
                        color = colors.success,
                    )
                }
            }

            Spacer(
                modifier = Modifier.height(
                    contentPadding.calculateBottomPadding() + Spacing.section,
                ),
            )
        }
    }
}

/**
 * What the week actually contained, from the event log.
 *
 * Shows nothing but counts and the user's own milestone titles — there is no
 * summarisation here, generated or otherwise. A quiet week says so plainly rather
 * than being dressed up.
 */
@Composable
private fun MovedForwardBlock(summary: WeeklySummary) {
    val colors = MilestoneTheme.colors

    MilestoneCard(contentPadding = Spacing.xxl) {
        Eyebrow(text = if (summary.hasActivity) "You moved forward" else "A quiet week")

        Spacer(modifier = Modifier.height(Spacing.lg))

        if (!summary.hasActivity) {
            Text(
                text = "Nothing was logged this week.",
                style = MilestoneTheme.type.bodyLarge,
                color = colors.inkMuted,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Quiet weeks are part of it. The questions below are still " +
                    "worth answering.",
                style = MilestoneTheme.type.bodySmall,
                color = colors.inkFaint,
            )
            return@MilestoneCard
        }

        TallyRow(count = summary.milestonesCompleted, singular = "milestone completed")
        TallyRow(count = summary.goalsCompleted, singular = "goal finished")
        TallyRow(count = summary.goalsStarted, singular = "goal started")
        TallyRow(count = summary.questsCompleted, singular = "smaller step done")

        if (summary.milestoneTitles.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.lg))
            Hairline()
            Spacer(modifier = Modifier.height(Spacing.lg))
            summary.milestoneTitles.take(MAX_LISTED).forEach { title ->
                Text(
                    text = "·  " + title,
                    style = MilestoneTheme.type.bodyMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(vertical = 2.dp),
                )
            }
            if (summary.milestoneTitles.size > MAX_LISTED) {
                Text(
                    text = "and " + (summary.milestoneTitles.size - MAX_LISTED) + " more",
                    style = MilestoneTheme.type.labelSmall,
                    color = colors.inkFaint,
                    modifier = Modifier.padding(top = Spacing.xs),
                )
            }
        }

        summary.highlight?.let { highlight ->
            Spacer(modifier = Modifier.height(Spacing.xl))
            Eyebrow(text = "Your highlight")
            Spacer(modifier = Modifier.height(Spacing.md))
            QuotedText(text = highlight)
        }
    }
}

/** One tally line. Renders nothing at all when the count is zero. */
@Composable
private fun TallyRow(count: Int, singular: String) {
    if (count == 0) return

    Row(
        modifier = Modifier.padding(vertical = Spacing.xs),
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = count.toString(),
            style = MilestoneTextStyles.MetricSmall,
            color = MilestoneTheme.colors.ink,
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = if (count == 1) singular else plural(singular),
            style = MilestoneTheme.type.bodyMedium,
            color = MilestoneTheme.colors.inkMuted,
            modifier = Modifier.padding(bottom = 1.dp),
        )
    }
}

/** Pluralises "milestone completed" as "milestones completed". */
private fun plural(singular: String): String {
    val words = singular.split(" ")
    return (listOf(words.first() + "s") + words.drop(1)).joinToString(" ")
}

private const val MAX_LISTED = 5
private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
