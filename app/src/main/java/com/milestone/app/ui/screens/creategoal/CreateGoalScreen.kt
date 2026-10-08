package com.milestone.app.ui.screens.creategoal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.domain.MilestoneTemplate
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.ui.components.EmojiPicker
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.Hairline
import com.milestone.app.ui.components.IconActionButton
import com.milestone.app.ui.components.MilestoneTextField
import com.milestone.app.ui.components.PrimaryButton
import com.milestone.app.ui.components.SelectableChip
import com.milestone.app.ui.components.SunkenPanel
import com.milestone.app.ui.components.TextActionButton
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.theme.MilestoneMotion
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import com.milestone.app.ui.theme.durationScale
import java.time.format.DateTimeFormatter

/**
 * Goal creation as a guided sequence rather than a form.
 *
 * One question per screen, each set at display size with its own helper line. The
 * cost is five taps instead of one long scroll; the benefit is that the "why"
 * question gets answered thoughtfully instead of being skipped, which is the
 * difference between a goal the user returns to and a row in a list.
 */
@Composable
fun CreateGoalScreen(
    onClose: () -> Unit,
    onCreated: (Long) -> Unit,
    viewModel: CreateGoalViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MilestoneTheme.colors

    LaunchedEffect(state.createdGoalId) {
        state.createdGoalId?.let(onCreated)
    }

    // Hoisted out of the transitionSpec below: that lambda is not composable, so
    // it cannot read the reduce-motion preference itself.
    val enterMillis = durationScale(MilestoneMotion.DurationStandard)
    val exitMillis = durationScale(MilestoneMotion.DurationQuick)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .systemBarsPadding()
            .imePadding(),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.sm, vertical = Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconActionButton(
                icon = MilestoneIcons.Close,
                contentDescription = "Cancel",
                onClick = onClose,
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
            ) {
                CreateGoalStep.entries.forEach { step ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(
                                color = if (step.ordinal <= state.step.ordinal) {
                                    colors.accent
                                } else {
                                    colors.hairline
                                },
                                shape = MilestoneTheme.shapes.pill,
                            ),
                    )
                }
            }
            Spacer(modifier = Modifier.width(Spacing.xxl))
        }

        AnimatedContent(
            targetState = state.step,
            transitionSpec = {
                // Steps slide a sixth of the width rather than the whole screen:
                // enough to show direction, not enough to feel like a page turn.
                val offset = if (targetState.ordinal > initialState.ordinal) 1 else -1
                (
                    slideInHorizontally(tween(enterMillis)) { full -> offset * full / 6 } +
                        fadeIn(tween(enterMillis))
                    ) togetherWith (
                    slideOutHorizontally(tween(exitMillis)) { full -> -offset * full / 6 } +
                        fadeOut(tween(exitMillis))
                    )
            },
            label = "createGoalStep",
            modifier = Modifier.weight(1f),
        ) { step ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter),
            ) {
                Spacer(modifier = Modifier.height(Spacing.xl))

                Text(
                    text = step.question,
                    style = MilestoneTheme.type.displaySmall,
                    color = colors.ink,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                Text(
                    text = step.helper,
                    style = MilestoneTheme.type.bodyMedium,
                    color = colors.inkMuted,
                )

                Spacer(modifier = Modifier.height(Spacing.heading))

                when (step) {
                    CreateGoalStep.Title -> TitleStep(state = state, viewModel = viewModel)
                    CreateGoalStep.Why -> WhyStep(state = state, viewModel = viewModel)
                    CreateGoalStep.Area -> AreaStep(state = state, viewModel = viewModel)
                    CreateGoalStep.Timing -> TimingStep(state = state, viewModel = viewModel)
                    CreateGoalStep.Milestones -> MilestonesStep(
                        state = state,
                        viewModel = viewModel,
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.section))
            }
        }

        Column(modifier = Modifier.padding(horizontal = Spacing.gutter)) {
            PrimaryButton(
                text = if (state.isLastStep) "Create this goal" else "Continue",
                enabled = state.canAdvance && !state.isSaving,
                trailingIcon = if (state.isLastStep) {
                    MilestoneIcons.Check
                } else {
                    MilestoneIcons.ArrowRight
                },
                onClick = viewModel::onNext,
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Spacing.minTouchTarget),
                contentAlignment = Alignment.Center,
            ) {
                if (state.step.ordinal > 0) {
                    TextActionButton(
                        text = "Back",
                        color = colors.inkFaint,
                        onClick = viewModel::onBack,
                    )
                }
            }
        }
    }
}

@Composable
private fun TitleStep(state: CreateGoalUiState, viewModel: CreateGoalViewModel) {
    MilestoneTextField(
        value = state.title,
        onValueChange = viewModel::onTitleChange,
        placeholder = "Run my first 10K",
        textStyle = MilestoneTheme.type.headlineMedium,
        minHeight = 64.dp,
        imeAction = ImeAction.Next,
    )

    Spacer(modifier = Modifier.height(Spacing.xxl))

    Eyebrow(text = "Pick a symbol")
    Spacer(modifier = Modifier.height(Spacing.md))

    EmojiPicker(
        choices = GOAL_EMOJI_CHOICES,
        selected = state.emoji,
        onSelect = viewModel::onEmojiChange,
    )
}

