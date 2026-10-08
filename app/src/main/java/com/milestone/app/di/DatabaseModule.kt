package com.milestone.app.di

import android.content.Context
import androidx.room.Room
import com.milestone.app.data.local.MilestoneDatabase
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
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): MilestoneDatabase =
        Room.databaseBuilder(
            context = context,
            klass = MilestoneDatabase::class.java,
            name = MilestoneDatabase.NAME,
        )
            // Deliberately no fallbackToDestructiveMigration(): the user's goal
            // history is irreplaceable, so a missing migration should fail loudly
            // in development rather than silently wipe their data in production.
            .build()

    @Provides fun provideUserProfileDao(db: MilestoneDatabase): UserProfileDao = db.userProfileDao()

    @Provides fun provideLifeAreaDao(db: MilestoneDatabase): LifeAreaDao = db.lifeAreaDao()

    @Provides fun provideSeasonDao(db: MilestoneDatabase): SeasonDao = db.seasonDao()

    @Provides fun provideGoalDao(db: MilestoneDatabase): GoalDao = db.goalDao()

    @Provides fun provideMilestoneDao(db: MilestoneDatabase): MilestoneDao = db.milestoneDao()

    @Provides fun provideQuestDao(db: MilestoneDatabase): QuestDao = db.questDao()

    @Provides fun provideReflectionDao(db: MilestoneDatabase): ReflectionDao = db.reflectionDao()

    @Provides fun provideAchievementDao(db: MilestoneDatabase): AchievementDao = db.achievementDao()

    @Provides
    fun provideProgressEventDao(db: MilestoneDatabase): ProgressEventDao = db.progressEventDao()

    @Provides fun provideBackupDao(db: MilestoneDatabase): BackupDao = db.backupDao()
}
