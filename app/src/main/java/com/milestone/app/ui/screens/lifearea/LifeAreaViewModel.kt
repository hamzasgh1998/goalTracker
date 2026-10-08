package com.milestone.app.ui.screens.lifearea

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.repository.GoalRepository
import com.milestone.app.data.repository.LifeAreaRepository
import com.milestone.app.domain.GoalHealthCalculator
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.model.toUi
import com.milestone.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LifeAreaUiState(
    val isLoading: Boolean = true,
    val area: LifeAreaEntity? = null,
    val active: List<GoalUi> = emptyList(),
    val paused: List<GoalUi> = emptyList(),
    val milestonesCompleted: Int = 0,
    val milestonesTotal: Int = 0,
) {
    val progress: Float
        get() = if (milestonesTotal == 0) 0f else {
            milestonesCompleted.toFloat() / milestonesTotal
        }

    val isEmpty: Boolean get() = active.isEmpty() && paused.isEmpty()
}

@HiltViewModel
class LifeAreaViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val lifeAreaRepository: LifeAreaRepository,
    goalRepository: GoalRepository,
    private val healthCalculator: GoalHealthCalculator,
) : ViewModel() {

    private val areaId: Long = checkNotNull(savedStateHandle.get<String>(Routes.AREA_ID_ARG))
        .toLong()

    val state: StateFlow<LifeAreaUiState> = combine(
        lifeAreaRepository.observeById(areaId),
        goalRepository.observeSummariesByArea(areaId),
    ) { area, summaries ->
        val goals = summaries.map { it.toUi(healthCalculator) }
        LifeAreaUiState(
            isLoading = false,
            area = area,
            active = goals.filter { it.status == GoalStatus.Active },
            paused = goals.filter { it.status == GoalStatus.Paused },
            // Area progress is milestone-weighted, not an average of percentages:
            // a nine-step goal should count for more than a two-step one.
            milestonesCompleted = goals.sumOf { it.completedMilestones },
            milestonesTotal = goals.sumOf { it.totalMilestones },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = LifeAreaUiState(),
    )

    /** Hides the area without touching its goals; built-ins can't be deleted. */
    fun onStopTracking() {
        viewModelScope.launch { lifeAreaRepository.setTracked(areaId, tracked = false) }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