@Composable
private fun WhyStep(state: CreateGoalUiState, viewModel: CreateGoalViewModel) {
    MilestoneTextField(
        value = state.why,
        onValueChange = viewModel::onWhyChange,
        placeholder = "Because I want to prove I can finish something difficult.",
        textStyle = MilestoneTheme.type.bodyLarge,
        maxLines = 6,
        minHeight = 140.dp,
        supportingText = "You can leave this blank, but it is the most useful " +
            "thing on the whole screen.",
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AreaStep(state: CreateGoalUiState, viewModel: CreateGoalViewModel) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        state.areas.forEach { area ->
            SelectableChip(
                text = area.name,
                leading = area.emoji,
                selected = state.selectedAreaId == area.id,
                accent = Color(area.colorArgb),
                onClick = { viewModel.onAreaSelect(area.id) },
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TimingStep(state: CreateGoalUiState, viewModel: CreateGoalViewModel) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        TargetChoice.entries.forEach { choice ->
            // Hide "end of season" when there is no season with an end date to
            // anchor it to, rather than quietly substituting another date.
            val available = choice != TargetChoice.EndOfSeason ||
                state.activeSeason?.endDate != null
            if (available) {
                SelectableChip(
                    text = choice.label,
                    selected = state.targetChoice == choice,
                    onClick = { viewModel.onTargetChoiceChange(choice) },
                )
            }
        }
    }

    state.activeSeason?.endDate?.let { end ->
        if (state.targetChoice == TargetChoice.EndOfSeason) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "That's ${end.format(DATE_FORMAT)}.",
                style = MilestoneTheme.type.labelMedium,
                color = MilestoneTheme.colors.inkFaint,
            )
        }
    }

    Spacer(modifier = Modifier.height(Spacing.section))

    Eyebrow(text = "How much attention this season?")
    Spacer(modifier = Modifier.height(Spacing.md))

    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        GoalPriority.entries.forEach { priority ->
            SelectableChip(
                text = priority.label,
                selected = state.priority == priority,
                onClick = { viewModel.onPriorityChange(priority) },
            )
        }
    }

    Spacer(modifier = Modifier.height(Spacing.md))
    Text(
        text = "Primary goals are the few you actually push on. " +
            "Keep it to three and this app will work the way it's meant to.",
        style = MilestoneTheme.type.bodySmall,
        color = MilestoneTheme.colors.inkFaint,
    )
}

@Composable
private fun MilestonesStep(state: CreateGoalUiState, viewModel: CreateGoalViewModel) {
    val colors = MilestoneTheme.colors

    MilestoneTextField(
        value = state.milestoneInput,
        onValueChange = viewModel::onMilestoneInputChange,
        // Neutral: a topical example here contradicts whatever goal was just named.
        placeholder = "Add a step",
        imeAction = ImeAction.Done,
        trailing = {
            IconActionButton(
                icon = MilestoneIcons.Plus,
                contentDescription = "Add milestone",
                tint = colors.accent,
                enabled = state.milestoneInput.isNotBlank(),
                onClick = viewModel::onAddMilestone,
            )
        },
    )

    if (state.milestones.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Spacing.xl))
        state.milestones.forEachIndexed { index, milestone ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "${index + 1}",
                    style = MilestoneTheme.type.labelMedium,
                    color = colors.inkFaint,
                    modifier = Modifier.width(24.dp),
                )
                Text(
                    text = milestone,
                    style = MilestoneTheme.type.bodyLarge,
                    color = colors.ink,
                    modifier = Modifier.weight(1f),
                )
                IconActionButton(
                    icon = MilestoneIcons.ChevronDown,
                    contentDescription = "Move down",
                    enabled = index < state.milestones.lastIndex,
                    onClick = { viewModel.onMoveMilestone(index, index + 1) },
                )
                IconActionButton(
                    icon = MilestoneIcons.Close,
                    contentDescription = "Remove ${milestone}",
                    onClick = { viewModel.onRemoveMilestone(index) },
                )
            }
            if (index < state.milestones.lastIndex) Hairline()
        }
    }

    if (state.templates.isNotEmpty()) {
        Spacer(modifier = Modifier.height(Spacing.section))
        Eyebrow(text = "Need a starting point?")
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Examples, not advice. Add them and change whatever doesn't fit.",
            style = MilestoneTheme.type.bodySmall,
            color = colors.inkFaint,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))

        state.templates.forEach { template ->
            TemplateCard(
                template = template,
                onApply = { viewModel.onApplyTemplate(template) },
            )
            Spacer(modifier = Modifier.height(Spacing.md))
        }
    }
}

@Composable
private fun TemplateCard(template: MilestoneTemplate, onApply: () -> Unit) {
    SunkenPanel {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = template.glyph, style = MilestoneTheme.type.headlineSmall)
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(
                text = template.label,
                style = MilestoneTheme.type.titleMedium,
                color = MilestoneTheme.colors.ink,
                modifier = Modifier.weight(1f),
            )
            TextActionButton(text = "Add all", onClick = onApply)
        }

        Spacer(modifier = Modifier.height(Spacing.sm))

        template.milestones.forEach { step ->
            Text(
                text = "·  $step",
                style = MilestoneTheme.type.bodySmall,
                color = MilestoneTheme.colors.inkMuted,
                modifier = Modifier.padding(vertical = 2.dp),
            )
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
