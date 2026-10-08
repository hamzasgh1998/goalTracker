package com.milestone.app.ui.screens.goaldetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.local.relation.GoalWithDetails
import com.milestone.app.data.local.relation.MilestoneWithQuests
import com.milestone.app.data.prefs.UserPreferences
import com.milestone.app.data.repository.GoalRepository
import com.milestone.app.data.repository.LifeAreaRepository
import com.milestone.app.domain.GoalHealthCalculator
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.ui.components.MilestoneNodeState
import com.milestone.app.ui.components.TimelineMilestone
import com.milestone.app.ui.components.TimelineQuest
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.model.toUi
import com.milestone.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * The goal's own fields, held while the user edits them.
 *
 * Kept separate from the loaded [GoalUi] so that an open editor is never
 * overwritten by the database flow re-emitting underneath it.
 */
data class GoalEditDraft(
    val title: String,
    val why: String,
    val emoji: String,
    val lifeAreaId: Long,
    val targetDate: LocalDate?,
    val priority: GoalPriority,
) {
    val canSave: Boolean get() = title.isNotBlank()
}

/**
 * One milestone's fields, held while the user edits them.
 *
 * The due date and note live here rather than being written as they are touched,
 * so that backing out of the sheet leaves the row exactly as it was.
 */
data class MilestoneEditDraft(
    val id: Long,
    val title: String,
    val note: String,
    val dueDate: LocalDate?,
    /** Where this step sits in the journey, for the move controls. */
    val index: Int,
    val total: Int,
) {
    val canSave: Boolean get() = title.isNotBlank()
    val canMoveUp: Boolean get() = index > 0
    val canMoveDown: Boolean get() = index < total - 1
}

/**
 * A quest being added or renamed.
 *
 * One draft serves both: adding and editing differ only in whether [questId] is
 * known, which is also the only thing that decides if the sheet offers a delete.
 */
data class QuestEditDraft(
    val milestoneId: Long,
    val questId: Long?,
    val title: String,
    /** The step this sits under, quoted in the sheet for context. */
    val milestoneTitle: String,
) {
    val isNew: Boolean get() = questId == null
    val canSave: Boolean get() = title.isNotBlank()
}

/** Transient UI concerns the database knows nothing about. */
data class GoalDetailLocalState(
    val isAddingMilestone: Boolean = false,
    val newMilestoneText: String = "",
    val isClosingChapter: Boolean = false,
    val closingReason: ClosingReason? = null,
    val closingNote: String = "",
    /** Non-null while the edit-milestone sheet is open. */
    val milestoneEdit: MilestoneEditDraft? = null,
    /** Non-null while the quest sheet is open, for a new or an existing one. */
    val questEdit: QuestEditDraft? = null,
    /** Non-null while the edit-goal sheet is open. */
    val edit: GoalEditDraft? = null,
    /** True while the permanent-delete confirmation is showing. */
    val isDeletingForever: Boolean = false,
    /**
     * Set once the goal has actually been deleted, so the screen can leave.
     *
     * Distinct from `notFound`: that means "it was gone when we looked", which
     * deserves an explanation on screen. This means "you just deleted it", which
     * deserves a quiet return to where you came from.
     */
    val wasDeleted: Boolean = false,
)

data class GoalDetailUiState(
    val isLoading: Boolean = true,
    /** True once the goal is known to be gone, e.g. deleted from another screen. */
    val notFound: Boolean = false,
    val goal: GoalUi? = null,
    val timeline: List<TimelineMilestone> = emptyList(),
    val questsEnabled: Boolean = true,
    /** Choices for the edit sheet's life-area picker. */
    val areas: List<LifeAreaEntity> = emptyList(),
    val local: GoalDetailLocalState = GoalDetailLocalState(),
)

