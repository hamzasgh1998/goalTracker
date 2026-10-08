package com.milestone.app.ui.screens.goals

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.data.repository.GoalRepository
import com.milestone.app.domain.GoalHealthCalculator
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import javax.inject.Inject

/** Which slice of the user's goals the list is showing. */
enum class GoalFilter(val label: String) {
    Active("Active"),
    Paused("Paused"),

    /** Completed and archived goals. Named for the feature, not the data. */
    PastChapters("Past chapters"),
}

data class GoalsUiState(
    val filter: GoalFilter = GoalFilter.Active,
    val goals: List<GoalUi> = emptyList(),
    val activeCount: Int = 0,
    val pausedCount: Int = 0,
    val closedCount: Int = 0,
    val isLoading: Boolean = true,
)

@HiltViewModel
class GoalsViewModel @Inject constructor(
    goalRepository: GoalRepository,
    private val healthCalculator: GoalHealthCalculator,
) : ViewModel() {

    private val filter = MutableStateFlow(GoalFilter.Active)

    val state: StateFlow<GoalsUiState> = combine(
        goalRepository.observeOpenSummaries(),
        goalRepository.observeClosedSummaries(),
        filter,
    ) { open, closed, selected ->
        val openGoals = open.map { it.toUi(healthCalculator) }
        val closedGoals = closed.map { it.toUi(healthCalculator) }

        val active = openGoals.filter { it.status == GoalStatus.Active }
        val paused = openGoals.filter { it.status == GoalStatus.Paused }

        GoalsUiState(
            filter = selected,
            goals = when (selected) {
                GoalFilter.Active -> active
                GoalFilter.Paused -> paused
                GoalFilter.PastChapters -> closedGoals
            },
            activeCount = active.size,
            pausedCount = paused.size,
            closedCount = closedGoals.size,
            isLoading = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = GoalsUiState(),
    )

    fun onFilterChange(value: GoalFilter) = filter.update { value }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
