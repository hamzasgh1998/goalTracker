package com.milestone.app.ui.screens.pastchapters

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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.data.repository.GoalRepository
import com.milestone.app.domain.GoalHealthCalculator
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.ui.components.EmptyState
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.IconActionButton
import com.milestone.app.ui.components.MilestoneCard
import com.milestone.app.ui.components.MilestoneProgressBar
import com.milestone.app.ui.components.SectionHeader
import com.milestone.app.ui.components.TextActionButton
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.model.toUi
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PastChaptersUiState(
    val isLoading: Boolean = true,
    val finished: List<GoalUi> = emptyList(),
    val closed: List<GoalUi> = emptyList(),
) {
    val isEmpty: Boolean get() = finished.isEmpty() && closed.isEmpty()
}

@HiltViewModel
class PastChaptersViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val healthCalculator: GoalHealthCalculator,
) : ViewModel() {

    val state: StateFlow<PastChaptersUiState> = goalRepository.observeClosedSummaries()
        .map { summaries ->
            val goals = summaries.map { it.toUi(healthCalculator) }
            PastChaptersUiState(
                isLoading = false,
                // Finished and closed are kept apart: they are different kinds of
                // ending and lumping them together cheapens the first one.
                finished = goals.filter { it.status == GoalStatus.Completed },
                closed = goals.filter { it.status == GoalStatus.Archived },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = PastChaptersUiState(),
        )

    fun onReopen(goalId: Long) {
        viewModelScope.launch { goalRepository.restore(goalId) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * Everything the user finished or set down.
 *
 * The framing is a bookshelf, not a graveyard — hence "Past Chapters" and not
 * "Goal Cemetery". A closed goal is shown with the reason the user gave and how
 * far they actually got, both of which are genuinely useful later, and reopening
 * one is always a tap away.
 */
@Composable
fun PastChaptersScreen(
    onBack: () -> Unit,
    onOpenGoal: (Long) -> Unit,
    contentPadding: PaddingValues,
    viewModel: PastChaptersViewModel = hiltViewModel(),
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
                Eyebrow(text = "Closed, not deleted")
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = "Past chapters",
                    style = MilestoneTheme.type.displayMedium,
                    color = colors.ink,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = "Nothing here is gone. Any of it can be picked back up.",
                    style = MilestoneTheme.type.bodyMedium,
                    color = colors.inkMuted,
                )
            }

            if (state.isEmpty && !state.isLoading) {
                item(key = "empty") {
                    EmptyState(
                        glyph = "📖",
                        headline = "No past chapters.",
                        body = "Goals you finish or close will be kept here, " +
                            "so you can look back on them.",
                    )
                }
            }

            if (state.finished.isNotEmpty()) {
                item(key = "finishedHeader") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    SectionHeader(eyebrow = "Finished")
                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
                items(items = state.finished, key = { "done-" + it.id }) { goal ->
                    ChapterCard(
                        goal = goal,
                        onOpen = { onOpenGoal(goal.id) },
                        onReopen = { viewModel.onReopen(goal.id) },
                    )
                }
            }

            if (state.closed.isNotEmpty()) {
                item(key = "closedHeader") {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    SectionHeader(eyebrow = "Set down")
                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
                items(items = state.closed, key = { "closed-" + it.id }) { goal ->
                    ChapterCard(
                        goal = goal,
                        onOpen = { onOpenGoal(goal.id) },
                        onReopen = { viewModel.onReopen(goal.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun ChapterCard(goal: GoalUi, onOpen: () -> Unit, onReopen: () -> Unit) {
    val colors = MilestoneTheme.colors
    val isFinished = goal.status == GoalStatus.Completed

    MilestoneCard(
        modifier = Modifier.padding(bottom = Spacing.md),
        onClick = onOpen,
        containerColor = if (isFinished) colors.surface else colors.surfaceMuted,
        contentPadding = Spacing.xl,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = goal.emoji, style = MilestoneTheme.type.headlineMedium)
            Spacer(modifier = Modifier.width(Spacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = goal.title,
                    style = MilestoneTheme.type.titleLarge,
                    color = if (isFinished) colors.ink else colors.inkMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (isFinished) {
                        "Finished"
                    } else {
                        goal.closingReason?.label ?: "Closed"
                    },
                    style = MilestoneTheme.type.labelMedium,
                    color = if (isFinished) colors.success else colors.inkFaint,
                )
            }
            if (isFinished) {
                Text(text = "🏆", style = MilestoneTheme.type.headlineMedium)
            }
        }

        Spacer(modifier = Modifier.height(Spacing.lg))

        MilestoneProgressBar(
            progress = goal.progress,
            steps = goal.totalMilestones,
            color = if (isFinished) colors.success else goal.areaColor.copy(alpha = 0.55f),
            height = 6.dp,
            label = goal.title + ": " + goal.progressPercent + "% complete",
        )

        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = if (isFinished) {
                    "All " + goal.totalMilestones + " milestones"
                } else {
                    // How far they actually got is the useful detail here.
                    "Got to " + goal.completedMilestones + " of " + goal.totalMilestones
                },
                style = MilestoneTheme.type.labelSmall,
                color = colors.inkFaint,
                modifier = Modifier.weight(1f),
            )
            TextActionButton(
                text = if (isFinished) "Reopen" else "Start again",
                onClick = onReopen,
            )
        }
    }
}
