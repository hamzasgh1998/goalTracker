package com.milestone.app.ui.screens.goaldetail

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.ui.components.EmojiPicker
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.HealthBadge
import com.milestone.app.ui.components.IconActionButton
import com.milestone.app.ui.components.MilestoneProgressBar
import com.milestone.app.ui.components.MilestoneTextField
import com.milestone.app.ui.components.MilestoneTimeline
import com.milestone.app.ui.components.PrimaryButton
import com.milestone.app.ui.components.QuotedText
import com.milestone.app.ui.components.SecondaryButton
import com.milestone.app.ui.components.SelectableChip
import com.milestone.app.ui.components.Tag
import com.milestone.app.ui.components.TextActionButton
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.screens.creategoal.GOAL_EMOJI_CHOICES
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTextStyles
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

/**
 * One goal, in full: why it matters, how far it has come, and what is next.
 *
 * The screen is organised around the timeline rather than around controls. The
 * header states the facts, the "why" is quoted back in the user's own words, and
 * the milestone journey takes the rest of the page. Destructive and
 * status-changing actions sit at the very bottom, past the work — so that opening
 * a goal invites progress rather than administration.
 */
@Composable
fun GoalDetailScreen(
    onBack: () -> Unit,
    viewModel: GoalDetailViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MilestoneTheme.colors

    // A deleted goal has nowhere to show itself, so the screen steps aside rather
    // than explaining what just happened at the user's own request.
    LaunchedEffect(state.local.wasDeleted) {
        if (state.local.wasDeleted) onBack()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background),
    ) {
        val goal = state.goal

        if (state.notFound) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .statusBarsPadding(),
            ) {
                DetailTopBar(onBack = onBack, onEdit = null)
                Spacer(modifier = Modifier.height(Spacing.section))
                Text(
                    text = "This goal is no longer here.",
                    style = MilestoneTheme.type.headlineMedium,
                    color = colors.inkMuted,
                    modifier = Modifier.padding(horizontal = Spacing.gutter),
                )
            }
            return@Box
        }

        if (goal == null) return@Box

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .imePadding(),
        ) {
            DetailTopBar(
                onBack = onBack,
                // An archived goal's deadline and priority no longer mean
                // anything, so the editor isn't offered there.
                onEdit = if (goal.status == GoalStatus.Archived) {
                    null
                } else {
                    viewModel::onStartEditingGoal
                },
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Spacing.gutter),
            ) {
                GoalHeader(goal = goal)

                if (goal.why.isNotBlank()) {
                    Spacer(modifier = Modifier.height(Spacing.section))
                    Eyebrow(text = "Why")
                    Spacer(modifier = Modifier.height(Spacing.lg))
                    QuotedText(text = goal.why, accent = goal.areaColor)
                }

                Spacer(modifier = Modifier.height(Spacing.section))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Eyebrow(text = "Milestones", modifier = Modifier.weight(1f))
                    if (goal.status.allowsEditing && !state.local.isAddingMilestone) {
                        TextActionButton(
                            text = "Add",
                            leadingIcon = MilestoneIcons.Plus,
                            onClick = viewModel::onStartAddingMilestone,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.xl))

                if (state.timeline.isEmpty() && !state.local.isAddingMilestone) {
                    EmptyMilestones(
                        canEdit = goal.status.allowsEditing,
                        onAdd = viewModel::onStartAddingMilestone,
                    )
                } else {
                    MilestoneTimeline(
                        milestones = state.timeline,
                        showQuests = state.questsEnabled,
                        onToggle = viewModel::onToggleMilestone,
                        onToggleQuest = viewModel::onToggleQuest,
                        onLongPress = viewModel::onStartEditingMilestone,
                        onLongPressQuest = viewModel::onStartEditingQuest,
                        // Null on a finished or closed goal, which also removes
                        // the "add a step" affordance from the timeline.
                        onAddQuest = if (goal.status.allowsEditing) {
                            viewModel::onStartAddingQuest
                        } else {
                            null
                        },
                    )
                }

                AnimatedVisibility(visible = state.local.isAddingMilestone) {
                    Column(modifier = Modifier.padding(top = Spacing.lg)) {
                        MilestoneTextField(
                            value = state.local.newMilestoneText,
                            onValueChange = viewModel::onNewMilestoneTextChange,
                            placeholder = "What's the next step?",
                            imeAction = ImeAction.Done,
                            trailing = {
                                IconActionButton(
                                    icon = MilestoneIcons.Check,
                                    contentDescription = "Save milestone",
                                    tint = colors.accent,
                                    enabled = state.local.newMilestoneText.isNotBlank(),
                                    onClick = viewModel::onConfirmAddMilestone,
                                )
                            },
                        )
                        Spacer(modifier = Modifier.height(Spacing.sm))
                        TextActionButton(
                            text = "Done adding",
                            color = colors.inkFaint,
                            onClick = viewModel::onCancelAddMilestone,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(Spacing.section))

                GoalActions(
                    goal = goal,
                    onPause = viewModel::onPause,
                    onResume = viewModel::onResume,
                    onComplete = viewModel::onComplete,
                    onRestore = viewModel::onRestore,
                    onCloseChapter = viewModel::onStartClosingChapter,
                    onDeleteForever = viewModel::onStartDeletingForever,
                )

                Spacer(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .height(Spacing.section),
                )
            }
        }

        if (state.local.isClosingChapter) {
            CloseChapterSheet(
                goalTitle = goal.title,
                selectedReason = state.local.closingReason,
                note = state.local.closingNote,
                onReasonChange = viewModel::onClosingReasonChange,
                onNoteChange = viewModel::onClosingNoteChange,
                onConfirm = viewModel::onConfirmCloseChapter,
                onDismiss = viewModel::onCancelClosingChapter,
            )
        }

        state.local.edit?.let { draft ->
            EditGoalSheet(
                draft = draft,
                areas = state.areas,
                onTitleChange = viewModel::onEditTitleChange,
                onWhyChange = viewModel::onEditWhyChange,
                onEmojiChange = viewModel::onEditEmojiChange,
                onAreaChange = viewModel::onEditAreaChange,
                onTargetDateChange = viewModel::onEditTargetDateChange,
                onPriorityChange = viewModel::onEditPriorityChange,
                onSave = viewModel::onSaveGoalEdits,
                onDismiss = viewModel::onCancelEditingGoal,
            )
        }

        if (state.local.isDeletingForever) {
            DeleteForeverSheet(
                goalTitle = goal.title,
                milestoneCount = state.timeline.size,
                onConfirm = viewModel::onConfirmDeleteForever,
                onDismiss = viewModel::onCancelDeletingForever,
            )
        }

        state.local.milestoneEdit?.let { draft ->
            EditMilestoneSheet(
                draft = draft,
                onTextChange = viewModel::onEditingTextChange,
                onNoteChange = viewModel::onEditingNoteChange,
                onDueDateChange = viewModel::onEditingDueDateChange,
                onMove = viewModel::onMoveEditedMilestone,
                onConfirm = viewModel::onConfirmEditing,
                onDelete = { viewModel.onDeleteMilestone(draft.id) },
                onDismiss = viewModel::onCancelEditing,
            )
        }

        state.local.questEdit?.let { draft ->
            QuestSheet(
                draft = draft,
                onTitleChange = viewModel::onQuestTitleChange,
                onConfirm = viewModel::onConfirmQuest,
                onDelete = draft.questId?.let { id -> { viewModel.onDeleteQuest(id) } },
                onDismiss = viewModel::onCancelQuest,
            )
        }
    }
}

