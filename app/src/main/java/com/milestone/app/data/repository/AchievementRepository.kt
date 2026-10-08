package com.milestone.app.data.repository

import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.dao.AchievementDao
import com.milestone.app.data.local.dao.GoalDao
import com.milestone.app.data.local.dao.MilestoneDao
import com.milestone.app.data.local.dao.ProgressEventDao
import com.milestone.app.data.local.dao.ReflectionDao
import com.milestone.app.data.local.dao.SeasonDao
import com.milestone.app.data.local.entity.AchievementEntity
import com.milestone.app.data.local.entity.ProgressEventEntity
import com.milestone.app.domain.achievement.AchievementCatalog
import com.milestone.app.domain.achievement.AchievementDefinition
import com.milestone.app.domain.model.ProgressEventType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/** A catalogue entry paired with whether (and when) the user earned it. */
data class AchievementState(
    val definition: AchievementDefinition,
    val unlockedAt: java.time.Instant?,
    val isNew: Boolean,
) {
    val isUnlocked: Boolean get() = unlockedAt != null
}

@Singleton
class AchievementRepository @Inject constructor(
    private val achievementDao: AchievementDao,
    private val goalDao: GoalDao,
    private val milestoneDao: MilestoneDao,
    private val reflectionDao: ReflectionDao,
    private val seasonDao: SeasonDao,
    private val progressEventDao: ProgressEventDao,
    private val clock: AppClock,
) {

    /** The whole catalogue, locked entries included, in display order. */
    fun observeAll(): Flow<List<AchievementState>> =
        achievementDao.observeAll().map { unlocks ->
            val byKey = unlocks.associateBy { it.achievementKey }
            AchievementCatalog.all.map { definition ->
                val unlock = byKey[definition.key]
                AchievementState(
                    definition = definition,
                    unlockedAt = unlock?.unlockedAt,
                    isNew = unlock?.isNew == true,
                )
            }
        }

    /** Unlocks not yet celebrated, so the UI can show them once and move on. */
    fun observeUnseen(): Flow<List<AchievementState>> =
        achievementDao.observeUnseen().map { unlocks ->
            unlocks.mapNotNull { unlock ->
                AchievementCatalog.find(unlock.achievementKey)?.let { definition ->
                    AchievementState(definition, unlock.unlockedAt, isNew = true)
                }
            }
        }

    suspend fun markSeen(key: String) = achievementDao.markSeen(key)

    suspend fun markAllSeen() = achievementDao.markAllSeen()

    /**
     * Awards [key] if it hasn't been awarded already, and writes it to the journey.
     *
     * Returns the definition when this call is what unlocked it, so the caller can
     * trigger the celebration animation — and `null` on repeat calls, which keeps
     * the whole thing safe to invoke from anywhere.
     */
    suspend fun unlock(key: String): AchievementDefinition? {
        val definition = AchievementCatalog.find(key) ?: return null
        if (achievementDao.isUnlocked(key)) return null

        val now = clock.now()
        val rowId = achievementDao.insert(
            AchievementEntity(achievementKey = key, unlockedAt = now, isNew = true),
        )
        // IGNORE conflict strategy returns -1 when the row already existed, which
        // means another caller won the race. Don't double-record the event.
        if (rowId == -1L) return null

        progressEventDao.insert(
            ProgressEventEntity(
                type = ProgressEventType.AchievementUnlocked,
                title = definition.title,
                subtitle = definition.description,
                occurredAt = now,
            ),
        )
        return definition
    }

    /**
     * Re-checks every count-based achievement and awards whatever now qualifies.
     *
     * Written to be called liberally after any mutation. It is idempotent and reads
     * only `COUNT(*)` queries, so the cost is negligible compared with getting the
     * trigger conditions right at a dozen separate call sites.
     *
     * Achievements that depend on a specific *act* rather than a total — Comeback,
     * Honest Close, Focus — are unlocked directly via [unlock] where that act
     * happens, because no amount of counting can tell you a goal was resumed.
     */
    suspend fun evaluateCounters(): List<AchievementDefinition> {
        val unlocked = mutableListOf<AchievementDefinition>()

        suspend fun award(key: String) {
            unlock(key)?.let(unlocked::add)
        }

        if (goalDao.count() >= 1) award(AchievementCatalog.FIRST_STEP)
        if (goalDao.distinctAreaCount() >= EXPLORER_AREAS) award(AchievementCatalog.EXPLORER)
        if (milestoneDao.completedCount() >= MOMENTUM_MILESTONES) {
            award(AchievementCatalog.MOMENTUM)
        }
        if (goalDao.completedCount() >= 1) award(AchievementCatalog.FINISHER)
        if (reflectionDao.count() >= WITNESS_REFLECTIONS) award(AchievementCatalog.WITNESS)
        if (seasonDao.getActive() != null) award(AchievementCatalog.NEW_CHAPTER)

        return unlocked
    }

    private companion object {
        const val EXPLORER_AREAS = 5
        const val MOMENTUM_MILESTONES = 10
        const val WITNESS_REFLECTIONS = 4
    }
}
