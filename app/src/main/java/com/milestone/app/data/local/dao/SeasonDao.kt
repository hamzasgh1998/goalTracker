package com.milestone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.milestone.app.data.local.entity.LifeSeasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonDao {

    @Query("SELECT * FROM life_season WHERE isActive = 1 LIMIT 1")
    fun observeActive(): Flow<LifeSeasonEntity?>

    @Query("SELECT * FROM life_season WHERE isActive = 1 LIMIT 1")
    suspend fun getActive(): LifeSeasonEntity?

    @Query("SELECT * FROM life_season ORDER BY startDate DESC")
    fun observeAll(): Flow<List<LifeSeasonEntity>>

    @Query("SELECT * FROM life_season WHERE id = :id")
    fun observeById(id: Long): Flow<LifeSeasonEntity?>

    @Insert
    suspend fun insert(season: LifeSeasonEntity): Long

    @Update
    suspend fun update(season: LifeSeasonEntity)

    @Query("UPDATE life_season SET isActive = 0")
    suspend fun clearActiveFlags()

    @Query("UPDATE life_season SET isActive = 1 WHERE id = :id")
    suspend fun markActive(id: Long)

    /**
     * Starts a season, guaranteeing it is the only active one.
     *
     * Wrapped in a transaction because the two statements together are the
     * invariant — a crash between them would leave the app with no active season
     * and an empty Home screen.
     */
    @Transaction
    suspend fun startSeason(season: LifeSeasonEntity): Long {
        clearActiveFlags()
        return insert(season.copy(isActive = true))
    }

    /** Switches which existing season is active, e.g. when revisiting a past one. */
    @Transaction
    suspend fun activate(id: Long) {
        clearActiveFlags()
        markActive(id)
    }
}