@Composable
private fun DetailTopBar(onBack: () -> Unit, onEdit: (() -> Unit)?) {
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
        if (onEdit != null) {
            IconActionButton(
                icon = MilestoneIcons.Edit,
                contentDescription = "Edit this goal",
                onClick = onEdit,
            )
        }
    }
}

@Composable
private fun GoalHeader(goal: GoalUi) {
    val colors = MilestoneTheme.colors

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = goal.emoji, style = MilestoneTheme.type.displaySmall)
            Spacer(modifier = Modifier.width(Spacing.md))
            Tag(
                text = goal.areaName,
                color = goal.areaColor,
                containerColor = goal.areaColor.copy(alpha = 0.12f),
            )
            if (goal.status == GoalStatus.Paused) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                Tag(text = "Paused")
            }
            if (goal.status == GoalStatus.Completed) {
                Spacer(modifier = Modifier.width(Spacing.sm))
                Tag(
                    text = "Complete",
                    color = colors.success,
                    containerColor = colors.success.copy(alpha = 0.12f),
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            text = goal.title,
            style = MilestoneTheme.type.displayLarge,
            color = colors.ink,
        )

        Spacer(modifier = Modifier.height(Spacing.xxl))

        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = "${goal.progressPercent}%",
                style = MilestoneTextStyles.Metric,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.width(Spacing.md))
            Text(
                text = goal.milestoneLabel,
                style = MilestoneTheme.type.bodyMedium,
                color = colors.inkMuted,
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        MilestoneProgressBar(
            progress = goal.progress,
            steps = goal.totalMilestones,
            color = goal.areaColor,
            label = "${goal.title}: ${goal.progressPercent}% complete",
        )

        Spacer(modifier = Modifier.height(Spacing.lg))

        Row(verticalAlignment = Alignment.CenterVertically) {
            HealthBadge(health = goal.health, text = goal.healthHeadline)
            goal.targetDate?.let { target ->
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = "by ${DATE_FORMAT.format(target)}",
                    style = MilestoneTheme.type.labelMedium,
                    color = colors.inkFaint,
                )
            }
        }
    }
}

