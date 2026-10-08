package com.milestone.app.data.repository

import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.dao.UserProfileDao
import com.milestone.app.data.local.entity.UserProfileEntity
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepository @Inject constructor(
    private val userProfileDao: UserProfileDao,
    private val clock: AppClock,
) {
    fun observe(): Flow<UserProfileEntity?> = userProfileDao.observe()

    suspend fun get(): UserProfileEntity? = userProfileDao.get()

    /**
     * Creates or updates the profile at the end of onboarding.
     *
     * [UserProfileEntity.startedAt] is preserved across re-runs so that "your
     * journey began" keeps pointing at the real first day even if the user later
     * changes their name or re-enters onboarding.
     */
    suspend fun completeOnboarding(name: String) {
        val existing = userProfileDao.get()
        userProfileDao.put(
            UserProfileEntity(
                name = name.trim(),
                startedAt = existing?.startedAt ?: clock.now(),
                hasOnboarded = true,
            ),
        )
    }

    suspend fun updateName(name: String) = userProfileDao.updateName(name.trim())

    suspend fun resetOnboarding() = userProfileDao.setOnboarded(false)
}
