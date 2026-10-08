package com.milestone.app.ui.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.entity.LifeSeasonEntity
import com.milestone.app.data.local.entity.ReflectionEntity
import com.milestone.app.data.local.entity.UserProfileEntity
import com.milestone.app.data.local.relation.GoalSummary
import com.milestone.app.data.local.relation.LifeAreaSummary
import com.milestone.app.data.prefs.UserPreferences
import com.milestone.app.data.repository.AchievementRepository
import com.milestone.app.data.repository.AchievementState
import com.milestone.app.data.repository.GoalRepository
import com.milestone.app.data.repository.LifeAreaRepository
import com.milestone.app.data.repository.ProfileRepository
import com.milestone.app.data.repository.ReflectionRepository
import com.milestone.app.data.repository.SeasonProgress
import com.milestone.app.data.repository.SeasonRepository
import com.milestone.app.domain.GoalHealthCalculator
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.ui.model.GoalUi
import com.milestone.app.ui.model.LifeAreaUi
import com.milestone.app.ui.model.toUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import javax.inject.Inject

/** The single action the Home screen points the user at. */
data class NextStepUi(
    val goalId: Long,
    val goalTitle: String,
    val milestoneTitle: String,
    val goalEmoji: String,
)

data class HomeUiState(
    val isLoading: Boolean = true,
    val userName: String = "",
    val greeting: String = "",
    val motivation: String = "",
    val season: SeasonProgress? = null,
    /** Average completion across the season's primary goals, 0f..1f. */
    val seasonProgress: Float = 0f,
    /** At most three: the season's primary goals. */
    val focusGoals: List<GoalUi> = emptyList(),
    val supportingGoals: List<GoalUi> = emptyList(),
    val nextStep: NextStepUi? = null,
    val areas: List<LifeAreaUi> = emptyList(),
    val unseenAchievements: List<AchievementState> = emptyList(),
    val showReflectionPrompt: Boolean = false,
) {
    val hasAnyGoals: Boolean get() = focusGoals.isNotEmpty() || supportingGoals.isNotEmpty()
}

/**
 * What the season is: who the user is, which season is running, and its goals.
 *
 * Grouped so the state can be assembled from two typed [combine] calls rather than
 * one seven-argument call that degrades into an array of `Any?` and casts.
 */
private data class SeasonSnapshot(
    val profile: UserProfileEntity?,
    val season: LifeSeasonEntity?,
    val goals: List<GoalSummary>,
)