@Composable
private fun EmptyMilestones(canEdit: Boolean, onAdd: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "Every big achievement starts somewhere.",
            style = MilestoneTheme.type.headlineMedium,
            color = MilestoneTheme.colors.ink,
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Break this into the first few steps. Four or five is plenty.",
            style = MilestoneTheme.type.bodyMedium,
            color = MilestoneTheme.colors.inkMuted,
        )
        if (canEdit) {
            Spacer(modifier = Modifier.height(Spacing.xl))
            SecondaryButton(
                text = "Add the first step",
                leadingIcon = MilestoneIcons.Plus,
                onClick = onAdd,
            )
        }
    }
}

/**
 * Status actions, deliberately placed below the milestones.
 *
 * Pausing is offered as plainly as finishing, with no warning styling: setting
 * something down on purpose is a legitimate outcome, and making it look like a
 * failure state is how an app ends up shaming the person using it.
 */
@Composable
private fun GoalActions(
    goal: GoalUi,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onComplete: () -> Unit,
    onRestore: () -> Unit,
    onCloseChapter: () -> Unit,
    onDeleteForever: () -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Eyebrow(text = "This goal")
        Spacer(modifier = Modifier.height(Spacing.lg))

        when (goal.status) {
            GoalStatus.Active -> {
                if (goal.totalMilestones > 0 && goal.progress >= 1f) {
                    PrimaryButton(
                        text = "Mark as complete",
                        trailingIcon = MilestoneIcons.Check,
                        onClick = onComplete,
                    )
                    Spacer(modifier = Modifier.height(Spacing.md))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    SecondaryButton(
                        text = "Pause",
                        leadingIcon = MilestoneIcons.Pause,
                        onClick = onPause,
                    )
                    SecondaryButton(
                        text = "Close chapter",
                        leadingIcon = MilestoneIcons.Book,
                        contentColor = MilestoneTheme.colors.inkMuted,
                        onClick = onCloseChapter,
                    )
                }
            }

            GoalStatus.Paused -> {
                PrimaryButton(
                    text = "Pick this back up",
                    trailingIcon = MilestoneIcons.Play,
                    onClick = onResume,
                )
                Spacer(modifier = Modifier.height(Spacing.md))
                SecondaryButton(
                    text = "Close chapter",
                    leadingIcon = MilestoneIcons.Book,
                    contentColor = MilestoneTheme.colors.inkMuted,
                    onClick = onCloseChapter,
                )
            }

            GoalStatus.Completed -> {
                Text(
                    text = "You finished this one.",
                    style = MilestoneTextStyles.Quote,
                    color = MilestoneTheme.colors.inkMuted,
                )
                Spacer(modifier = Modifier.height(Spacing.lg))
                SecondaryButton(text = "Reopen it", onClick = onRestore)
            }

            GoalStatus.Archived -> {
                goal.closingReason?.let { reason ->
                    Text(
                        text = "Closed: ${reason.label.lowercase()}.",
                        style = MilestoneTheme.type.bodyMedium,
                        color = MilestoneTheme.colors.inkMuted,
                    )
                    Spacer(modifier = Modifier.height(Spacing.lg))
                }
                SecondaryButton(
                    text = "Start this chapter again",
                    leadingIcon = MilestoneIcons.Play,
                    onClick = onRestore,
                )
                Spacer(modifier = Modifier.height(Spacing.xxl))
                // The only hard delete in the app, and only on a chapter the user
                // has already closed. Styled quietly: it is a last resort, not a
                // suggestion.
                TextActionButton(
                    text = "Delete permanently",
                    leadingIcon = MilestoneIcons.Trash,
                    color = MilestoneTheme.colors.inkFaint,
                    onClick = onDeleteForever,
                )
            }
        }
    }
}

