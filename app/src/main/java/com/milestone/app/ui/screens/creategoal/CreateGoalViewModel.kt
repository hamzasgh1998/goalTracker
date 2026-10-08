package com.milestone.app.ui.screens.creategoal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.local.entity.LifeSeasonEntity
import com.milestone.app.data.repository.GoalDraft
import com.milestone.app.data.repository.GoalRepository
import com.milestone.app.data.repository.LifeAreaRepository
import com.milestone.app.data.repository.SeasonRepository
import com.milestone.app.domain.MilestoneTemplate
import com.milestone.app.domain.MilestoneTemplates
import com.milestone.app.domain.model.GoalPriority
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/**
 * The five questions, in order.
 *
 * Creating a goal is a sequence of single questions rather than one long form,
 * because the answers are supposed to require a moment's thought — particularly
 * [Why], which is the one the user will come back and read in six weeks.
 */
enum class CreateGoalStep(val question: String, val helper: String) {
    Title(
        question = "What do you want to accomplish?",
        helper = "Name it the way you'd say it out loud.",
    ),
    Why(
        question = "Why does this matter to you?",
        helper = "This is the part you'll want to read again on a bad week.",
    ),
    Area(
        question = "Which part of your life is this?",
        helper = "It helps to see where your attention actually goes.",
    ),
    Timing(
        question = "When would you like to get there?",
        helper = "A date is useful, not compulsory.",
    ),
    Milestones(
        question = "Break it into steps.",
        helper = "Four or five is usually right. You can change them any time.",
    ),
}

/** The deadline presets offered instead of a calendar grid. */
enum class TargetChoice(val label: String) {
    OneMonth("In a month"),
    ThreeMonths("In three months"),
    SixMonths("In six months"),
    EndOfSeason("End of this season"),
    NoDate("No date for now"),
}

/** Stand-in emoji until the user picks one. */
const val DEFAULT_GOAL_EMOJI = "🎯"

/** The emoji palette offered during creation. Deliberately small. */
val GOAL_EMOJI_CHOICES = listOf(
    "🎯", "🚀", "🏃", "💪", "📚", "🎸", "💰", "🧠",
    "❤️", "🎨", "🌎", "🧘", "📷", "✍️", "🏔", "🌱",
)

data class CreateGoalUiState(
    val step: CreateGoalStep = CreateGoalStep.Title,
    val title: String = "",
    val emoji: String = DEFAULT_GOAL_EMOJI,
    val why: String = "",
    val areas: List<LifeAreaEntity> = emptyList(),
    val selectedAreaId: Long? = null,
    val targetChoice: TargetChoice = TargetChoice.ThreeMonths,
    val priority: GoalPriority = GoalPriority.Primary,
    val milestones: List<String> = emptyList(),
    val milestoneInput: String = "",
    val templates: List<MilestoneTemplate> = emptyList(),
    val activeSeason: LifeSeasonEntity? = null,
    val isSaving: Boolean = false,
    val createdGoalId: Long? = null,
) {
    val canAdvance: Boolean
        get() = when (step) {
            CreateGoalStep.Title -> title.isNotBlank()
            CreateGoalStep.Why -> true
            CreateGoalStep.Area -> selectedAreaId != null
            CreateGoalStep.Timing -> true
            // A goal with no milestones is allowed; the detail screen invites
            // adding them later rather than blocking creation here.
            CreateGoalStep.Milestones -> true
        }

    val isLastStep: Boolean get() = step == CreateGoalStep.Milestones
}

