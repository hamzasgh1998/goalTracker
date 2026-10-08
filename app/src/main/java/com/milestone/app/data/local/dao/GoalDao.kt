package com.milestone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.milestone.app.data.local.entity.GoalEntity
import com.milestone.app.data.local.relation.GoalSummary
import com.milestone.app.data.local.relation.GoalWithDetails
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface GoalDao {

    @Transaction
    @Query("SELECT * FROM goal WHERE id = :id")
    fun observeDetails(id: Long): Flow<GoalWithDetails?>

    @Query("SELECT * FROM goal WHERE id = :id")
    suspend fun get(id: Long): GoalEntity?

    /**
     * All open goals, most urgent framing first: primary before secondary before
     * maintenance, then soonest target date, then newest.
     *
     * `targetDate IS NULL` is sorted last explicitly — SQLite orders NULL first by
     * default, which would put undated someday-goals above a deadline this week.
     */
    @Query(
        """
        SELECT g.*,
            a.name AS areaName,
            a.emoji AS areaEmoji,
            a.colorArgb AS areaColorArgb,
            (SELECT COUNT(*) FROM milestone m WHERE m.goalId = g.id) AS totalMilestones,
            (SELECT COUNT(*) FROM milestone m2
                WHERE m2.goalId = g.id AND m2.isCompleted = 1) AS completedMilestones,
            (SELECT m3.title FROM milestone m3
                WHERE m3.goalId = g.id AND m3.isCompleted = 0
                ORDER BY m3.sortOrder ASC LIMIT 1) AS nextMilestoneTitle
        FROM goal g
        LEFT JOIN life_area a ON a.id = g.lifeAreaId
        WHERE g.status IN ('Active', 'Paused')
        ORDER BY
            CASE g.priority WHEN 'Primary' THEN 0 WHEN 'Secondary' THEN 1 ELSE 2 END ASC,
            CASE WHEN g.targetDate IS NULL THEN 1 ELSE 0 END ASC,
            g.targetDate ASC,
            g.createdAt DESC
        """,
    )
    fun observeOpenSummaries(): Flow<List<GoalSummary>>

    /** The active season's goals only — what Home is allowed to show. */
    @Query(
        """
        SELECT g.*,
            a.name AS areaName,
            a.emoji AS areaEmoji,
            a.colorArgb AS areaColorArgb,
            (SELECT COUNT(*) FROM milestone m WHERE m.goalId = g.id) AS totalMilestones,
            (SELECT COUNT(*) FROM milestone m2
                WHERE m2.goalId = g.id AND m2.isCompleted = 1) AS completedMilestones,
            (SELECT m3.title FROM milestone m3
                WHERE m3.goalId = g.id AND m3.isCompleted = 0
                ORDER BY m3.sortOrder ASC LIMIT 1) AS nextMilestoneTitle
        FROM goal g
        LEFT JOIN life_area a ON a.id = g.lifeAreaId
        WHERE g.seasonId = :seasonId AND g.status IN ('Active', 'Paused')
        ORDER BY
            CASE g.priority WHEN 'Primary' THEN 0 WHEN 'Secondary' THEN 1 ELSE 2 END ASC,
            g.createdAt ASC
        """,
    )
    fun observeSeasonSummaries(seasonId: Long): Flow<List<GoalSummary>>

    @Query(
        """
        SELECT g.*,
            a.name AS areaName,
            a.emoji AS areaEmoji,
            a.colorArgb AS areaColorArgb,
            (SELECT COUNT(*) FROM milestone m WHERE m.goalId = g.id) AS totalMilestones,
            (SELECT COUNT(*) FROM milestone m2
                WHERE m2.goalId = g.id AND m2.isCompleted = 1) AS completedMilestones,
            (SELECT m3.title FROM milestone m3
                WHERE m3.goalId = g.id AND m3.isCompleted = 0
                ORDER BY m3.sortOrder ASC LIMIT 1) AS nextMilestoneTitle
        FROM goal g
        LEFT JOIN life_area a ON a.id = g.lifeAreaId
        WHERE g.lifeAreaId = :areaId AND g.status IN ('Active', 'Paused')
        ORDER BY g.createdAt DESC
        """,
    )
    fun observeSummariesByArea(areaId: Long): Flow<List<GoalSummary>>

    /** Past Chapters: completed and archived goals, most recently closed first. */
    @Query(
        """
        SELECT g.*,
            a.name AS areaName,
            a.emoji AS areaEmoji,
            a.colorArgb AS areaColorArgb,
            (SELECT COUNT(*) FROM milestone m WHERE m.goalId = g.id) AS totalMilestones,
            (SELECT COUNT(*) FROM milestone m2
                WHERE m2.goalId = g.id AND m2.isCompleted = 1) AS completedMilestones,
            NULL AS nextMilestoneTitle
        FROM goal g
        LEFT JOIN life_area a ON a.id = g.lifeAreaId
        WHERE g.status IN ('Completed', 'Archived')
        ORDER BY COALESCE(g.archivedAt, g.completedAt, g.createdAt) DESC
        """,
    )
    fun observeClosedSummaries(): Flow<List<GoalSummary>>

    @Query("SELECT COUNT(*) FROM goal")
    suspend fun count(): Int

    @Query("SELECT COUNT(DISTINCT lifeAreaId) FROM goal")
    suspend fun distinctAreaCount(): Int

    @Query("SELECT COUNT(*) FROM goal WHERE status = 'Completed'")
    suspend fun completedCount(): Int

    @Insert
    suspend fun insert(goal: GoalEntity): Long

    @Update
    suspend fun update(goal: GoalEntity)

    @Query("DELETE FROM goal WHERE id = :id")
    suspend fun deleteById(id: Long)

    /**
     * Stamps "something moved here just now".
     *
     * Called from the same transaction as the completion that caused it, so goal
     * health can never drift out of step with the milestone data it describes.
     */
    @Query("UPDATE goal SET lastProgressAt = :at WHERE id = :id")
    suspend fun touchProgress(id: Long, at: Instant)

    @Query("UPDATE goal SET status = :status WHERE id = :id")
    suspend fun setStatus(id: Long, status: GoalStatus)

    @Query("UPDATE goal SET priority = :priority WHERE id = :id")
    suspend fun setPriority(id: Long, priority: GoalPriority)

    @Query("UPDATE goal SET seasonId = :seasonId WHERE id = :id")
    suspend fun setSeason(id: Long, seasonId: Long?)

    @Query(
        """
        UPDATE goal
        SET status = 'Completed', completedAt = :at, lastProgressAt = :at
        WHERE id = :id
        """,
    )
    suspend fun markCompleted(id: Long, at: Instant)

    /**
     * Reopens a completed goal, clearing the completion stamps.
     *
     * Needed because completion is inferred from milestones: un-ticking the last
     * milestone has to be able to put the goal back into Active.
     */
    @Query(
        """
        UPDATE goal
        SET status = 'Active', completedAt = NULL, lastProgressAt = :at
        WHERE id = :id
        """,
    )
    suspend fun markReopened(id: Long, at: Instant)

    @Query(
        """
        UPDATE goal
        SET status = 'Archived', archivedAt = :at, closingReason = :reason, closingNote = :note
        WHERE id = :id
        """,
    )
    suspend fun archive(id: Long, at: Instant, reason: ClosingReason, note: String?)

    @Query(
        """
        UPDATE goal
        SET status = 'Active', archivedAt = NULL, closingReason = NULL, closingNote = NULL,
            lastProgressAt = :at
        WHERE id = :id
        """,
    )
    suspend fun restore(id: Long, at: Instant)
}
