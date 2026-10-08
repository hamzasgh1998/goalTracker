package com.milestone.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant
import java.time.LocalDate

/**
 * A step on the way to a goal. The unit the app is named after.
 *
 * Milestones are ordered by [sortOrder] rather than by date, because the journey
 * visualisation is a sequence — "run 5km" comes before "run 7km" whether or not
 * either has a deadline.
 */
@Entity(
    tableName = "milestone",
    foreignKeys = [
        ForeignKey(
            entity = GoalEntity::class,
            parentColumns = ["id"],
            childColumns = ["goalId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["goalId", "sortOrder"])],
)
data class MilestoneEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val goalId: Long,

    val title: String,

    /** Optional note the user attaches — context, a reminder, how it went. */
    val note: String? = null,

    /**
     * Position in the goal's sequence.
     *
     * Sparse values are fine; reordering rewrites the whole goal's milestones in
     * one transaction, so gaps never accumulate.
     */
    val sortOrder: Int,

    val isCompleted: Boolean = false,

    val completedAt: Instant? = null,

    val dueDate: LocalDate? = null,

    val createdAt: Instant,
)
