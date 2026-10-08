package com.milestone.app.data.local

import androidx.room.TypeConverter
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.domain.model.ProgressEventType
import java.time.Instant
import java.time.LocalDate

/**
 * Room converters.
 *
 * Two storage conventions, applied consistently across every entity:
 *  - a *moment* ([Instant]) is stored as epoch milliseconds — "when did this
 *    happen", always absolute;
 *  - a *calendar day* ([LocalDate]) is stored as an epoch day number — "which day
 *    is this due", deliberately timezone-free so a deadline doesn't shift when the
 *    user travels.
 *
 * Enums are stored by `name`, which makes the database readable but means renaming
 * an enum constant requires a migration.
 */
class Converters {

    @TypeConverter
    fun instantToMillis(value: Instant?): Long? = value?.toEpochMilli()

    @TypeConverter
    fun millisToInstant(value: Long?): Instant? = value?.let(Instant::ofEpochMilli)

    @TypeConverter
    fun localDateToEpochDay(value: LocalDate?): Long? = value?.toEpochDay()

    @TypeConverter
    fun epochDayToLocalDate(value: Long?): LocalDate? = value?.let(LocalDate::ofEpochDay)

    @TypeConverter
    fun goalStatusToName(value: GoalStatus): String = value.name

    @TypeConverter
    fun nameToGoalStatus(value: String): GoalStatus =
        enumValueOrDefault(value, GoalStatus.Active)

    @TypeConverter
    fun goalPriorityToName(value: GoalPriority): String = value.name

    @TypeConverter
    fun nameToGoalPriority(value: String): GoalPriority =
        enumValueOrDefault(value, GoalPriority.Secondary)

    @TypeConverter
    fun closingReasonToName(value: ClosingReason?): String? = value?.name

    @TypeConverter
    fun nameToClosingReason(value: String?): ClosingReason? =
        value?.let { enumValueOrDefault(it, ClosingReason.Other) }

    @TypeConverter
    fun progressEventTypeToName(value: ProgressEventType): String = value.name

    @TypeConverter
    fun nameToProgressEventType(value: String): ProgressEventType =
        enumValueOrDefault(value, ProgressEventType.MilestoneCompleted)
}

/**
 * Reads an enum by name, falling back instead of throwing.
 *
 * A row written by a newer build (or an enum constant removed in a refactor) should
 * degrade to a sensible default rather than crash the user's whole history on
 * read — the data here is irreplaceable and entirely local.
 */
private inline fun <reified T : Enum<T>> enumValueOrDefault(name: String, default: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: default
