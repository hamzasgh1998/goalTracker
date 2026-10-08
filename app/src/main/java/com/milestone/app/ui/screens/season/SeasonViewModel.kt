package com.milestone.app.ui.screens.season

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.data.local.entity.LifeSeasonEntity
import com.milestone.app.data.repository.AchievementRepository
import com.milestone.app.data.repository.GoalRepository
import com.milestone.app.data.repository.SeasonProgress
import com.milestone.app.data.repository.SeasonRepository
import com.milestone.app.domain.GoalHealthCalculator
import com.milestone.app.domain.achievement.AchievementCatalog
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** Transient season-screen state the database knows nothing about. */
data class SeasonLocalState(
    /** Goal whose tier is being changed, if any. */
    val retieringGoalId: Long? = null,
    val isStartingSeason: Boolean = false,
    val draftTitle: String = "",
    val draftTheme: String = "",
    val draftIntention: String = "",
    val themeOptions: List<String> = emptyList(),
    val draftStart: LocalDate? = null,
    val draftEnd: LocalDate? = null,
)

data class SeasonUiState(
    val isLoading: Boolean = true,
    val season: LifeSeasonEntity? = null,
    val progress: SeasonProgress? = null,
    val primary: List<GoalUi> = emptyList(),
    val secondary: List<GoalUi> = emptyList(),
    val maintenance: List<GoalUi> = emptyList(),
    /** Open goals not attached to this season. */
    val outsideSeason: List<GoalUi> = emptyList(),
    val pastSeasons: List<LifeSeasonEntity> = emptyList(),
    val local: SeasonLocalState = SeasonLocalState(),
) {
    val goalProgress: Float
        get() = if (primary.isEmpty()) 0f else primary.map(GoalUi::progress).average().toFloat()

    /**
     * The season's own discipline check.
     *
     * Three is the number the whole product is built around; going past it is
     * allowed but is worth saying out loud.
     */
    val isOverCommitted: Boolean get() = primary.size > MAX_PRIMARY

    companion object {
        const val MAX_PRIMARY = 3
    }
}

@HiltViewModel
class SeasonViewModel @Inject constructor(
    private val seasonRepository: SeasonRepository,
    private val goalRepository: GoalRepository,
    private val achievementRepository: AchievementRepository,
    private val healthCalculator: GoalHealthCalculator,
) : ViewModel() {

    private val local = MutableStateFlow(SeasonLocalState())

    val state: StateFlow<SeasonUiState> = combine(
        seasonRepository.observeActive(),
        goalRepository.observeOpenSummaries(),
        seasonRepository.observeAll(),
        local,
    ) { season, openGoals, allSeasons, localState ->
        val goals = openGoals.map { summary ->
            summary.toUi(healthCalculator) to summary.goal.seasonId
        }
        val inSeason = goals.filter { (_, seasonId) -> seasonId != null && seasonId == season?.id }
            .map { (goal, _) -> goal }

        SeasonUiState(
            isLoading = false,
            season = season,
            progress = season?.let(seasonRepository::progressOf),
            primary = inSeason.filter { it.priority == GoalPriority.Primary },
            secondary = inSeason.filter { it.priority == GoalPriority.Secondary },
            maintenance = inSeason.filter { it.priority == GoalPriority.Maintenance },
            outsideSeason = goals
                .filter { (_, seasonId) -> seasonId == null || seasonId != season?.id }
                .map { (goal, _) -> goal },
            pastSeasons = allSeasons.filter { it.id != season?.id },
            local = localState,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = SeasonUiState(),
    )

    // ------------------------------------------------------------ goal tiers

    fun onStartRetiering(goalId: Long) = local.update { it.copy(retieringGoalId = goalId) }

    fun onCancelRetiering() = local.update { it.copy(retieringGoalId = null) }

    fun onPriorityChosen(priority: GoalPriority) {
        val goalId = local.value.retieringGoalId ?: return
        viewModelScope.launch {
            goalRepository.setPriority(goalId, priority)
            local.update { it.copy(retieringGoalId = null) }
        }
    }

    /** Pulls a goal into the running season, keeping its current tier. */
    fun onAddToSeason(goalId: Long) {
        val seasonId = state.value.season?.id ?: return
        viewModelScope.launch { goalRepository.setSeason(goalId, seasonId) }
    }

    /** Removes a goal from the season without touching the goal itself. */
    fun onRemoveFromSeason(goalId: Long) {
        viewModelScope.launch {
            goalRepository.setSeason(goalId, null)
            local.update { it.copy(retieringGoalId = null) }
        }
    }

    // --------------------------------------------------------- new season

    fun onStartNewSeason() {
        val suggestion = seasonRepository.suggestForToday()
        local.update {
            it.copy(
                isStartingSeason = true,
                draftTitle = suggestion.title,
                draftTheme = suggestion.themeOptions.first(),
                draftIntention = "",
                themeOptions = suggestion.themeOptions,
                draftStart = suggestion.startDate,
                draftEnd = suggestion.endDate,
            )
        }
    }

    fun onCancelNewSeason() = local.update { it.copy(isStartingSeason = false) }

    fun onDraftTitleChange(value: String) = local.update { it.copy(draftTitle = value) }

    fun onDraftThemeChange(value: String) = local.update { it.copy(draftTheme = value) }

    fun onDraftIntentionChange(value: String) = local.update { it.copy(draftIntention = value) }

    /**
     * Starts the next season.
     *
     * Goals are deliberately *not* carried over: choosing again is the ritual that
     * makes a season mean anything. They stay reachable under "not in this season"
     * and can be pulled in one at a time.
     */
    fun onConfirmNewSeason() {
        val draft = local.value
        if (draft.draftTitle.isBlank() || draft.draftTheme.isBlank()) return
        val endingSeason = state.value

        viewModelScope.launch {
            // Award Focus for the season being closed, if it kept to three.
            if (endingSeason.season != null &&
                endingSeason.primary.isNotEmpty() &&
                endingSeason.primary.size <= SeasonUiState.MAX_PRIMARY
            ) {
                achievementRepository.unlock(AchievementCatalog.FOCUS)
            }

            seasonRepository.startSeason(
                title = draft.draftTitle,
                theme = draft.draftTheme,
                intention = draft.draftIntention,
                startDate = draft.draftStart ?: LocalDate.now(),
                endDate = draft.draftEnd,
            )
            achievementRepository.evaluateCounters()
            local.update { it.copy(isStartingSeason = false) }
        }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
