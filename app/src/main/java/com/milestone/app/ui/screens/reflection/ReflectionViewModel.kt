package com.milestone.app.ui.screens.reflection

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.repository.AchievementRepository
import com.milestone.app.data.repository.ReflectionAnswers
import com.milestone.app.data.repository.ReflectionRepository
import com.milestone.app.data.repository.WeeklySummary
import com.milestone.app.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

/** The five prompts, in the order they are asked. */
enum class ReflectionQuestion(val prompt: String, val hint: String) {
    MovedForward(
        prompt = "What moved forward this week?",
        hint = "Even the small things count.",
    ),
    DidNotMove(
        prompt = "What didn't move?",
        hint = "No judgement. Just noticing.",
    ),
    Surprised(
        prompt = "What surprised you?",
        hint = "Good or bad.",
    ),
    ProudOf(
        prompt = "What are you proud of?",
        hint = "This becomes the week's highlight.",
    ),
    NextFocus(
        prompt = "What matters most next week?",
        hint = "One thing, if you can.",
    ),
}

data class ReflectionUiState(
    val isLoading: Boolean = true,
    val weekStart: LocalDate = LocalDate.now(),
    val weekNumber: Int = 0,
    /** False when the user opened an older week from the history. */
    val isCurrentWeek: Boolean = true,
    val summary: WeeklySummary? = null,
    val answers: Map<ReflectionQuestion, String> = emptyMap(),
    /** True once a reflection exists for this week. */
    val alreadyWritten: Boolean = false,
    val isSaving: Boolean = false,
    val justSaved: Boolean = false,
) {
    fun answerFor(question: ReflectionQuestion): String = answers[question].orEmpty()

    val hasAnyAnswer: Boolean get() = answers.values.any { it.isNotBlank() }
}

@HiltViewModel
class ReflectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val reflectionRepository: ReflectionRepository,
    private val achievementRepository: AchievementRepository,
    private val clock: AppClock,
) : ViewModel() {

    /**
     * The week being reflected on.
     *
     * Absent for the normal entry points, which mean "this week". The history
     * passes an ISO date; an unparseable one falls back to the current week
     * rather than crashing on a bad deep link.
     */
    private val requestedWeek: LocalDate? = savedStateHandle
        .get<String>(Routes.REFLECTION_WEEK_ARG)
        ?.let { raw -> runCatching { LocalDate.parse(raw) }.getOrNull() }

    private val _state = MutableStateFlow(ReflectionUiState())
    val state: StateFlow<ReflectionUiState> = _state.asStateFlow()

    init {
        load()
    }

    /**
     * Loads the week's tallies and any existing answers.
     *
     * Read once rather than observed: this screen is a form, and having the text
     * fields re-seeded from the database mid-typing would fight the user.
     */
    private fun load() {
        viewModelScope.launch {
            val thisWeek = reflectionRepository.currentWeekStart()
            // Normalised to a Monday: a date from anywhere in the week resolves to
            // the same entry the reflection is keyed by.
            val weekStart = requestedWeek?.let(clock::weekStart) ?: thisWeek
            val existing = reflectionRepository.getForWeek(weekStart)
            val summary = reflectionRepository.summaryFor(weekStart)

            _state.update {
                it.copy(
                    isLoading = false,
                    weekStart = weekStart,
                    weekNumber = summary.weekNumber,
                    isCurrentWeek = weekStart == thisWeek,
                    summary = summary,
                    alreadyWritten = existing != null,
                    answers = buildMap {
                        existing?.movedForward?.let { v -> put(ReflectionQuestion.MovedForward, v) }
                        existing?.didNotMove?.let { v -> put(ReflectionQuestion.DidNotMove, v) }
                        existing?.surprised?.let { v -> put(ReflectionQuestion.Surprised, v) }
                        existing?.proudOf?.let { v -> put(ReflectionQuestion.ProudOf, v) }
                        existing?.nextFocus?.let { v -> put(ReflectionQuestion.NextFocus, v) }
                    },
                )
            }
        }
    }

    fun onAnswerChange(question: ReflectionQuestion, value: String) {
        _state.update { current ->
            current.copy(
                answers = current.answers + (question to value),
                // Any edit after saving puts the screen back into a dirty state.
                justSaved = false,
            )
        }
    }

    fun onSave() {
        val current = _state.value
        if (current.isSaving) return
        _state.update { it.copy(isSaving = true) }

        viewModelScope.launch {
            reflectionRepository.save(
                weekStart = current.weekStart,
                answers = ReflectionAnswers(
                    movedForward = current.answerFor(ReflectionQuestion.MovedForward),
                    didNotMove = current.answerFor(ReflectionQuestion.DidNotMove),
                    surprised = current.answerFor(ReflectionQuestion.Surprised),
                    proudOf = current.answerFor(ReflectionQuestion.ProudOf),
                    nextFocus = current.answerFor(ReflectionQuestion.NextFocus),
                ),
            )
            achievementRepository.evaluateCounters()

            // Re-read so the summary picks up the new highlight immediately.
            val summary = reflectionRepository.summaryFor(current.weekStart)
            _state.update {
                it.copy(
                    isSaving = false,
                    justSaved = true,
                    alreadyWritten = true,
                    summary = summary,
                )
            }
        }
    }
}
