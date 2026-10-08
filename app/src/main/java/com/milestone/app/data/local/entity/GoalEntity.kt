package com.milestone.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import java.time.Instant
import java.time.LocalDate

/**
 * The central object of the app.
 *
 * Deleting a [LifeAreaEntity] cascades to its goals, but deleting a
 * [LifeSeasonEntity] only nulls [seasonId] — a season ending must never take the
 * user's goals with it. That asymmetry is the whole reason the two foreign keys
 * differ in `onDelete`.
 */
@Entity(
    tableName = "goal",
    foreignKeys = [
        ForeignKey(
            entity = LifeAreaEntity::class,
            parentColumns = ["id"],
            childColumns = ["lifeAreaId"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LifeSeasonEntity::class,
            parentColumns = ["id"],
            childColumns = ["seasonId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [
        Index(value = ["lifeAreaId"]),
        Index(value = ["seasonId"]),
        Index(value = ["status"]),
    ],
)
data class GoalEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** "Run my first 10K". Shown at display size, so it should be short. */
    val title: String,

    /**
     * The user's answer to "why does this matter to you?".
     *
     * Asked during creation and quoted back on the detail screen, because it is
     * the thing that still matters on day 40 when motivation has gone.
     */
    val why: String,

    val lifeAreaId: Long,

    /** Null when the goal isn't part of the current season — a someday goal. */
    val seasonId: Long? = null,

    /** An emoji the user picks; stands in for an icon everywhere this goal appears. */
    val emoji: String,

    /** Null when the user declined to commit to a date, which is allowed. */
    val targetDate: LocalDate? = null,

    val status: GoalStatus = GoalStatus.Active,

    val priority: GoalPriority = GoalPriority.Secondary,

    val createdAt: Instant,

    /**
     * Last time anything moved on this goal.
     *
     * Maintained on every milestone/quest completion and read by the goal-health
     * calculation. Denormalised on purpose: health is computed for every visible
     * goal on Home, and deriving it from a MAX() over milestones each time would
     * mean a subquery per row.
     */
    val lastProgressAt: Instant? = null,

    val completedAt: Instant? = null,

    /** Set when the goal moves to Past Chapters. */
    val closingReason: ClosingReason? = null,

    /** Optional free text captured alongside [closingReason]. */
    val closingNote: String? = null,

    val archivedAt: Instant? = null,
)
