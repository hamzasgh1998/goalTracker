package com.milestone.app.ui.screens.map

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.data.local.dao.MilestoneDao
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

/** How closely the map is drawn. */
enum class MapZoom(val label: String) {
    /** Everything at once, nodes packed tight. */
    Overview("Whole life"),

    /** Fewer nodes on screen, each one readable. */
    Detail("Up close"),
}

/** One stop on a path. */
data class MapNode(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
    val isCurrent: Boolean,
)

/** One goal, drawn as a route. */
data class MapPath(
    val goalId: Long,
    val title: String,
    val emoji: String,
    val areaName: String,
    val color: Color,
    val progress: Float,
    val isPaused: Boolean,
    val nodes: List<MapNode>,
)

data class LifeMapUiState(
    val isLoading: Boolean = true,
    val zoom: MapZoom = MapZoom.Detail,
    val paths: List<MapPath> = emptyList(),
    val totalNodes: Int = 0,
    val reachedNodes: Int = 0,
) {
    val isEmpty: Boolean get() = paths.isEmpty()
}

@HiltViewModel
class LifeMapViewModel @Inject constructor(
    goalRepository: GoalRepository,
    milestoneDao: MilestoneDao,
    private val healthCalculator: GoalHealthCalculator,
) : ViewModel() {

    private val zoom = MutableStateFlow(MapZoom.Detail)

    val state: StateFlow<LifeMapUiState> = combine(
        goalRepository.observeOpenSummaries(),
        milestoneDao.observeAll(),
        zoom,
    ) { summaries, milestones, currentZoom ->
        val byGoal = milestones.groupBy { it.goalId }
        val paths = summaries.map { summary ->
            val goal: GoalUi = summary.toUi(healthCalculator)
            val goalMilestones = byGoal[goal.id].orEmpty()
            // The first outstanding step is "you are here" — exactly one per path.
            val currentId = goalMilestones.firstOrNull { !it.isCompleted }?.id

            MapPath(
                goalId = goal.id,
                title = goal.title,
                emoji = goal.emoji,
                areaName = goal.areaName,
                color = goal.areaColor,
                progress = goal.progress,
                isPaused = goal.status == GoalStatus.Paused,
                nodes = goalMilestones.map { milestone ->
                    MapNode(
                        id = milestone.id,
                        title = milestone.title,
                        isCompleted = milestone.isCompleted,
                        isCurrent = milestone.id == currentId,
                    )
                },
            )
        }
            // A goal with no milestones has no route to draw.
            .filter { it.nodes.isNotEmpty() }

        LifeMapUiState(
            isLoading = false,
            zoom = currentZoom,
            paths = paths,
            totalNodes = paths.sumOf { it.nodes.size },
            reachedNodes = paths.sumOf { path -> path.nodes.count { it.isCompleted } },
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = LifeMapUiState(),
    )

    fun onZoomChange(value: MapZoom) = zoom.update { value }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
