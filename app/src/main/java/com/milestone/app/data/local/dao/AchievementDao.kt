package com.milestone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.milestone.app.data.local.entity.AchievementEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AchievementDao {

    @Query("SELECT * FROM achievement_unlock ORDER BY unlockedAt DESC")
    fun observeAll(): Flow<List<AchievementEntity>>

    @Query("SELECT * FROM achievement_unlock WHERE isNew = 1 ORDER BY unlockedAt ASC")
    fun observeUnseen(): Flow<List<AchievementEntity>>

    @Query("SELECT achievementKey FROM achievement_unlock")
    suspend fun unlockedKeys(): List<String>

    @Query("SELECT EXISTS(SELECT 1 FROM achievement_unlock WHERE achievementKey = :key)")
    suspend fun isUnlocked(key: String): Boolean

    /**
     * IGNORE, not REPLACE: an achievement is earned once.
     *
     * Unlock checks run after many different actions, so a repeat insert is normal
     * and must not overwrite the original [AchievementEntity.unlockedAt] — that
     * date is the record of when the user actually did it.
     */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(unlock: AchievementEntity): Long

    @Query("UPDATE achievement_unlock SET isNew = 0 WHERE achievementKey = :key")
    suspend fun markSeen(key: String)

    @Query("UPDATE achievement_unlock SET isNew = 0")
    suspend fun markAllSeen()

    @Query("SELECT COUNT(*) FROM achievement_unlock")
    suspend fun count(): Int
}