/** Everything surrounding the season: context, nudges and celebrations. */
private data class ContextSnapshot(
    val areas: List<LifeAreaSummary>,
    val unseenAchievements: List<AchievementState>,
    val reflectionThisWeek: ReflectionEntity?,
    val dismissedPromptWeek: Long?,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    profileRepository: ProfileRepository,
    private val seasonRepository: SeasonRepository,
    goalRepository: GoalRepository,
    lifeAreaRepository: LifeAreaRepository,
    reflectionRepository: ReflectionRepository,
    private val achievementRepository: AchievementRepository,
    private val preferences: UserPreferences,
    private val healthCalculator: GoalHealthCalculator,
    private val clock: AppClock,
) : ViewModel() {

    private val seasonSnapshot = combine(
        profileRepository.observe(),
        seasonRepository.observeActive(),
        goalRepository.observeActiveSeasonSummaries(),
        ::SeasonSnapshot,
    )

    private val contextSnapshot = combine(
        lifeAreaRepository.observeTrackedSummaries(),
        achievementRepository.observeUnseen(),
        reflectionRepository.observeForWeek(),
        preferences.reflectionPromptDismissedWeek,
        ::ContextSnapshot,
    )

    val state: StateFlow<HomeUiState> =
        combine(seasonSnapshot, contextSnapshot) { season, context ->
            buildState(season, context)
        }.stateIn(
            scope = viewModelScope,
            // Survives configuration changes and brief tab switches without
            // re-running seven queries on every return to Home.
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = HomeUiState(),
        )

    fun onAchievementSeen(key: String) {
        viewModelScope.launch { achievementRepository.markSeen(key) }
    }

    fun onDismissReflectionPrompt() {
        viewModelScope.launch {
            preferences.dismissReflectionPrompt(clock.weekStart().toEpochDay())
        }
    }

    private fun buildState(
        snapshot: SeasonSnapshot,
        context: ContextSnapshot,
    ): HomeUiState {
        val goals = snapshot.goals.map { it.toUi(healthCalculator) }
        val primary = goals.filter { it.priority == GoalPriority.Primary }
        val focus = primary.take(MAX_FOCUS_GOALS)
        val focusIds = focus.map(GoalUi::id).toSet()

        return HomeUiState(
            isLoading = false,
            userName = snapshot.profile?.name.orEmpty(),
            greeting = greetingFor(snapshot.profile?.name),
            motivation = motivationForToday(),
            season = snapshot.season?.let(seasonRepository::progressOf),
            seasonProgress = averageProgress(primary),
            focusGoals = focus,
            supportingGoals = goals.filterNot { it.id in focusIds },
            // Fall back to all goals so a user with no primary goal still gets a
            // next step rather than an empty slot.
            nextStep = nextStepFrom(focus.ifEmpty { goals }),
            areas = context.areas.map { it.toUi() },
            unseenAchievements = context.unseenAchievements,
            showReflectionPrompt = shouldPromptReflection(
                hasReflectionThisWeek = context.reflectionThisWeek != null,
                dismissedWeekEpochDay = context.dismissedPromptWeek,
            ),
        )
    }

    /** Time-of-day greeting. The one part of the app that changes by the hour. */
    private fun greetingFor(name: String?): String {
        val hour = clock.now().atZone(clock.zone()).hour
        val part = when (hour) {
            in 0..4 -> "Still up"
            in 5..11 -> "Good morning"
            in 12..17 -> "Good afternoon"
            else -> "Good evening"
        }
        return if (name.isNullOrBlank()) part else "$part, $name"
    }

    /**
     * Picks the day's line from a fixed list.
     *
     * Indexed by epoch day, so it is stable for the whole day and different
     * tomorrow. No randomness means the copy never changes while the user is
     * looking at it; no generation means nothing has to be paid for.
     */
    private fun motivationForToday(): String =
        MOTIVATIONS[(clock.today().toEpochDay() % MOTIVATIONS.size).toInt()]

    private fun averageProgress(goals: List<GoalUi>): Float =
        if (goals.isEmpty()) 0f else goals.map(GoalUi::progress).average().toFloat()

    /** The first unfinished milestone across the user's most important goals. */
    private fun nextStepFrom(goals: List<GoalUi>): NextStepUi? =
        goals.firstNotNullOfOrNull { goal ->
            goal.nextStep?.let { milestone ->
                NextStepUi(
                    goalId = goal.id,
                    goalTitle = goal.title,
                    milestoneTitle = milestone,
                    goalEmoji = goal.emoji,
                )
            }
        }

    /**
     * Whether to offer the weekly reflection.
     *
     * Only from Thursday onward, so there is enough week to look back on, and
     * never twice: writing one or dismissing the prompt both silence it until the
     * following Monday.
     */
    private fun shouldPromptReflection(
        hasReflectionThisWeek: Boolean,
        dismissedWeekEpochDay: Long?,
    ): Boolean {
        if (hasReflectionThisWeek) return false
        if (dismissedWeekEpochDay == clock.weekStart().toEpochDay()) return false
        return clock.today().dayOfWeek >= DayOfWeek.THURSDAY
    }

    private companion object {
        const val MAX_FOCUS_GOALS = 3
        const val STOP_TIMEOUT_MS = 5_000L

        /**
         * Calm encouragement. Deliberately avoids hustle language — the product's
         * position is that one meaningful step beats a burst of activity.
         */
        val MOTIVATIONS = listOf(
            "You don't need to change everything today.",
            "One important thing, moved forward.",
            "Your next chapter starts with one step.",
            "Small steps still count as steps.",
            "Progress is quieter than people expect.",
            "What you do today is the story you read later.",
            "Finish something small.",
            "Momentum beats motivation.",
            "A season is long enough to matter.",
            "Begin before you feel ready.",
            "Consistency is the whole trick.",
            "Keep the promise you made to yourself.",
        )
    }
}
