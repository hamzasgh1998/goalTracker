package com.milestone.app.ui.screens.reflection

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.entity.ReflectionEntity
import com.milestone.app.data.repository.ReflectionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject

/** One past week, as a card in the history. */
data class ReflectionHistoryEntry(
    val weekStart: LocalDate,
    val weekNumber: Int,
    /** How many of the five questions were answered. */
    val answeredCount: Int,
    /**
     * The line shown on the card.
     *
     * Their answer to "what are you proud of?", falling back to "what moved
     * forward?". Quoted verbatim — nothing on this screen is summarised, which is
     * the same promise the reflection screen itself makes.
     */
    val excerpt: String?,
    val isCurrentWeek: Boolean,
) {
    val isEmpty: Boolean get() = answeredCount == 0
}

data class ReflectionHistoryUiState(
    val isLoading: Boolean = true,
    val entries: List<ReflectionHistoryEntry> = emptyList(),
    /** Resolved from the app clock so date labels never read the system clock. */
    val today: LocalDate = LocalDate.now(),
) {
    val weeksWritten: Int get() = entries.count { !it.isEmpty }
}

/**
 * Every reflection the user has written, newest first.
 *
 * Observed rather than loaded once: editing a week and coming back should show
 * the revision, and the list is small enough that a live query costs nothing.
 */
@HiltViewModel
class ReflectionHistoryViewModel @Inject constructor(
    reflectionRepository: ReflectionRepository,
    private val clock: AppClock,
) : ViewModel() {

    val state: StateFlow<ReflectionHistoryUiState> = reflectionRepository.observeAll()
        .map { reflections ->
            val thisWeek = clock.weekStart()
            ReflectionHistoryUiState(
                isLoading = false,
                today = clock.today(),
                entries = reflections.map { reflection ->
                    ReflectionHistoryEntry(
                        weekStart = reflection.weekStart,
                        weekNumber = reflection.weekStart.get(WEEK_OF_YEAR),
                        answeredCount = reflection.answeredCount,
                        excerpt = reflection.proudOf?.takeIf { it.isNotBlank() }
                            ?: reflection.movedForward?.takeIf { it.isNotBlank() },
                        isCurrentWeek = reflection.weekStart == thisWeek,
                    )
                },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = ReflectionHistoryUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L

        /**
         * Locale-aware: which day a week starts on, and therefore which week a
         * date falls in, differs by country.
         */
        val WEEK_OF_YEAR = WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()
    }
}

/** How many of the five prompts this entry actually answered. */
private val ReflectionEntity.answeredCount: Int
    get() = listOf(movedForward, didNotMove, surprised, proudOf, nextFocus)
        .count { !it.isNullOrBlank() }
