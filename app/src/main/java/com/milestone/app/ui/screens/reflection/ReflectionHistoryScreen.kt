package com.milestone.app.ui.screens.reflection

import androidx.compose.foundation.background
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
import com.milestone.app.ui.components.IconActionButton
import com.milestone.app.ui.components.MilestoneCard
import com.milestone.app.ui.components.QuotedText
import com.milestone.app.ui.components.Tag
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Every week the user has written about.
 *
 * Reflections were being written and then becoming unreachable: the entry only
 * ever opened the current week, so five weeks of someone's own sentences sat in
 * the database with no way back to them. Re-reading them is most of the value of
 * having written them.
 *
 * Tapping a week opens the ordinary reflection screen for that week, so an old
 * entry can be added to rather than merely viewed.
 */
@Composable
fun ReflectionHistoryScreen(
    onBack: () -> Unit,
    onOpenWeek: (LocalDate) -> Unit,
    contentPadding: PaddingValues,
    viewModel: ReflectionHistoryViewModel = hiltViewModel(),
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

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                bottom = contentPadding.calculateBottomPadding() + Spacing.section,
            ),
        ) {
            item(key = "header") {
                Eyebrow(text = "In your own words")
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Past reflections",
                    style = MilestoneTheme.type.displayMedium,
                    color = colors.ink,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = weeksWrittenLabel(state.weeksWritten),
                    style = MilestoneTheme.type.bodyMedium,
                    color = colors.inkMuted,
                )
                Spacer(modifier = Modifier.height(Spacing.heading))
            }

            if (state.entries.isEmpty() && !state.isLoading) {
                item(key = "empty") {
                    EmptyState(
                        glyph = "🕯",
                        headline = "Nothing written yet.",
                        body = "Once you've sat down with a week, it will be kept " +
                            "here — the small things you noticed are the ones you " +
                            "forget first.",
                    )
                }
            }

            items(items = state.entries, key = { it.weekStart.toString() }) { entry ->
                ReflectionHistoryCard(
                    entry = entry,
                    today = state.today,
                    onClick = { onOpenWeek(entry.weekStart) },
                )
            }
        }
    }
}

@Composable
private fun ReflectionHistoryCard(
    entry: ReflectionHistoryEntry,
    today: LocalDate,
    onClick: () -> Unit,
) {
    val colors = MilestoneTheme.colors

    MilestoneCard(
        modifier = Modifier.padding(bottom = Spacing.md),
        onClick = onClick,
        contentPadding = Spacing.xl,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Week " + entry.weekNumber,
                    style = MilestoneTheme.type.titleMedium,
                    color = colors.ink,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = rangeLabel(entry.weekStart, today),
                    style = MilestoneTheme.type.labelMedium,
                    color = colors.inkFaint,
                )
            }
            if (entry.isCurrentWeek) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                Tag(
                    text = "This week",
                    color = colors.accent,
                    containerColor = colors.accentWash,
                )
            }
        }

        entry.excerpt?.let { excerpt ->
            Spacer(modifier = Modifier.height(Spacing.lg))
            QuotedText(text = excerpt, maxLines = 3)
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(
            // The count, not a judgement: one answer out of five is still a week
            // the user showed up for.
            text = answeredLabel(entry.answeredCount),
            style = MilestoneTheme.type.labelSmall,
            color = colors.inkFaint,
        )
    }
}

/**
 * "6 – 12 October", with the month repeated only when the week straddles two,
 * and the year added only when it is not the current one.
 *
 * [today] is passed in rather than read from the system clock so the label is
 * deterministic in tests and consistent with the rest of the screen's dates.
 */
internal fun rangeLabel(weekStart: LocalDate, today: LocalDate): String {
    val weekEnd = weekStart.plusDays(6)
    val start = if (weekStart.month == weekEnd.month) {
        weekStart.dayOfMonth.toString()
    } else {
        SHORT_FORMAT.format(weekStart)
    }
    val range = start + " – " + LONG_FORMAT.format(weekEnd)
    return if (weekEnd.year == today.year) range else range + " " + weekEnd.year
}

private fun answeredLabel(count: Int): String = when (count) {
    0 -> "Opened, nothing written"
    1 -> "1 of 5 questions answered"
    5 -> "All five answered"
    else -> "$count of 5 questions answered"
}

private fun weeksWrittenLabel(count: Int): String = when (count) {
    0 -> "Your weekly reflections will collect here."
    1 -> "One week, written down."
    else -> "$count weeks, written down."
}

private val SHORT_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
private val LONG_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
