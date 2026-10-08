package com.milestone.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.milestone.app.data.local.dao.AchievementDao
import com.milestone.app.data.local.dao.BackupDao
import com.milestone.app.data.local.dao.GoalDao
import com.milestone.app.data.local.dao.LifeAreaDao
import com.milestone.app.data.local.dao.MilestoneDao
import com.milestone.app.data.local.dao.ProgressEventDao
import com.milestone.app.data.local.dao.QuestDao
import com.milestone.app.data.local.dao.ReflectionDao
import com.milestone.app.data.local.dao.SeasonDao
import com.milestone.app.data.local.dao.UserProfileDao
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
 * The app's single source of truth. Entirely on-device; nothing here syncs.
 *
 * On schema changes: this database holds data the user cannot get back from
 * anywhere, so destructive migration must never be enabled. Bump [VERSION] and
 * write a real migration. The generated schema JSON under `app/schemas` is what
 * makes that diff reviewable, which is why KSP is configured to emit it.
 */
@Database(
    entities = [
        UserProfileEntity::class,
        LifeAreaEntity::class,
        LifeSeasonEntity::class,
        GoalEntity::class,
        MilestoneEntity::class,
        QuestEntity::class,
        ReflectionEntity::class,
        AchievementEntity::class,
        ProgressEventEntity::class,
    ],
    version = MilestoneDatabase.VERSION,
    exportSchema = true,
)
@TypeConverters(Converters::class)
abstract class MilestoneDatabase : RoomDatabase() {

    abstract fun userProfileDao(): UserProfileDao
    abstract fun lifeAreaDao(): LifeAreaDao
    abstract fun seasonDao(): SeasonDao
    abstract fun goalDao(): GoalDao
    abstract fun milestoneDao(): MilestoneDao
    abstract fun questDao(): QuestDao
    abstract fun reflectionDao(): ReflectionDao
    abstract fun achievementDao(): AchievementDao
    abstract fun progressEventDao(): ProgressEventDao

    /** Whole-table access, used only by export and restore. */
    abstract fun backupDao(): BackupDao

    companion object {
        const val VERSION = 1
        const val NAME = "milestone.db"
    }
}
