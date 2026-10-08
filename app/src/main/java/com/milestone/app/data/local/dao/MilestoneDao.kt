package com.milestone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.milestone.app.data.local.entity.MilestoneEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface MilestoneDao {

    @Query("SELECT * FROM milestone WHERE goalId = :goalId ORDER BY sortOrder ASC")
    fun observeForGoal(goalId: Long): Flow<List<MilestoneEntity>>

    /**
     * Every milestone in the database, ordered.
     *
     * Used by the Life Map, which draws all open goals at once: one query the
     * caller groups by goal beats a per-goal query inside a loop, and the whole
     * table is tens to low hundreds of rows for a real user.
     */
    @Query("SELECT * FROM milestone ORDER BY goalId ASC, sortOrder ASC")
    fun observeAll(): Flow<List<MilestoneEntity>>

    @Query("SELECT * FROM milestone WHERE id = :id")
    suspend fun get(id: Long): MilestoneEntity?

    @Query("SELECT * FROM milestone WHERE goalId = :goalId ORDER BY sortOrder ASC")
    suspend fun getForGoal(goalId: Long): List<MilestoneEntity>

    /** The next step on a goal: lowest-ordered milestone still outstanding. */
    @Query(
        """
        SELECT * FROM milestone
        WHERE goalId = :goalId AND isCompleted = 0
        ORDER BY sortOrder ASC LIMIT 1
        """,
    )
    suspend fun nextOpenForGoal(goalId: Long): MilestoneEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM milestone WHERE goalId = :goalId")
    suspend fun maxSortOrder(goalId: Long): Int

    @Query("SELECT COUNT(*) FROM milestone WHERE goalId = :goalId")
    suspend fun countForGoal(goalId: Long): Int

    @Query("SELECT COUNT(*) FROM milestone WHERE goalId = :goalId AND isCompleted = 0")
    suspend fun openCountForGoal(goalId: Long): Int

    @Query("SELECT COUNT(*) FROM milestone WHERE isCompleted = 1")
    suspend fun completedCount(): Int

    @Insert
    suspend fun insert(milestone: MilestoneEntity): Long

    @Insert
    suspend fun insertAll(milestones: List<MilestoneEntity>): List<Long>

    @Update
    suspend fun update(milestone: MilestoneEntity)

    @Update
    suspend fun updateAll(milestones: List<MilestoneEntity>)

    @Delete
    suspend fun delete(milestone: MilestoneEntity)

    @Query("DELETE FROM milestone WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE milestone SET isCompleted = :completed, completedAt = :at WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, at: Instant?)

    @Query("UPDATE milestone SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int)

    /**
     * Rewrites the order of a goal's milestones in one pass.
     *
     * Takes the ids in their new sequence and renumbers from zero, so the stored
     * order always matches what the user just dragged into place without gaps
     * accumulating over repeated moves.
     */
    @Transaction
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }
}
