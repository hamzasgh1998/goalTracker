package com.milestone.app.data.repository

import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.dao.ProgressEventDao
import com.milestone.app.data.local.entity.ProgressEventEntity
import com.milestone.app.domain.model.ProgressEventType
import kotlinx.coroutines.flow.Flow
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Events sharing a day, with that day already resolved for display. */
data class JourneyDay(
    val date: LocalDate,
    val events: List<ProgressEventEntity>,
)

@Singleton
class JourneyRepository @Inject constructor(
    private val progressEventDao: ProgressEventDao,
    private val clock: AppClock,
) {
    fun observeAll(): Flow<List<ProgressEventEntity>> = progressEventDao.observeAll()

    fun observeRecent(limit: Int = 8): Flow<List<ProgressEventEntity>> =
        progressEventDao.observeRecent(limit)

    fun observeForGoal(goalId: Long): Flow<List<ProgressEventEntity>> =
        progressEventDao.observeForGoal(goalId)

    fun observeBetween(from: Instant, until: Instant): Flow<List<ProgressEventEntity>> =
        progressEventDao.observeBetween(from, until)

    suspend fun eventsBetween(from: Instant, until: Instant): List<ProgressEventEntity> =
        progressEventDao.getBetween(from, until)

    /**
     * Buckets events into days, newest first.
     *
     * Grouping happens here rather than in the UI because the day boundary depends
     * on the user's zone, which is the clock's business — a composable doing this
     * would quietly use UTC and put late-evening events on the wrong day.
     */
    fun groupByDay(events: List<ProgressEventEntity>): List<JourneyDay> =
        events
            .groupBy { it.occurredAt.atZone(clock.zone()).toLocalDate() }
            .toSortedMap(reverseOrder())
            .map { (date, dayEvents) ->
                JourneyDay(date = date, events = dayEvents.sortedByDescending { it.occurredAt })
            }

    /** Counts of the event types worth celebrating, for the weekly summary. */
    fun tally(events: List<ProgressEventEntity>): Map<ProgressEventType, Int> =
        events.groupingBy { it.type }.eachCount()
}
