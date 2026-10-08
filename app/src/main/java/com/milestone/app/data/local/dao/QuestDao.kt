package com.milestone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.milestone.app.data.local.entity.QuestEntity
import kotlinx.coroutines.flow.Flow
import java.time.Instant

@Dao
interface QuestDao {

    @Query("SELECT * FROM quest WHERE milestoneId = :milestoneId ORDER BY sortOrder ASC")
    fun observeForMilestone(milestoneId: Long): Flow<List<QuestEntity>>

    @Query("SELECT * FROM quest WHERE id = :id")
    suspend fun get(id: Long): QuestEntity?

    @Query("SELECT COALESCE(MAX(sortOrder), -1) FROM quest WHERE milestoneId = :milestoneId")
    suspend fun maxSortOrder(milestoneId: Long): Int

    @Query("SELECT COUNT(*) FROM quest WHERE milestoneId = :milestoneId AND isCompleted = 0")
    suspend fun openCountForMilestone(milestoneId: Long): Int

    @Insert
    suspend fun insert(quest: QuestEntity): Long

    @Insert
    suspend fun insertAll(quests: List<QuestEntity>): List<Long>

    @Update
    suspend fun update(quest: QuestEntity)

    @Delete
    suspend fun delete(quest: QuestEntity)

    @Query("DELETE FROM quest WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE quest SET isCompleted = :completed, completedAt = :at WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean, at: Instant?)

    @Query("UPDATE quest SET sortOrder = :sortOrder WHERE id = :id")
    suspend fun setSortOrder(id: Long, sortOrder: Int)

    @Transaction
    suspend fun reorder(orderedIds: List<Long>) {
        orderedIds.forEachIndexed { index, id -> setSortOrder(id, index) }
    }
}
