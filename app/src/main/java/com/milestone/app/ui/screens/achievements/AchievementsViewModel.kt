package com.milestone.app.ui.screens.achievements

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.data.repository.AchievementRepository
import com.milestone.app.data.repository.AchievementState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AchievementsUiState(
    val isLoading: Boolean = true,
    val earned: List<AchievementState> = emptyList(),
    val locked: List<AchievementState> = emptyList(),
) {
    val earnedCount: Int get() = earned.size
    val total: Int get() = earned.size + locked.size
}

@HiltViewModel
class AchievementsViewModel @Inject constructor(
    private val achievementRepository: AchievementRepository,
) : ViewModel() {

    val state: StateFlow<AchievementsUiState> = achievementRepository.observeAll()
        .map { all ->
            AchievementsUiState(
                isLoading = false,
                // Most recent first, so a new unlock is at the top where the
                // celebration banner just pointed.
                earned = all.filter { it.isUnlocked }.sortedByDescending { it.unlockedAt },
                locked = all.filterNot { it.isUnlocked },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = AchievementsUiState(),
        )

    /**
     * Clears the "new" flag on everything.
     *
     * Called when the screen is opened: arriving here *is* seeing them, and
     * leaving the Home banner up afterwards would be nagging.
     */
    fun onScreenSeen() {
        viewModelScope.launch { achievementRepository.markAllSeen() }
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
