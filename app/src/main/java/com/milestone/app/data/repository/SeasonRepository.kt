package com.milestone.app.data.repository

import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.dao.ProgressEventDao
import com.milestone.app.data.local.dao.SeasonDao
import com.milestone.app.data.local.entity.LifeSeasonEntity
import com.milestone.app.data.local.entity.ProgressEventEntity
import com.milestone.app.domain.model.ProgressEventType
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.Month
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** A proposed season, pre-filled so starting one takes a tap rather than an essay. */
data class SeasonSuggestion(
    val title: String,
    val startDate: LocalDate,
    val endDate: LocalDate,
    val themeOptions: List<String>,
)

/** How far through the active season we are, for the Home progress indicator. */
data class SeasonProgress(
    val season: LifeSeasonEntity,
    /** 0f..1f by elapsed time. Null-safe for open-ended seasons (always 0f). */
    val elapsedFraction: Float,
    val daysRemaining: Int?,
)

@Singleton
class SeasonRepository @Inject constructor(
    private val seasonDao: SeasonDao,
    private val progressEventDao: ProgressEventDao,
    private val clock: AppClock,
) {
    fun observeActive(): Flow<LifeSeasonEntity?> = seasonDao.observeActive()

    fun observeAll(): Flow<List<LifeSeasonEntity>> = seasonDao.observeAll()

    fun observeById(id: Long): Flow<LifeSeasonEntity?> = seasonDao.observeById(id)

    suspend fun getActive(): LifeSeasonEntity? = seasonDao.getActive()

    /**
     * Begins a season and records it on the journey.
     *
     * The DAO guarantees this becomes the only active season.
     */
    suspend fun startSeason(
        title: String,
        theme: String,
        intention: String,
        startDate: LocalDate = clock.today(),
        endDate: LocalDate?,
    ): Long {
        val id = seasonDao.startSeason(
            LifeSeasonEntity(
                title = title.trim(),
                theme = theme.trim(),
                intention = intention.trim(),
                startDate = startDate,
                endDate = endDate,
            ),
        )
        progressEventDao.insert(
            ProgressEventEntity(
                type = ProgressEventType.SeasonStarted,
                title = title.trim(),
                subtitle = theme.trim().ifEmpty { null },
                seasonId = id,
                occurredAt = clock.now(),
            ),
        )
        return id
    }

    suspend fun update(season: LifeSeasonEntity) = seasonDao.update(season)

    suspend fun activate(id: Long) = seasonDao.activate(id)

    /** Elapsed-time progress through a season. */
    fun progressOf(season: LifeSeasonEntity): SeasonProgress {
        val today = clock.today()
        val end = season.endDate
            ?: return SeasonProgress(season, elapsedFraction = 0f, daysRemaining = null)

        val total = ChronoUnit.DAYS.between(season.startDate, end).coerceAtLeast(1)
        val elapsed = ChronoUnit.DAYS.between(season.startDate, today).coerceIn(0, total)
        return SeasonProgress(
            season = season,
            elapsedFraction = elapsed.toFloat() / total.toFloat(),
            daysRemaining = ChronoUnit.DAYS.between(today, end).coerceAtLeast(0).toInt(),
        )
    }

    /**
     * Proposes the calendar season the user is currently in.
     *
     * Meteorological quarters (Mar/Jun/Sep/Dec) rather than solstices, because the
     * boundaries land on month starts and read naturally as "Autumn 2026". The
     * season is named for the *northern* hemisphere; the title is freely editable,
     * which is the simplest honest answer for a local-only app that never asks
     * where the user lives.
     */
    fun suggestForToday(): SeasonSuggestion {
        val today = clock.today()
        val (name, startMonth) = when (today.month) {
            Month.MARCH, Month.APRIL, Month.MAY -> "Spring" to Month.MARCH
            Month.JUNE, Month.JULY, Month.AUGUST -> "Summer" to Month.JUNE
            Month.SEPTEMBER, Month.OCTOBER, Month.NOVEMBER -> "Autumn" to Month.SEPTEMBER
            else -> "Winter" to Month.DECEMBER
        }

        // Winter straddles the new year, so its start may be in the previous one.
        val startYear =
            if (startMonth == Month.DECEMBER && today.month != Month.DECEMBER) {
                today.year - 1
            } else {
                today.year
            }
        val start = LocalDate.of(startYear, startMonth, 1)
        val end = start.plusMonths(3).minusDays(1)

        return SeasonSuggestion(
            title = "$name ${start.year}",
            startDate = start,
            endDate = end,
            themeOptions = themeOptionsFor(name),
        )
    }

    /** Theme words that fit the mood of each season, as starting points only. */
    private fun themeOptionsFor(seasonName: String): List<String> = when (seasonName) {
        "Spring" -> listOf("Begin", "Build", "Grow", "Plant")
        "Summer" -> listOf("Explore", "Expand", "Push", "Live")
        "Autumn" -> listOf("Build & Grow", "Focus", "Finish", "Deepen")
        else -> listOf("Rest", "Reset", "Learn", "Reflect")
    }
}
