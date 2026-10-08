package com.milestone.app.data.repository

import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.dao.ProgressEventDao
import com.milestone.app.data.local.dao.ReflectionDao
import com.milestone.app.data.local.entity.ProgressEventEntity
import com.milestone.app.data.local.entity.ReflectionEntity
import com.milestone.app.domain.model.ProgressEventType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.temporal.WeekFields
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/** The user's answers for one week. All fields optional. */
data class ReflectionAnswers(
    val movedForward: String? = null,
    val didNotMove: String? = null,
    val surprised: String? = null,
    val proudOf: String? = null,
    val nextFocus: String? = null,
) {
    val isEmpty: Boolean
        get() = listOf(movedForward, didNotMove, surprised, proudOf, nextFocus)
            .all { it.isNullOrBlank() }
}

/**
 * What a week actually contained.
 *
 * Note that nothing here is generated or summarised: the counts are literal
 * database tallies and [highlight] / [nextFocus] are the user's own sentences,
 * shown back verbatim. No paid service, and nothing invented.
 */
data class WeeklySummary(
    val weekStart: LocalDate,
    val weekNumber: Int,
    val milestonesCompleted: Int,
    val goalsCompleted: Int,
    val goalsStarted: Int,
    val questsCompleted: Int,
    /** The user's answer to "what are you proud of?". */
    val highlight: String?,
    /** Their answer to "what matters most next week?". */
    val nextFocus: String?,
    /** Titles of the milestones finished, for the "you moved forward" list. */
    val milestoneTitles: List<String>,
) {
    val hasActivity: Boolean
        get() = milestonesCompleted + goalsCompleted + goalsStarted + questsCompleted > 0
}

@Singleton
class ReflectionRepository @Inject constructor(
    private val reflectionDao: ReflectionDao,
    private val progressEventDao: ProgressEventDao,
    private val clock: AppClock,
) {
    fun currentWeekStart(): LocalDate = clock.weekStart()

    fun observeForWeek(weekStart: LocalDate = currentWeekStart()): Flow<ReflectionEntity?> =
        reflectionDao.observeForWeek(weekStart)

    fun observeAll(): Flow<List<ReflectionEntity>> = reflectionDao.observeAll()

    suspend fun getForWeek(weekStart: LocalDate): ReflectionEntity? =
        reflectionDao.getForWeek(weekStart)

    /**
     * Saves a week's reflection, preserving the original [ReflectionEntity.createdAt].
     *
     * Upsert by week rather than insert-new, so re-opening the screen mid-week
     * continues the same entry instead of creating a second one.
     */
    suspend fun save(weekStart: LocalDate, answers: ReflectionAnswers) {
        val now = clock.now()
        val existing = reflectionDao.getForWeek(weekStart)
        reflectionDao.put(
            ReflectionEntity(
                id = existing?.id ?: 0,
                weekStart = weekStart,
                movedForward = answers.movedForward?.trim()?.ifEmpty { null },
                didNotMove = answers.didNotMove?.trim()?.ifEmpty { null },
                surprised = answers.surprised?.trim()?.ifEmpty { null },
                proudOf = answers.proudOf?.trim()?.ifEmpty { null },
                nextFocus = answers.nextFocus?.trim()?.ifEmpty { null },
                createdAt = existing?.createdAt ?: now,
                updatedAt = now,
            ),
        )
    }

    /** Builds the week's summary from committed events plus the saved answers. */
    suspend fun summaryFor(weekStart: LocalDate = currentWeekStart()): WeeklySummary {
        val zone = clock.zone()
        val from = weekStart.atStartOfDay(zone).toInstant()
        val until = weekStart.plusWeeks(1).atStartOfDay(zone).toInstant()

        val events = progressEventDao.getBetween(from, until)
        val reflection = reflectionDao.getForWeek(weekStart)

        return WeeklySummary(
            weekStart = weekStart,
            weekNumber = weekStart.get(WeekFields.of(Locale.getDefault()).weekOfWeekBasedYear()),
            milestonesCompleted = events.count(ProgressEventType.MilestoneCompleted),
            goalsCompleted = events.count(ProgressEventType.GoalCompleted),
            goalsStarted = events.count(ProgressEventType.GoalCreated),
            questsCompleted = events.count(ProgressEventType.QuestCompleted),
            highlight = reflection?.proudOf,
            nextFocus = reflection?.nextFocus,
            milestoneTitles = events
                .filter { it.type == ProgressEventType.MilestoneCompleted }
                .map { it.title },
        )
    }
}

private fun List<ProgressEventEntity>.count(type: ProgressEventType): Int =
    count { it.type == type }