/**
 * The closing flow: asks why, offers no judgement, and never deletes.
 *
 * The reason is the point — a goal closed for "wrong timing" is an invitation to
 * come back, which is information the user cannot reconstruct a year later.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun CloseChapterSheet(
    goalTitle: String,
    selectedReason: ClosingReason?,
    note: String,
    onReasonChange: (ClosingReason) -> Unit,
    onNoteChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
                text = "Why are you closing this chapter?",
                style = MilestoneTheme.type.headlineLarge,
                color = MilestoneTheme.colors.ink,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "\"$goalTitle\" will be kept in Past Chapters. " +
                    "Nothing is deleted, and you can pick it up again whenever.",
                style = MilestoneTheme.type.bodyMedium,
                color = MilestoneTheme.colors.inkMuted,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                ClosingReason.entries.forEach { reason ->
                    SelectableChip(
                        text = reason.label,
                        selected = selectedReason == reason,
                        onClick = { onReasonChange(reason) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            MilestoneTextField(
                value = note,
                onValueChange = onNoteChange,
                label = "Anything to remember?",
                placeholder = "Optional",
                maxLines = 3,
                minHeight = 80.dp,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            PrimaryButton(
                text = "Close this chapter",
                enabled = selectedReason != null,
                onClick = onConfirm,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextActionButton(
                    text = "Keep it open",
                    color = MilestoneTheme.colors.inkFaint,
                    onClick = onDismiss,
                )
            }
        }
    }
}

/**
 * Editing a goal's own fields: its name, its symbol, where it lives, and when.
 *
 * A sheet rather than a second pass through the creation wizard. The wizard's job
 * is to make someone think before committing; by the time they are fixing a typo
 * or moving a deadline, five full-screen questions would be an obstacle.
 *
 * Milestones are absent on purpose — they are edited on the timeline itself,
 * where their order is visible.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun EditGoalSheet(
    draft: GoalEditDraft,
    areas: List<LifeAreaEntity>,
    onTitleChange: (String) -> Unit,
    onWhyChange: (String) -> Unit,
    onEmojiChange: (String) -> Unit,
    onAreaChange: (Long) -> Unit,
    onTargetDateChange: (LocalDate?) -> Unit,
    onPriorityChange: (GoalPriority) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MilestoneTheme.colors
    var pickingDate by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = colors.surface,
        shape = MilestoneShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.section)
                .imePadding(),
        ) {
            Text(
                text = "Edit this goal",
                style = MilestoneTheme.type.headlineLarge,
                color = colors.ink,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Eyebrow(text = "Name")
            Spacer(modifier = Modifier.height(Spacing.md))
            MilestoneTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                placeholder = "What are you working towards?",
                textStyle = MilestoneTheme.type.headlineSmall,
                minHeight = 60.dp,
                supportingText = if (draft.canSave) null else "A goal needs a name.",
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Eyebrow(text = "Symbol")
            Spacer(modifier = Modifier.height(Spacing.md))
            EmojiPicker(
                choices = GOAL_EMOJI_CHOICES,
                selected = draft.emoji,
                onSelect = onEmojiChange,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Eyebrow(text = "Why it matters")
            Spacer(modifier = Modifier.height(Spacing.md))
            MilestoneTextField(
                value = draft.why,
                onValueChange = onWhyChange,
                placeholder = "The reason you started.",
                maxLines = 5,
                minHeight = 120.dp,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Eyebrow(text = "Part of your life")
            Spacer(modifier = Modifier.height(Spacing.md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                areas.forEach { area ->
                    SelectableChip(
                        text = area.name,
                        leading = area.emoji,
                        selected = draft.lifeAreaId == area.id,
                        accent = Color(area.colorArgb),
                        onClick = { onAreaChange(area.id) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Eyebrow(text = "Target date")
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = draft.targetDate?.let(DATE_FORMAT::format) ?: "No date set",
                    style = MilestoneTheme.type.bodyLarge,
                    color = if (draft.targetDate == null) colors.inkFaint else colors.ink,
                    modifier = Modifier.weight(1f),
                )
                TextActionButton(
                    text = if (draft.targetDate == null) "Pick a date" else "Change",
                    onClick = { pickingDate = true },
                )
                if (draft.targetDate != null) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    TextActionButton(
                        text = "Clear",
                        color = colors.inkFaint,
                        onClick = { onTargetDateChange(null) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Eyebrow(text = "How much attention")
            Spacer(modifier = Modifier.height(Spacing.md))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                GoalPriority.entries.forEach { priority ->
                    SelectableChip(
                        text = priority.label,
                        selected = draft.priority == priority,
                        onClick = { onPriorityChange(priority) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.heading))

            PrimaryButton(
                text = "Save changes",
                enabled = draft.canSave,
                trailingIcon = MilestoneIcons.Check,
                onClick = onSave,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextActionButton(
                    text = "Discard changes",
                    color = colors.inkFaint,
                    onClick = onDismiss,
                )
            }
        }
    }

    if (pickingDate) {
        GoalTargetDatePicker(
            initial = draft.targetDate,
            onPicked = { date ->
                onTargetDateChange(date)
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
}

/**
 * The calendar for a goal's target date.
 *
 * A real picker rather than the wizard's "in three months" presets: creation is
 * about committing to a rough horizon, whereas someone editing a date usually has
 * a specific day in mind — a race, a deadline, a birthday.
 *
 * Dates are read and written at UTC midnight, which is how the Material picker
 * reports its selection. Treating them as local time is the classic way a picked
 * date lands a day early west of Greenwich.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GoalTargetDatePicker(
    initial: LocalDate?,
    onPicked: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial
            ?.atStartOfDay(ZoneOffset.UTC)
            ?.toInstant()
            ?.toEpochMilli(),
    )

    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextActionButton(
                text = "Set date",
                enabled = pickerState.selectedDateMillis != null,
                onClick = {
                    pickerState.selectedDateMillis?.let { millis ->
                        onPicked(
                            Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate(),
                        )
                    }
                },
            )
        },
        dismissButton = {
            TextActionButton(
                text = "Cancel",
                color = MilestoneTheme.colors.inkFaint,
                onClick = onDismiss,
            )
        },
    ) {
        DatePicker(state = pickerState, showModeToggle = false)
    }
}

/**
 * Editing one step: its name, when it is due, and a note to self.
 *
 * The due date and the note were readable long before they were writable — the
 * timeline rendered both, but only the sample data could produce them. They are
 * optional and presented as such: a step with neither is the normal case, and
 * nothing here nags for them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditMilestoneSheet(
    draft: MilestoneEditDraft,
    onTextChange: (String) -> Unit,
    onNoteChange: (String) -> Unit,
    onDueDateChange: (LocalDate?) -> Unit,
    onMove: (Int) -> Unit,
    onConfirm: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MilestoneTheme.colors
    var pickingDate by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = MilestoneShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.section)
                .imePadding(),
        ) {
            Text(
                text = "Edit this step",
                style = MilestoneTheme.type.headlineMedium,
                color = colors.ink,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            MilestoneTextField(
                value = draft.title,
                onValueChange = onTextChange,
                placeholder = "Milestone",
                imeAction = ImeAction.Next,
                supportingText = if (draft.canSave) null else "A step needs a name.",
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Eyebrow(text = "Due")
            Spacer(modifier = Modifier.height(Spacing.md))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = draft.dueDate?.let(DATE_FORMAT::format) ?: "No date",
                    style = MilestoneTheme.type.bodyLarge,
                    color = if (draft.dueDate == null) colors.inkFaint else colors.ink,
                    modifier = Modifier.weight(1f),
                )
                TextActionButton(
                    text = if (draft.dueDate == null) "Pick a date" else "Change",
                    onClick = { pickingDate = true },
                )
                if (draft.dueDate != null) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    TextActionButton(
                        text = "Clear",
                        color = colors.inkFaint,
                        onClick = { onDueDateChange(null) },
                    )
                }
            }

            if (draft.total > 1) {
                Spacer(modifier = Modifier.height(Spacing.xxl))

                Eyebrow(text = "Position")
                Spacer(modifier = Modifier.height(Spacing.md))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Step " + (draft.index + 1) + " of " + draft.total,
                        style = MilestoneTheme.type.bodyLarge,
                        color = colors.ink,
                        modifier = Modifier.weight(1f),
                    )
                    IconActionButton(
                        icon = MilestoneIcons.ChevronUp,
                        contentDescription = "Move this step earlier",
                        enabled = draft.canMoveUp,
                        onClick = { onMove(-1) },
                    )
                    IconActionButton(
                        icon = MilestoneIcons.ChevronDown,
                        contentDescription = "Move this step later",
                        enabled = draft.canMoveDown,
                        onClick = { onMove(1) },
                    )
                }
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            Eyebrow(text = "Note")
            Spacer(modifier = Modifier.height(Spacing.md))
            MilestoneTextField(
                value = draft.note,
                onValueChange = onNoteChange,
                placeholder = "Anything worth remembering about this step.",
                maxLines = 4,
                minHeight = 96.dp,
            )

            Spacer(modifier = Modifier.height(Spacing.heading))

            PrimaryButton(text = "Save", enabled = draft.canSave, onClick = onConfirm)
            Spacer(modifier = Modifier.height(Spacing.md))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextActionButton(
                    text = "Delete this step",
                    color = colors.stalled,
                    onClick = onDelete,
                )
            }
        }
    }

    if (pickingDate) {
        GoalTargetDatePicker(
            initial = draft.dueDate,
            onPicked = { date ->
                onDueDateChange(date)
                pickingDate = false
            },
            onDismiss = { pickingDate = false },
        )
    }
}

/**
 * Adding or renaming a quest — the optional third tier.
 *
 * One sheet serves both, because they differ only in whether there is something
 * to delete. Adding keeps the sheet open with the field cleared: someone breaking
 * a milestone down is usually writing three or four steps in one sitting, and
 * dismissing after each would make that deliberately tedious.
 *
 * [onDelete] being null is what marks this as a new quest.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuestSheet(
    draft: QuestEditDraft,
    onTitleChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDelete: (() -> Unit)?,
    onDismiss: () -> Unit,
) {
    val colors = MilestoneTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = MilestoneShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.section)
                .imePadding(),
        ) {
            Text(
                text = if (draft.isNew) "A smaller step" else "Edit this step",
                style = MilestoneTheme.type.headlineMedium,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Part of \"" + draft.milestoneTitle + "\"",
                style = MilestoneTheme.type.bodyMedium,
                color = colors.inkMuted,
            )

            Spacer(modifier = Modifier.height(Spacing.xl))

            MilestoneTextField(
                value = draft.title,
                onValueChange = onTitleChange,
                placeholder = "What's involved?",
                imeAction = ImeAction.Done,
                trailing = if (draft.isNew) {
                    {
                        IconActionButton(
                            icon = MilestoneIcons.Plus,
                            contentDescription = "Add this step",
                            tint = colors.accent,
                            enabled = draft.canSave,
                            onClick = onConfirm,
                        )
                    }
                } else {
                    null
                },
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            PrimaryButton(
                text = if (draft.isNew) "Done adding" else "Save",
                enabled = draft.isNew || draft.canSave,
                onClick = if (draft.isNew) onDismiss else onConfirm,
            )

            if (onDelete != null) {
                Spacer(modifier = Modifier.height(Spacing.md))
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    TextActionButton(
                        text = "Remove this step",
                        color = colors.stalled,
                        onClick = onDelete,
                    )
                }
            }
        }
    }
}

/**
 * The one irreversible action in the app.
 *
 * Everything else about closing a goal is designed to be undoable — that is what
 * Past Chapters is for. This exists because "kept forever" should still be the
 * user's choice rather than the app's, but it says plainly what goes and makes
 * the safe option the easy one: confirming takes a deliberate tap on quiet
 * styling, while keeping it takes the prominent button.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeleteForeverSheet(
    goalTitle: String,
    milestoneCount: Int,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MilestoneTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = MilestoneShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.section),
        ) {
            Text(
                text = "Delete this permanently?",
                style = MilestoneTheme.type.headlineLarge,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "\"" + goalTitle + "\"" + milestoneSuffix(milestoneCount) +
                    " will be gone from this device. This cannot be undone, and " +
                    "the journey entries it created will no longer point anywhere.",
                style = MilestoneTheme.type.bodyMedium,
                color = colors.inkMuted,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            PrimaryButton(text = "Keep it", onClick = onDismiss)
            Spacer(modifier = Modifier.height(Spacing.md))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextActionButton(
                    text = "Delete permanently",
                    color = colors.stalled,
                    onClick = onConfirm,
                )
            }
        }
    }
}

/** ", and its 9 milestones" — stated, because that is the part people forget. */
private fun milestoneSuffix(count: Int): String = when (count) {
    0 -> ""
    1 -> ", and its milestone,"
    else -> ", and its $count milestones,"
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM")
