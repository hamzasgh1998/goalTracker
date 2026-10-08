package com.milestone.app.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.milestone.app.data.local.entity.AchievementEntity
import com.milestone.app.data.local.entity.GoalEntity
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.local.entity.LifeSeasonEntity
import com.milestone.app.data.local.entity.MilestoneEntity
import com.milestone.app.data.local.entity.ProgressEventEntity
import com.milestone.app.data.local.entity.QuestEntity
import com.milestone.app.data.local.entity.ReflectionEntity
import com.milestone.app.data.local.entity.UserProfileEntity

/**
 * Whole-table reads and writes, for export and restore only.
 *
 * Separate from the feature DAOs on purpose. Those expose the narrow, ordered,
 * projected queries the screens actually want; this one exists to move every row
 * in and out verbatim, which is the opposite concern. Keeping them apart stops
 * "give me every goal including archived ones, unsorted" from becoming something
 * a screen reaches for by accident.
 *
 * The inserts keep each row's original id — primary keys are only generated when
 * the id is 0 — so the foreign keys in a backup still line up after a restore.
 */
@Dao
interface BackupDao {

    // ------------------------------------------------------------------ reads

    @Query("SELECT * FROM user_profile")
    suspend fun profiles(): List<UserProfileEntity>

    @Query("SELECT * FROM life_area ORDER BY id")
    suspend fun lifeAreas(): List<LifeAreaEntity>

    @Query("SELECT * FROM life_season ORDER BY id")
    suspend fun seasons(): List<LifeSeasonEntity>

    @Query("SELECT * FROM goal ORDER BY id")
    suspend fun goals(): List<GoalEntity>

    @Query("SELECT * FROM milestone ORDER BY id")
    suspend fun milestones(): List<MilestoneEntity>

    @Query("SELECT * FROM quest ORDER BY id")
    suspend fun quests(): List<QuestEntity>

    @Query("SELECT * FROM reflection ORDER BY id")
    suspend fun reflections(): List<ReflectionEntity>

    @Query("SELECT * FROM achievement_unlock ORDER BY id")
    suspend fun achievements(): List<AchievementEntity>

    @Query("SELECT * FROM progress_event ORDER BY id")
    suspend fun progressEvents(): List<ProgressEventEntity>

    // ----------------------------------------------------------------- writes

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfiles(rows: List<UserProfileEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLifeAreas(rows: List<LifeAreaEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSeasons(rows: List<LifeSeasonEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGoals(rows: List<GoalEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMilestones(rows: List<MilestoneEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertQuests(rows: List<QuestEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReflections(rows: List<ReflectionEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAchievements(rows: List<AchievementEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgressEvents(rows: List<ProgressEventEntity>)

    // ---------------------------------------------------------------- deletes

    /**
     * Clears every table, children first.
     *
     * Order matters even though the schema cascades: deleting a life area would
     * take its goals with it, and relying on that would mean the wipe depended on
     * foreign-key enforcement being switched on.
     */
    @Query("DELETE FROM progress_event")
    suspend fun clearProgressEvents()

    @Query("DELETE FROM achievement_unlock")
    suspend fun clearAchievements()

    @Query("DELETE FROM reflection")
    suspend fun clearReflections()

    @Query("DELETE FROM quest")
    suspend fun clearQuests()

    @Query("DELETE FROM milestone")
    suspend fun clearMilestones()

    @Query("DELETE FROM goal")
    suspend fun clearGoals()

    @Query("DELETE FROM life_season")
    suspend fun clearSeasons()

    @Query("DELETE FROM life_area")
    suspend fun clearLifeAreas()

    @Query("DELETE FROM user_profile")
    suspend fun clearProfiles()
}
