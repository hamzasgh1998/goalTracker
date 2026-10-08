package com.milestone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * One week's written reflection.
 *
 * Keyed by [weekStart] (the Monday) with a unique index, so re-opening the
 * reflection screen in the same week edits the existing entry instead of stacking
 * duplicates.
 *
 * Every text field is nullable: a user who answers one question out of five has
 * still reflected, and the summary renders whatever is there.
 */
@Entity(
    tableName = "reflection",
    indices = [Index(value = ["weekStart"], unique = true)],
)
data class ReflectionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** The Monday of the week being reflected on. */
    val weekStart: LocalDate,

    /** "What moved forward this week?" */
    val movedForward: String? = null,

    /** "What didn't move?" */
    val didNotMove: String? = null,

    /** "What surprised you?" */
    val surprised: String? = null,

    /** "What are you proud of?" — becomes the week's highlight in the summary. */
    val proudOf: String? = null,

    /** "What matters most next week?" */
    val nextFocus: String? = null,

    val createdAt: Instant,

    val updatedAt: Instant,
)
