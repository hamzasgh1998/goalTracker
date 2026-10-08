package com.milestone.app.ui.screens.journey

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.data.local.entity.ProgressEventEntity
import com.milestone.app.domain.model.ProgressEventType
import com.milestone.app.ui.components.EmptyState
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * Everything the user has actually done, newest first.
 *
 * This screen exists because people systematically underestimate their own
 * progress — the work disappears the moment it is done. So it is append-only, it
 * is never pruned, and it shows the *snapshot* text recorded at the time rather
 * than joining live rows, which means renaming or closing a goal cannot rewrite
 * what happened.
 *
 * A single continuous rail runs down the whole list, through the day headings as
 * well as the events, so scrolling reads as travelling back along one path.
 */
@Composable
fun JourneyScreen(
    onOpenGoal: (Long) -> Unit,
    contentPadding: PaddingValues,
    viewModel: JourneyViewModel = hiltViewModel(),
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
            Eyebrow(text = "Where you've been")
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Journey",
                style = MilestoneTheme.type.displayMedium,
                color = colors.ink,
            )
            if (!state.isEmpty) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = summaryLine(state),
                    style = MilestoneTheme.type.bodyMedium,
                    color = colors.inkMuted,
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.heading))

        if (state.isEmpty && !state.isLoading) {
            EmptyState(
                glyph = "🌱",
                headline = "Nothing to look back on yet.",
                body = "Every milestone you finish is recorded here, so you can see " +
                    "how far you came.",
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.gutter,
                end = Spacing.gutter,
                bottom = contentPadding.calculateBottomPadding() + Spacing.section,
            ),
        ) {
            state.days.forEachIndexed { dayIndex, day ->
                item(key = "day-" + day.date) {
                    DayHeading(
                        relativeLabel = day.relativeLabel,
                        absoluteLabel = day.absoluteLabel,
                        isFirst = dayIndex == 0,
                    )
                }

                items(
                    count = day.events.size,
                    key = { index -> "event-" + day.events[index].id },
                ) { index ->
                    val event = day.events[index]
                    JourneyEventRow(
                        event = event,
                        isLastOverall = dayIndex == state.days.lastIndex &&
                            index == day.events.lastIndex,
                        onClick = event.goalId?.let { { onOpenGoal(it) } },
                    )
                }
            }
        }
    }
}

private fun summaryLine(state: JourneyUiState): String {
    val parts = buildList {
        if (state.milestonesCompleted > 0) {
            add(
                state.milestonesCompleted.toString() +
                    if (state.milestonesCompleted == 1) " milestone" else " milestones",
            )
        }
        if (state.goalsCompleted > 0) {
            add(
                state.goalsCompleted.toString() +
                    if (state.goalsCompleted == 1) " goal finished" else " goals finished",
            )
        }
    }
    return if (parts.isEmpty()) {
        state.totalEvents.toString() + " moments so far"
    } else {
        parts.joinToString(" · ")
    }
}

/**
 * A day marker on the rail.
 *
 * The rail passes straight through it, so the heading reads as a signpost on the
 * path rather than as a list-section header sitting above a separate list.
 */
@Composable
private fun DayHeading(relativeLabel: String, absoluteLabel: String, isFirst: Boolean) {
    val colors = MilestoneTheme.colors

    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(RAIL_WIDTH)
                .fillMaxHeight(),
        ) {
            Rail(topColor = if (isFirst) Color.Transparent else colors.hairline)
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = MARKER_TOP)
                    .size(9.dp)
                    .background(colors.inkFaint, CircleShape),
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(
            modifier = Modifier.padding(
                top = if (isFirst) 0.dp else Spacing.xl,
                bottom = Spacing.lg,
            ),
        ) {
            Text(
                text = relativeLabel,
                style = MilestoneTheme.type.titleLarge,
                color = colors.ink,
            )
            Text(
                text = absoluteLabel,
                style = MilestoneTheme.type.labelSmall,
                color = colors.inkFaint,
            )
        }
    }
}

@Composable
private fun JourneyEventRow(
    event: ProgressEventEntity,
    isLastOverall: Boolean,
    onClick: (() -> Unit)?,
) {
    val colors = MilestoneTheme.colors
    // Life-area colour recorded at the time, so the timeline stays colour-coded
    // even for goals that have since been closed.
    val accent = event.accentArgb?.let { Color(it) } ?: colors.inkFaint

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                },
            ),
    ) {
        Box(
            modifier = Modifier
                .width(RAIL_WIDTH)
                .fillMaxHeight(),
        ) {
            Rail(
                topColor = colors.hairline,
                bottomColor = if (isLastOverall) Color.Transparent else colors.hairline,
            )
            Box(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = MARKER_TOP)
                    .size(EVENT_DOT)
                    .background(colors.background, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = event.type.glyph,
                    style = MilestoneTheme.type.labelSmall,
                    color = accent,
                )
            }
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(modifier = Modifier.padding(bottom = Spacing.lg)) {
            Text(
                text = event.title,
                style = MilestoneTheme.type.bodyLarge,
                color = colors.ink,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            event.subtitle?.let { subtitle ->
                Spacer(modifier = Modifier.height(1.dp))
                Text(
                    text = subtitle,
                    style = MilestoneTheme.type.labelMedium,
                    color = if (event.type == ProgressEventType.MilestoneCompleted) {
                        accent
                    } else {
                        colors.inkFaint
                    },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * The vertical rail, drawn behind the markers.
 *
 * Each row paints its own segment so one continuous line runs through rows of
 * different heights — the same technique the goal timeline uses.
 */
@Composable
private fun Rail(
    topColor: Color = MilestoneTheme.colors.hairline,
    bottomColor: Color = MilestoneTheme.colors.hairline,
) {
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxHeight().width(RAIL_WIDTH)) {
        val centerX = size.width / 2f
        val markerCenter = with(density) { (MARKER_TOP + EVENT_DOT / 2).toPx() }
        val stroke = with(density) { 1.5.dp.toPx() }

        drawLine(
            color = topColor,
            start = Offset(centerX, 0f),
            end = Offset(centerX, markerCenter),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = bottomColor,
            start = Offset(centerX, markerCenter),
            end = Offset(centerX, size.height),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

private val RAIL_WIDTH = 28.dp
private val EVENT_DOT = 22.dp

/** Distance from the top of a row to the top of its marker. */
private val MARKER_TOP = 2.dp
