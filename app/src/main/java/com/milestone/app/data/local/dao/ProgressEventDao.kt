package com.milestone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.milestone.app.data.local.entity.ProgressEventEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface ProgressEventDao {

    @Query("SELECT * FROM progress_event ORDER BY occurredAt DESC")
    fun observeAll(): Flow<List<ProgressEventEntity>>

    @Query("SELECT * FROM progress_event ORDER BY occurredAt DESC LIMIT :limit")
    fun observeRecent(limit: Int): Flow<List<ProgressEventEntity>>

    @Query("SELECT * FROM progress_event WHERE goalId = :goalId ORDER BY occurredAt DESC")
    fun observeForGoal(goalId: Long): Flow<List<ProgressEventEntity>>

    /**
     * Events inside a half-open window, used to build the weekly summary counts.
     *
     * Half-open ([from, until)) so consecutive weeks can't both claim the same
     * event at a boundary.
     */
    @Query(
        """
        SELECT * FROM progress_event
        WHERE occurredAt >= :from AND occurredAt < :until
        ORDER BY occurredAt ASC
        """,
    )
    fun observeBetween(from: Instant, until: Instant): Flow<List<ProgressEventEntity>>

    @Query(
        """
        SELECT * FROM progress_event
        WHERE occurredAt >= :from AND occurredAt < :until
        ORDER BY occurredAt ASC
        """,
    )
    suspend fun getBetween(from: Instant, until: Instant): List<ProgressEventEntity>

    @Insert
    suspend fun insert(event: ProgressEventEntity): Long

    @Insert
    suspend fun insertAll(events: List<ProgressEventEntity>)

    @Query("SELECT COUNT(*) FROM progress_event")
    suspend fun count(): Int
}
