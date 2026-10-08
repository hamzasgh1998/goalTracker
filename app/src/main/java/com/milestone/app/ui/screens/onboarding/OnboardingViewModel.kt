package com.milestone.app.ui.screens.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.repository.AchievementRepository
import com.milestone.app.data.repository.LifeAreaRepository
import com.milestone.app.data.repository.ProfileRepository
import com.milestone.app.data.repository.SeasonRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

enum class OnboardingStep {
    /** The four welcome pages. */
    Intro,
    Name,
    Areas,
    Season,
    ;

    val isFirst: Boolean get() = this == Intro
}

data class OnboardingUiState(
    val step: OnboardingStep = OnboardingStep.Intro,
    val name: String = "",
    val areas: List<LifeAreaEntity> = emptyList(),
    val selectedAreaIds: Set<Long> = emptySet(),
    val seasonTitle: String = "",
    val seasonTheme: String = "",
    val seasonIntention: String = "",
    val themeOptions: List<String> = emptyList(),
    val seasonStart: LocalDate? = null,
    val seasonEnd: LocalDate? = null,
    val isSaving: Boolean = false,
    val isComplete: Boolean = false,
) {
    /** Whether the current step has enough to continue. */
    val canAdvance: Boolean
        get() = when (step) {
            OnboardingStep.Intro -> true
            OnboardingStep.Name -> name.isNotBlank()
            // At least one area, so the first goal has somewhere to live.
            OnboardingStep.Areas -> selectedAreaIds.isNotEmpty()
            OnboardingStep.Season -> seasonTitle.isNotBlank() && seasonTheme.isNotBlank()
        }
}

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val lifeAreaRepository: LifeAreaRepository,
    private val seasonRepository: SeasonRepository,
    private val achievementRepository: AchievementRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(OnboardingUiState())
    val state: StateFlow<OnboardingUiState> = _state.asStateFlow()

    init {
        // Life areas are seeded by AppInitializer; make sure they exist even if
        // onboarding is somehow reached first.
        viewModelScope.launch {
            lifeAreaRepository.ensureDefaults()
            val areas = lifeAreaRepository.observeAll()
            val suggestion = seasonRepository.suggestForToday()

            _state.update { current ->
                current.copy(
                    seasonTitle = suggestion.title,
                    seasonTheme = suggestion.themeOptions.first(),
                    themeOptions = suggestion.themeOptions,
                    seasonStart = suggestion.startDate,
                    seasonEnd = suggestion.endDate,
                )
            }

            areas.collect { list ->
                _state.update { it.copy(areas = list) }
            }
        }
    }

    fun onNameChange(value: String) = _state.update { it.copy(name = value) }

    fun onToggleArea(areaId: Long) = _state.update { current ->
        val selected = current.selectedAreaIds
        current.copy(
            selectedAreaIds = if (areaId in selected) selected - areaId else selected + areaId,
        )
    }

    fun onSeasonTitleChange(value: String) = _state.update { it.copy(seasonTitle = value) }

    fun onSeasonThemeChange(value: String) = _state.update { it.copy(seasonTheme = value) }

    fun onSeasonIntentionChange(value: String) = _state.update { it.copy(seasonIntention = value) }

    fun onBack() = _state.update { current ->
        val previous = OnboardingStep.entries.getOrNull(current.step.ordinal - 1)
        if (previous == null) current else current.copy(step = previous)
    }

    /** Advances, or commits everything when already on the last step. */
    fun onNext() {
        val current = _state.value
        if (!current.canAdvance) return

        val next = OnboardingStep.entries.getOrNull(current.step.ordinal + 1)
        if (next != null) {
            _state.update { it.copy(step = next) }
            return
        }
        finish()
    }

    /**
     * Writes the profile, the tracked areas and the first season.
     *
     * Ordered so that if the process dies partway, the user is left needing to
     * redo the *least* work: the profile last means an interrupted run shows
     * onboarding again rather than an app with no areas and no season.
     */
    private fun finish() {
        val current = _state.value
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            lifeAreaRepository.applyTrackedSelection(current.selectedAreaIds)

            seasonRepository.startSeason(
                title = current.seasonTitle,
                theme = current.seasonTheme,
                // Left blank when the user skipped it. Inventing a sentence here
                // and showing it back as their intention would be putting words
                // in their mouth; Home omits the line instead.
                intention = current.seasonIntention,
                startDate = current.seasonStart ?: LocalDate.now(),
                endDate = current.seasonEnd,
            )

            profileRepository.completeOnboarding(current.name)
            achievementRepository.evaluateCounters()

            _state.update { it.copy(isSaving = false, isComplete = true) }
        }
    }
}