@HiltViewModel
class GoalDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val goalRepository: GoalRepository,
    private val lifeAreaRepository: LifeAreaRepository,
    private val preferences: UserPreferences,
    private val healthCalculator: GoalHealthCalculator,
    private val clock: AppClock,
) : ViewModel() {

    private val goalId: Long = checkNotNull(savedStateHandle.get<String>(Routes.GOAL_ID_ARG))
        .toLong()

    private val local = MutableStateFlow(GoalDetailLocalState())

    val state: StateFlow<GoalDetailUiState> = combine(
        goalRepository.observeDetails(goalId),
        preferences.settings,
        lifeAreaRepository.observeAll(),
        local,
    ) { details, settings, areas, localState ->
        if (details == null) {
            GoalDetailUiState(isLoading = false, notFound = true, local = localState)
        } else {
            GoalDetailUiState(
                isLoading = false,
                goal = details.toUi(healthCalculator),
                timeline = buildTimeline(details),
                questsEnabled = settings.questsEnabled,
                // Hidden areas are excluded, except the one this goal is already
                // in: a goal filed under an area the user later stopped tracking
                // must still show its own answer as selected.
                areas = areas.filter {
                    it.isTracked || it.id == details.goal.lifeAreaId
                },
                local = localState,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = GoalDetailUiState(),
    )

    // ------------------------------------------------------------- milestones

    fun onToggleMilestone(milestoneId: Long) {
        viewModelScope.launch {
            val current = state.value.timeline.firstOrNull { it.id == milestoneId }
            goalRepository.setMilestoneCompleted(
                milestoneId = milestoneId,
                completed = current?.state != MilestoneNodeState.Done,
            )
        }
    }

    fun onToggleQuest(questId: Long) {
        viewModelScope.launch {
            val completed = state.value.timeline
                .flatMap { it.quests }
                .firstOrNull { it.id == questId }
                ?.isCompleted ?: false
            goalRepository.setQuestCompleted(questId, !completed)
        }
    }

    fun onStartAddingMilestone() =
        local.update { it.copy(isAddingMilestone = true, newMilestoneText = "") }

    fun onNewMilestoneTextChange(value: String) =
        local.update { it.copy(newMilestoneText = value) }

    fun onCancelAddMilestone() =
        local.update { it.copy(isAddingMilestone = false, newMilestoneText = "") }

    fun onConfirmAddMilestone() {
        val text = local.value.newMilestoneText.trim()
        if (text.isEmpty()) return
        viewModelScope.launch {
            goalRepository.addMilestone(goalId = goalId, title = text)
            // Stay in add mode: adding several steps in a row is the common case
            // when a user is fleshing out a goal.
            local.update { it.copy(newMilestoneText = "") }
        }
    }

    /**
     * Opens the milestone editor, seeded from the stored row.
     *
     * Read from the database rather than from the rendered timeline: the timeline
     * carries a *formatted* due date ("Due 14 Oct") and no raw one, and a form
     * seeded from display text would round-trip a lossy copy back into the row.
     */
    fun onStartEditingMilestone(milestoneId: Long) {
        viewModelScope.launch {
            val milestone = goalRepository.getMilestone(milestoneId) ?: return@launch
            val timeline = state.value.timeline
            local.update {
                it.copy(
                    milestoneEdit = MilestoneEditDraft(
                        id = milestone.id,
                        title = milestone.title,
                        note = milestone.note.orEmpty(),
                        dueDate = milestone.dueDate,
                        index = timeline.indexOfFirst { row -> row.id == milestoneId },
                        total = timeline.size,
                    ),
                )
            }
        }
    }

    /**
     * Moves the step being edited one place up or down.
     *
     * Buttons rather than a drag gesture, for three reasons: long-press already
     * opens this sheet, so a drag would have to displace it; the timeline draws
     * its own connectors on a canvas, which a reordering drag would have to
     * reproduce mid-gesture; and a drag is unusable with a screen reader, whereas
     * two buttons are not. The creation wizard orders its steps the same way.
     */
    fun onMoveEditedMilestone(delta: Int) {
        val draft = local.value.milestoneEdit ?: return
        val target = draft.index + delta
        if (target !in 0 until draft.total) return

        // The draft index moves with it so the buttons stay correct without
        // waiting for the database flow to come back round.
        local.update { it.copy(milestoneEdit = draft.copy(index = target)) }
        onMoveMilestone(draft.index, target)
    }

    /** Applies one field to the open milestone draft; a no-op when closed. */
    private fun updateMilestoneDraft(transform: (MilestoneEditDraft) -> MilestoneEditDraft) =
        local.update { current ->
            current.copy(milestoneEdit = current.milestoneEdit?.let(transform))
        }

    fun onEditingTextChange(value: String) = updateMilestoneDraft { it.copy(title = value) }

    fun onEditingNoteChange(value: String) = updateMilestoneDraft { it.copy(note = value) }

    fun onEditingDueDateChange(date: LocalDate?) = updateMilestoneDraft { it.copy(dueDate = date) }

    fun onCancelEditing() = local.update { it.copy(milestoneEdit = null) }

    fun onConfirmEditing() {
        val draft = local.value.milestoneEdit ?: return
        if (!draft.canSave) return

        viewModelScope.launch {
            goalRepository.editMilestone(
                milestoneId = draft.id,
                title = draft.title,
                note = draft.note,
                dueDate = draft.dueDate,
            )
            local.update { it.copy(milestoneEdit = null) }
        }
    }

    fun onDeleteMilestone(milestoneId: Long) {
        viewModelScope.launch {
            goalRepository.deleteMilestone(milestoneId)
            local.update { it.copy(milestoneEdit = null) }
        }
    }

    // ----------------------------------------------------------------- quests

    /**
     * Opens the quest sheet for a new step under [milestoneId].
     *
     * Quests can only be added to the current milestone, because that is the only
     * one the timeline shows them under — offering to add sub-steps to a row whose
     * sub-steps are hidden would be a trap.
     */
    fun onStartAddingQuest(milestoneId: Long) {
        val milestone = state.value.timeline.firstOrNull { it.id == milestoneId } ?: return
        local.update {
            it.copy(
                questEdit = QuestEditDraft(
                    milestoneId = milestoneId,
                    questId = null,
                    title = "",
                    milestoneTitle = milestone.title,
                ),
            )
        }
    }

    fun onStartEditingQuest(questId: Long) {
        val milestone = state.value.timeline
            .firstOrNull { row -> row.quests.any { it.id == questId } } ?: return
        val quest = milestone.quests.first { it.id == questId }

        local.update {
            it.copy(
                questEdit = QuestEditDraft(
                    milestoneId = milestone.id,
                    questId = questId,
                    title = quest.title,
                    milestoneTitle = milestone.title,
                ),
            )
        }
    }

    fun onQuestTitleChange(value: String) = local.update { current ->
        current.copy(questEdit = current.questEdit?.copy(title = value))
    }

    fun onCancelQuest() = local.update { it.copy(questEdit = null) }

    fun onConfirmQuest() {
        val draft = local.value.questEdit ?: return
        if (!draft.canSave) return

        viewModelScope.launch {
            if (draft.questId == null) {
                goalRepository.addQuest(draft.milestoneId, draft.title)
                // Stay open with the field cleared: breaking a milestone down
                // usually means adding three or four steps in one sitting.
                local.update { current ->
                    current.copy(questEdit = current.questEdit?.copy(title = ""))
                }
            } else {
                goalRepository.updateQuestTitle(draft.questId, draft.title)
                local.update { it.copy(questEdit = null) }
            }
        }
    }

    fun onDeleteQuest(questId: Long) {
        viewModelScope.launch {
            goalRepository.deleteQuest(questId)
            local.update { it.copy(questEdit = null) }
        }
    }

    fun onMoveMilestone(from: Int, to: Int) {
        val ids = state.value.timeline.map { it.id }.toMutableList()
        if (from !in ids.indices || to !in ids.indices) return
        ids.add(to, ids.removeAt(from))
        viewModelScope.launch { goalRepository.reorderMilestones(ids) }
    }

    // -------------------------------------------------------------- goal edits

    /**
     * Opens the editor, seeded from the goal as it currently stands.
     *
     * Seeded once, at open: from here on the draft is the single source of truth
     * for what the fields show, which is what stops a background re-emission
     * wiping out half-typed text.
     */
    fun onStartEditingGoal() {
        val goal = state.value.goal ?: return
        local.update {
            it.copy(
                edit = GoalEditDraft(
                    title = goal.title,
                    why = goal.why,
                    emoji = goal.emoji,
                    lifeAreaId = goal.lifeAreaId,
                    targetDate = goal.targetDate,
                    priority = goal.priority,
                ),
            )
        }
    }

    fun onCancelEditingGoal() = local.update { it.copy(edit = null) }

    /** Applies one field to the open draft; a no-op when the sheet is closed. */
    private fun updateDraft(transform: (GoalEditDraft) -> GoalEditDraft) =
        local.update { current ->
            current.copy(edit = current.edit?.let(transform))
        }

    fun onEditTitleChange(value: String) = updateDraft { it.copy(title = value) }

    fun onEditWhyChange(value: String) = updateDraft { it.copy(why = value) }

    fun onEditEmojiChange(value: String) = updateDraft { it.copy(emoji = value) }

    fun onEditAreaChange(areaId: Long) = updateDraft { it.copy(lifeAreaId = areaId) }

    fun onEditTargetDateChange(date: LocalDate?) = updateDraft { it.copy(targetDate = date) }

    fun onEditPriorityChange(priority: GoalPriority) = updateDraft { it.copy(priority = priority) }

    fun onSaveGoalEdits() {
        val draft = local.value.edit ?: return
        if (!draft.canSave) return

        viewModelScope.launch {
            goalRepository.editGoal(
                goalId = goalId,
                title = draft.title,
                why = draft.why,
                emoji = draft.emoji,
                lifeAreaId = draft.lifeAreaId,
                targetDate = draft.targetDate,
                priority = draft.priority,
            )
            local.update { it.copy(edit = null) }
        }
    }

    // ------------------------------------------------------------ goal status

    fun onPause() {
        viewModelScope.launch { goalRepository.pause(goalId) }
    }

    fun onResume() {
        viewModelScope.launch { goalRepository.resume(goalId) }
    }

    fun onComplete() {
        viewModelScope.launch { goalRepository.complete(goalId) }
    }

    fun onRestore() {
        viewModelScope.launch { goalRepository.restore(goalId) }
    }

    fun onStartDeletingForever() = local.update { it.copy(isDeletingForever = true) }

    fun onCancelDeletingForever() = local.update { it.copy(isDeletingForever = false) }

    /**
     * Hard-deletes the goal and everything under it.
     *
     * Only offered on a chapter the user has already closed, and only behind an
     * explicit confirmation that says what will be lost — everywhere else in the
     * app, "finished with this" means archiving.
     */
    fun onConfirmDeleteForever() {
        viewModelScope.launch {
            goalRepository.deleteForever(goalId)
            local.update { it.copy(isDeletingForever = false, wasDeleted = true) }
        }
    }

    fun onStartClosingChapter() = local.update { it.copy(isClosingChapter = true) }

    fun onCancelClosingChapter() = local.update {
        it.copy(isClosingChapter = false, closingReason = null, closingNote = "")
    }

    fun onClosingReasonChange(reason: ClosingReason) =
        local.update { it.copy(closingReason = reason) }

    fun onClosingNoteChange(value: String) = local.update { it.copy(closingNote = value) }

    fun onConfirmCloseChapter() {
        val reason = local.value.closingReason ?: return
        val note = local.value.closingNote
        viewModelScope.launch {
            goalRepository.archive(goalId, reason, note)
            local.update {
                it.copy(isClosingChapter = false, closingReason = null, closingNote = "")
            }
        }
    }

    /**
     * Turns the loaded tree into timeline rows.
     *
     * Exactly one row is [MilestoneNodeState.Current] — the lowest-ordered
     * incomplete one — which is what lets the user glance at a long goal and see
     * where they are. A fully completed goal has no current node, by design:
     * there is nothing left to point at.
     */
    private fun buildTimeline(details: GoalWithDetails): List<TimelineMilestone> {
        val sorted = details.sortedMilestones
        val currentId = sorted.firstOrNull { !it.milestone.isCompleted }?.milestone?.id

        return sorted.mapIndexed { index, entry ->
            val milestone = entry.milestone
            TimelineMilestone(
                id = milestone.id,
                title = milestone.title,
                state = when {
                    milestone.isCompleted -> MilestoneNodeState.Done
                    milestone.id == currentId -> MilestoneNodeState.Current
                    else -> MilestoneNodeState.Upcoming
                },
                note = milestone.note,
                metadata = metadataFor(entry),
                isFinal = index == sorted.lastIndex && sorted.size > 1,
                quests = entry.sortedQuests.map { quest ->
                    TimelineQuest(
                        id = quest.id,
                        title = quest.title,
                        isCompleted = quest.isCompleted,
                    )
                },
            )
        }
    }

    /** "Done 3 Oct" for finished steps, "Due 14 Oct" for the ones ahead. */
    private fun metadataFor(entry: MilestoneWithQuests): String? {
        val milestone = entry.milestone
        milestone.completedAt?.let { completed ->
            return "Done ${DATE_FORMAT.format(completed.atZone(clock.zone()).toLocalDate())}"
        }
        milestone.dueDate?.let { due ->
            return "Due ${DATE_FORMAT.format(due)}"
        }
        val openQuests = entry.quests.count { !it.isCompleted }
        return when (openQuests) {
            0 -> null
            1 -> "1 step to go"
            else -> "$openQuests steps to go"
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
        val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMM")
    }
}

/** True when the goal can still be worked on. */
val GoalStatus.allowsEditing: Boolean get() = this == GoalStatus.Active || this == GoalStatus.Paused