@HiltViewModel
class CreateGoalViewModel @Inject constructor(
    private val goalRepository: GoalRepository,
    private val lifeAreaRepository: LifeAreaRepository,
    private val seasonRepository: SeasonRepository,
    private val clock: AppClock,
) : ViewModel() {

    private val _state = MutableStateFlow(CreateGoalUiState())
    val state: StateFlow<CreateGoalUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val season = seasonRepository.getActive()
            _state.update { it.copy(activeSeason = season) }

            lifeAreaRepository.observeTracked().collect { areas ->
                _state.update { current ->
                    current.copy(
                        areas = areas,
                        // Pre-select when there is only one plausible answer.
                        selectedAreaId = current.selectedAreaId
                            ?: areas.singleOrNull()?.id,
                    )
                }
            }
        }
    }

    fun onTitleChange(value: String) = _state.update { it.copy(title = value) }

    fun onEmojiChange(value: String) = _state.update { it.copy(emoji = value) }

    fun onWhyChange(value: String) = _state.update { it.copy(why = value) }

    fun onAreaSelect(areaId: Long) = _state.update { it.copy(selectedAreaId = areaId) }

    fun onTargetChoiceChange(choice: TargetChoice) =
        _state.update { it.copy(targetChoice = choice) }

    fun onPriorityChange(priority: GoalPriority) = _state.update { it.copy(priority = priority) }

    fun onMilestoneInputChange(value: String) = _state.update { it.copy(milestoneInput = value) }

    /** Commits whatever is in the milestone input box as a new step. */
    fun onAddMilestone() = _state.update { current ->
        val text = current.milestoneInput.trim()
        if (text.isEmpty()) {
            current
        } else {
            current.copy(milestones = current.milestones + text, milestoneInput = "")
        }
    }

    fun onRemoveMilestone(index: Int) = _state.update { current ->
        current.copy(milestones = current.milestones.filterIndexed { i, _ -> i != index })
    }

    fun onMoveMilestone(from: Int, to: Int) = _state.update { current ->
        val list = current.milestones.toMutableList()
        if (from !in list.indices || to !in list.indices) return@update current
        list.add(to, list.removeAt(from))
        current.copy(milestones = list)
    }

    /** Appends a template's steps, skipping any the user already has. */
    fun onApplyTemplate(template: MilestoneTemplate) = _state.update { current ->
        val existing = current.milestones.map { it.lowercase() }.toSet()
        val additions = template.milestones.filter { it.lowercase() !in existing }
        current.copy(milestones = current.milestones + additions)
    }

    fun onBack() = _state.update { current ->
        val previous = CreateGoalStep.entries.getOrNull(current.step.ordinal - 1)
        if (previous == null) current else current.copy(step = previous)
    }

    fun onNext() {
        val current = _state.value
        if (!current.canAdvance) return

        val next = CreateGoalStep.entries.getOrNull(current.step.ordinal + 1)
        if (next == null) {
            save()
            return
        }

        _state.update { updated ->
            updated.copy(
                step = next,
                // Suggestions are computed when entering the milestone step, so
                // they reflect the final title rather than a half-typed one.
                templates = if (next == CreateGoalStep.Milestones) {
                    MilestoneTemplates.suggestFor(updated.title)
                } else {
                    updated.templates
                },
            )
        }
    }

    private fun save() {
        val current = _state.value
        val areaId = current.selectedAreaId ?: return
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            val id = goalRepository.createGoal(
                GoalDraft(
                    title = current.title,
                    why = current.why,
                    lifeAreaId = areaId,
                    emoji = current.emoji,
                    targetDate = resolveTargetDate(current),
                    priority = current.priority,
                    seasonId = current.activeSeason?.id,
                    milestoneTitles = current.milestones,
                ),
            )
            _state.update { it.copy(isSaving = false, createdGoalId = id) }
        }
    }

    /**
     * Turns the chosen preset into a date.
     *
     * [TargetChoice.EndOfSeason] falls back to three months when the season has no
     * end date, rather than silently producing an undated goal the user believes
     * is dated.
     */
    private fun resolveTargetDate(state: CreateGoalUiState): LocalDate? {
        val today = clock.today()
        return when (state.targetChoice) {
            TargetChoice.OneMonth -> today.plusMonths(1)
            TargetChoice.ThreeMonths -> today.plusMonths(3)
            TargetChoice.SixMonths -> today.plusMonths(6)
            TargetChoice.EndOfSeason -> state.activeSeason?.endDate ?: today.plusMonths(3)
            TargetChoice.NoDate -> null
        }
    }
}
