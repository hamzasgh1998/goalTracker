package com.milestone.app.data.repository

import com.milestone.app.data.local.dao.LifeAreaDao
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.local.relation.LifeAreaSummary
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The eight starter areas.
 *
 * These are seeded as ordinary rows, not constants the code depends on: the user
 * can untrack, rename or add to them. Colours are chosen to stay distinguishable
 * from one another *and* from the five accent colours, since an area's colour and
 * the user's accent frequently sit side by side.
 */
object DefaultLifeAreas {
    val entities: List<LifeAreaEntity> = listOf(
        LifeAreaEntity(
            name = "Knowledge", emoji = "🧠", colorArgb = 0xFF5566CC,
            isBuiltIn = true, sortOrder = 0,
        ),
        LifeAreaEntity(
            name = "Health", emoji = "💪", colorArgb = 0xFF2E9E6B,
            isBuiltIn = true, sortOrder = 1,
        ),
        LifeAreaEntity(
            name = "Career", emoji = "💼", colorArgb = 0xFF3D7FA8,
            isBuiltIn = true, sortOrder = 2,
        ),
        LifeAreaEntity(
            name = "Finance", emoji = "💰", colorArgb = 0xFF8A8232,
            isBuiltIn = true, sortOrder = 3,
        ),
        LifeAreaEntity(
            name = "Relationships", emoji = "❤️", colorArgb = 0xFFC05070,
            isBuiltIn = true, sortOrder = 4,
        ),
        LifeAreaEntity(
            name = "Creativity", emoji = "🎨", colorArgb = 0xFF8E5BC4,
            isBuiltIn = true, sortOrder = 5,
        ),
        LifeAreaEntity(
            name = "Experiences", emoji = "🌎", colorArgb = 0xFFC77A3A,
            isBuiltIn = true, sortOrder = 6,
        ),
        LifeAreaEntity(
            name = "Wellbeing", emoji = "🧘", colorArgb = 0xFF4AA3A3,
            isBuiltIn = true, sortOrder = 7,
        ),
    )
}

@Singleton
class LifeAreaRepository @Inject constructor(
    private val lifeAreaDao: LifeAreaDao,
) {
    fun observeAll(): Flow<List<LifeAreaEntity>> = lifeAreaDao.observeAll()

    fun observeTracked(): Flow<List<LifeAreaEntity>> = lifeAreaDao.observeTracked()

    fun observeTrackedSummaries(): Flow<List<LifeAreaSummary>> =
        lifeAreaDao.observeTrackedSummaries()

    fun observeById(id: Long): Flow<LifeAreaEntity?> = lifeAreaDao.observeById(id)

    /**
     * Writes the starter areas once.
     *
     * Idempotent by row count so it is safe to call on every launch — the caller
     * doesn't have to track whether first-run setup already happened.
     */
    suspend fun ensureDefaults() {
        if (lifeAreaDao.count() == 0) {
            lifeAreaDao.insertAll(DefaultLifeAreas.entities)
        }
    }

    /** Applies the onboarding selection: chosen areas tracked, the rest hidden. */
    suspend fun applyTrackedSelection(trackedIds: Set<Long>) {
        lifeAreaDao.getAll().forEach { area ->
            val shouldTrack = area.id in trackedIds
            if (area.isTracked != shouldTrack) {
                lifeAreaDao.setTracked(area.id, shouldTrack)
            }
        }
    }

    suspend fun create(name: String, emoji: String, colorArgb: Long): Long {
        val sortOrder = lifeAreaDao.count()
        return lifeAreaDao.insert(
            LifeAreaEntity(
                name = name,
                emoji = emoji,
                colorArgb = colorArgb,
                isBuiltIn = false,
                sortOrder = sortOrder,
            ),
        )
    }

    suspend fun update(area: LifeAreaEntity) = lifeAreaDao.update(area)

    suspend fun setTracked(id: Long, tracked: Boolean) = lifeAreaDao.setTracked(id, tracked)

    /** Only user-created areas can be removed; built-ins are untracked instead. */
    suspend fun delete(area: LifeAreaEntity) {
        if (area.isBuiltIn) {
            lifeAreaDao.setTracked(area.id, false)
        } else {
            lifeAreaDao.delete(area)
        }
    }
}
