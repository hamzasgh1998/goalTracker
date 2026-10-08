package com.milestone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.local.relation.LifeAreaSummary
import kotlinx.coroutines.flow.Flow

@Dao
interface LifeAreaDao {

    @Query("SELECT * FROM life_area ORDER BY sortOrder ASC")
    fun observeAll(): Flow<List<LifeAreaEntity>>

    @Query("SELECT * FROM life_area WHERE isTracked = 1 ORDER BY sortOrder ASC")
    fun observeTracked(): Flow<List<LifeAreaEntity>>

    @Query("SELECT * FROM life_area WHERE id = :id")
    fun observeById(id: Long): Flow<LifeAreaEntity?>

    /**
     * Areas with the counts their radial orb shows.
     *
     * Only open goals count: an area shouldn't read "8 goals" when seven of them
     * are finished chapters. Archived work is reachable from Past Chapters instead.
     */
    @Query(
        """
        SELECT a.*,
            (SELECT COUNT(*) FROM goal g
                WHERE g.lifeAreaId = a.id AND g.status IN ('Active', 'Paused')) AS goalCount,
            (SELECT COUNT(*) FROM milestone m
                INNER JOIN goal g2 ON g2.id = m.goalId
                WHERE g2.lifeAreaId = a.id AND g2.status IN ('Active', 'Paused')) AS milestoneCount,
            (SELECT COUNT(*) FROM milestone m2
                INNER JOIN goal g3 ON g3.id = m2.goalId
                WHERE g3.lifeAreaId = a.id AND g3.status IN ('Active', 'Paused')
                AND m2.isCompleted = 1) AS completedMilestoneCount
        FROM life_area a
        WHERE a.isTracked = 1
        ORDER BY a.sortOrder ASC
        """,
    )
    fun observeTrackedSummaries(): Flow<List<LifeAreaSummary>>

    @Query("SELECT * FROM life_area ORDER BY sortOrder ASC")
    suspend fun getAll(): List<LifeAreaEntity>

    @Query("SELECT COUNT(*) FROM life_area")
    suspend fun count(): Int

    @Insert
    suspend fun insert(area: LifeAreaEntity): Long

    @Insert
    suspend fun insertAll(areas: List<LifeAreaEntity>): List<Long>

    @Update
    suspend fun update(area: LifeAreaEntity)

    @Query("UPDATE life_area SET isTracked = :tracked WHERE id = :id")
    suspend fun setTracked(id: Long, tracked: Boolean)

    @Query("UPDATE life_area SET isTracked = 1 WHERE id IN (:ids)")
    suspend fun trackAll(ids: List<Long>)

    /** Only user-created areas are deletable; built-ins are hidden, not removed. */
    @Delete
    suspend fun delete(area: LifeAreaEntity)
}
