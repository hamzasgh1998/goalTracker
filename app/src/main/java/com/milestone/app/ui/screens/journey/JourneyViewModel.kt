package com.milestone.app.ui.screens.journey

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.entity.ProgressEventEntity
import com.milestone.app.data.repository.JourneyRepository
import com.milestone.app.domain.model.ProgressEventType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** A day's worth of history, with its heading already resolved. */
data class JourneyDayUi(
    val date: LocalDate,
    /** "Today", "3 days ago", "2 months ago". */
    val relativeLabel: String,
    /** The absolute date, shown quietly beside the relative one. */
    val absoluteLabel: String,
    val events: List<ProgressEventEntity>,
)

data class JourneyUiState(
    val isLoading: Boolean = true,
    val days: List<JourneyDayUi> = emptyList(),
    val totalEvents: Int = 0,
    val milestonesCompleted: Int = 0,
    val goalsCompleted: Int = 0,
) {
    val isEmpty: Boolean get() = days.isEmpty()
}

@HiltViewModel
class JourneyViewModel @Inject constructor(
    private val journeyRepository: JourneyRepository,
    private val clock: AppClock,
) : ViewModel() {

    val state: StateFlow<JourneyUiState> = journeyRepository.observeAll()
        .map { events ->
            val today = clock.today()
            JourneyUiState(
                isLoading = false,
                days = journeyRepository.groupByDay(events).map { day ->
                    JourneyDayUi(
                        date = day.date,
                        relativeLabel = relativeLabel(day.date, today),
                        absoluteLabel = absoluteLabel(day.date, today),
                        events = day.events,
                    )
                },
                totalEvents = events.size,
                milestonesCompleted = events.count {
                    it.type == ProgressEventType.MilestoneCompleted
                },
                goalsCompleted = events.count { it.type == ProgressEventType.GoalCompleted },
            )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            initialValue = JourneyUiState(),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/**
 * Human-scale distance into the past.
 *
 * Coarsens as it goes back — exact days for the last week, then weeks, then
 * months — because "4 days ago" is information a person can act on and "243 days
 * ago" is not. The absolute date is always shown alongside, so nothing is lost by
 * rounding.
 */
internal fun relativeLabel(date: LocalDate, today: LocalDate): String {
    val days = ChronoUnit.DAYS.between(date, today)
    return when {
        days <= 0L -> "Today"
        days == 1L -> "Yesterday"
        days < 7L -> days.toString() + " days ago"
        days < 14L -> "Last week"
        days < 31L -> (days / 7L).toString() + " weeks ago"
        days < 62L -> "Last month"
        days < 365L -> (days / 30L).toString() + " months ago"
        days < 730L -> "Last year"
        else -> (days / 365L).toString() + " years ago"
    }
}

/** "14 September", with the year added once it stops being obvious. */
internal fun absoluteLabel(date: LocalDate, today: LocalDate): String {
    val month = date.month.getDisplayName(
        java.time.format.TextStyle.FULL,
        java.util.Locale.getDefault(),
    )
    return if (date.year == today.year) {
        date.dayOfMonth.toString() + " " + month
    } else {
        date.dayOfMonth.toString() + " " + month + " " + date.year
    }
}
