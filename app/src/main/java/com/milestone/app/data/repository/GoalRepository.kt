package com.milestone.app.data.repository

import androidx.room.withTransaction
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.MilestoneDatabase
import com.milestone.app.data.local.dao.GoalDao
import com.milestone.app.data.local.dao.LifeAreaDao
import com.milestone.app.data.local.dao.MilestoneDao
import com.milestone.app.data.local.dao.ProgressEventDao
import com.milestone.app.data.local.dao.QuestDao
import com.milestone.app.data.local.dao.SeasonDao
import com.milestone.app.data.local.entity.GoalEntity
import com.milestone.app.data.local.entity.MilestoneEntity
import com.milestone.app.data.local.entity.ProgressEventEntity
import com.milestone.app.data.local.entity.QuestEntity
import com.milestone.app.data.local.relation.GoalSummary
import com.milestone.app.data.local.relation.GoalWithDetails
import com.milestone.app.domain.achievement.AchievementCatalog
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.domain.model.ProgressEventType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** Everything collected by the goal-creation flow, before anything is persisted. */
data class GoalDraft(
    val title: String,
    val why: String,
    val lifeAreaId: Long,
    val emoji: String,
    val targetDate: LocalDate?,
    val priority: GoalPriority,
    val seasonId: Long?,
    /** In the order the user arranged them. Blank entries are dropped. */
    val milestoneTitles: List<String>,
)

/**
 * Writes for goals, milestones and quests.
 *
 * Every mutation that the user perceives as one action is wrapped in a single Room
 * transaction, because each one touches several tables: ticking a milestone also
 * stamps the goal's `lastProgressAt`, appends a journey event, and may flip the
 * goal's own status. Splitting those would let a crash leave a goal showing 9-of-9
 * milestones while still marked Active.
 *
 * Achievement checks deliberately run *after* the transaction commits — they are
 * read-only derivations of committed state, and holding the write lock while
 * running nine counting queries would serialise the UI for no benefit.
 */
@Singleton
class GoalRepository @Inject constructor(
    private val database: MilestoneDatabase,
    private val goalDao: GoalDao,
    private val milestoneDao: MilestoneDao,
    private val questDao: QuestDao,
    private val lifeAreaDao: LifeAreaDao,
    private val progressEventDao: ProgressEventDao,
    private val seasonDao: SeasonDao,
    private val achievements: AchievementRepository,
    private val clock: AppClock,
) {

    // ---------------------------------------------------------------- reads

    fun observeOpenSummaries(): Flow<List<GoalSummary>> = goalDao.observeOpenSummaries()

    fun observeClosedSummaries(): Flow<List<GoalSummary>> = goalDao.observeClosedSummaries()

    fun observeSummariesByArea(areaId: Long): Flow<List<GoalSummary>> =
        goalDao.observeSummariesByArea(areaId)

    fun observeDetails(goalId: Long): Flow<GoalWithDetails?> = goalDao.observeDetails(goalId)

    /** Goals in whichever season is currently active; empty when there is none. */
    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeActiveSeasonSummaries(): Flow<List<GoalSummary>> =
        seasonDao.observeActive().flatMapLatest { season ->
            if (season == null) flowOf(emptyList()) else goalDao.observeSeasonSummaries(season.id)
        }

    suspend fun get(goalId: Long): GoalEntity? = goalDao.get(goalId)

    // ------------------------------------------------------------ goal writes

    /** Creates a goal with its initial milestones, as one unit. */
    suspend fun createGoal(draft: GoalDraft): Long {
        val now = clock.now()
        val areaColor = lifeAreaDao.getAll().firstOrNull { it.id == draft.lifeAreaId }?.colorArgb

        val goalId = database.withTransaction {
            val id = goalDao.insert(
                GoalEntity(
                    title = draft.title.trim(),
                    why = draft.why.trim(),
                    lifeAreaId = draft.lifeAreaId,
                    seasonId = draft.seasonId,
                    emoji = draft.emoji,
                    targetDate = draft.targetDate,
                    priority = draft.priority,
                    createdAt = now,
                    lastProgressAt = null,
                ),
            )

            val milestones = draft.milestoneTitles
                .map(String::trim)
                .filter(String::isNotEmpty)
                .mapIndexed { index, title ->
                    MilestoneEntity(
                        goalId = id,
                        title = title,
                        sortOrder = index,
                        createdAt = now,
                    )
                }
            if (milestones.isNotEmpty()) milestoneDao.insertAll(milestones)

            progressEventDao.insert(
                ProgressEventEntity(
                    type = ProgressEventType.GoalCreated,
                    title = draft.title.trim(),
                    subtitle = "A new goal begins",
                    goalId = id,
                    accentArgb = areaColor,
                    occurredAt = now,
                ),
            )
            id
        }

        achievements.evaluateCounters()
        return goalId
    }

    suspend fun updateGoal(goal: GoalEntity) = goalDao.update(goal)

    /**
     * Applies the user's edits to a goal's own fields.
     *
     * Reads-then-copies instead of accepting a whole [GoalEntity] from the UI.
     * Status, the progress stamps and the closing details are all derived
     * elsewhere; letting an edit screen round-trip the entire row is how a form
     * that was opened ten minutes ago ends up writing back a stale
     * `lastProgressAt` and resetting the goal's health.
     *
     * A blank title is rejected rather than saved, so a cleared field cannot
     * leave a goal nameless everywhere it appears.
     */
    suspend fun editGoal(
        goalId: Long,
        title: String,
        why: String,
        emoji: String,
        lifeAreaId: Long,
        targetDate: LocalDate?,
        priority: GoalPriority,
    ) {
        val existing = goalDao.get(goalId) ?: return
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        goalDao.update(
            existing.copy(
                title = cleanTitle,
                why = why.trim(),
                emoji = emoji,
                lifeAreaId = lifeAreaId,
                targetDate = targetDate,
                priority = priority,
            ),
        )
    }

    suspend fun setPriority(goalId: Long, priority: GoalPriority) =
        goalDao.setPriority(goalId, priority)

    suspend fun setSeason(goalId: Long, seasonId: Long?) = goalDao.setSeason(goalId, seasonId)

    suspend fun pause(goalId: Long) {
        val goal = goalDao.get(goalId) ?: return
        database.withTransaction {
            goalDao.setStatus(goalId, GoalStatus.Paused)
            progressEventDao.insert(
                ProgressEventEntity(
                    type = ProgressEventType.GoalPaused,
                    title = goal.title,
                    subtitle = "Set down for now",
                    goalId = goalId,
                    occurredAt = clock.now(),
                ),
            )
        }
    }

    /**
     * Resumes a paused goal.
     *
     * This is the only path to the Comeback achievement: coming back to something
     * you had set down is an act, and no count of rows can detect it after the
     * fact.
     */
    suspend fun resume(goalId: Long) {
        val goal = goalDao.get(goalId) ?: return
        val wasPaused = goal.status == GoalStatus.Paused
        val now = clock.now()

        database.withTransaction {
            goalDao.setStatus(goalId, GoalStatus.Active)
            goalDao.touchProgress(goalId, now)
            progressEventDao.insert(
                ProgressEventEntity(
                    type = ProgressEventType.GoalResumed,
                    title = goal.title,
                    subtitle = "Picked back up",
                    goalId = goalId,
                    occurredAt = now,
                ),
            )
        }

        if (wasPaused) achievements.unlock(AchievementCatalog.COMEBACK)
    }

    /** Marks a goal finished by hand, independent of its milestone count. */
    suspend fun complete(goalId: Long) {
        val goal = goalDao.get(goalId) ?: return
        val now = clock.now()
        database.withTransaction {
            goalDao.markCompleted(goalId, now)
            progressEventDao.insert(
                ProgressEventEntity(
                    type = ProgressEventType.GoalCompleted,
                    title = goal.title,
                    subtitle = "Goal complete",
                    goalId = goalId,
                    occurredAt = now,
                ),
            )
        }
        achievements.evaluateCounters()
    }

    /** Closes a chapter: archived with a reason, never deleted. */
    suspend fun archive(goalId: Long, reason: ClosingReason, note: String?) {
        val goal = goalDao.get(goalId) ?: return
        val now = clock.now()
        database.withTransaction {
            goalDao.archive(goalId, now, reason, note?.trim()?.ifEmpty { null })
            progressEventDao.insert(
                ProgressEventEntity(
                    type = ProgressEventType.GoalArchived,
                    title = goal.title,
                    subtitle = reason.label,
                    goalId = goalId,
                    occurredAt = now,
                ),
            )
        }
        achievements.unlock(AchievementCatalog.HONEST_CLOSE)
    }

    /** Brings a closed chapter back into the active set. */
    suspend fun restore(goalId: Long) {
        goalDao.restore(goalId, clock.now())
    }

    /**
     * Hard-deletes a goal and its whole tree.
     *
     * Only reachable from an explicit, confirmed "delete permanently" action —
     * archiving is what the rest of the app does.
     */
    suspend fun deleteForever(goalId: Long) = goalDao.deleteById(goalId)

    // ------------------------------------------------------- milestone writes

    suspend fun addMilestone(
        goalId: Long,
        title: String,
        dueDate: LocalDate? = null,
        note: String? = null,
    ): Long {
        val now = clock.now()
        return database.withTransaction {
            val id = milestoneDao.insert(
                MilestoneEntity(
                    goalId = goalId,
                    title = title.trim(),
                    note = note?.trim()?.ifEmpty { null },
                    sortOrder = milestoneDao.maxSortOrder(goalId) + 1,
                    dueDate = dueDate,
                    createdAt = now,
                ),
            )
            progressEventDao.insert(
                ProgressEventEntity(
                    type = ProgressEventType.MilestoneAdded,
                    title = title.trim(),
                    subtitle = goalDao.get(goalId)?.title,
                    goalId = goalId,
                    milestoneId = id,
                    occurredAt = now,
                ),
            )
            id
        }
    }

    /** The row as it currently stands, for seeding an edit form. */
    suspend fun getMilestone(milestoneId: Long): MilestoneEntity? = milestoneDao.get(milestoneId)

    /**
     * Applies the user's edits to one milestone.
     *
     * Title, note and due date move together because that is how the edit sheet
     * presents them — three separate read-modify-write pairs for one "Save" would
     * be three chances to half-apply an edit.
     *
     * A no-op when the row is already gone, so an edit racing a delete fails
     * quietly instead of recreating what the user just removed, and a blank title
     * is refused rather than saved.
     */
    suspend fun editMilestone(
        milestoneId: Long,
        title: String,
        note: String?,
        dueDate: LocalDate?,
    ) {
        val existing = milestoneDao.get(milestoneId) ?: return
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return

        milestoneDao.update(
            existing.copy(
                title = cleanTitle,
                note = note?.trim()?.ifEmpty { null },
                dueDate = dueDate,
            ),
        )
    }

    suspend fun deleteMilestone(milestoneId: Long) = milestoneDao.deleteById(milestoneId)

    suspend fun reorderMilestones(orderedIds: List<Long>) = milestoneDao.reorder(orderedIds)

    /**
     * Ticks or un-ticks a milestone, and keeps the parent goal consistent.
     *
     * The goal's own status is *derived* here rather than left to the user: ticking
     * the last outstanding milestone completes the goal, and un-ticking one on a
     * completed goal reopens it. Without the reverse case, correcting a mis-tap
     * would leave a goal permanently marked done.
     */
    suspend fun setMilestoneCompleted(milestoneId: Long, completed: Boolean) {
        val milestone = milestoneDao.get(milestoneId) ?: return
        val now = clock.now()

        database.withTransaction {
            milestoneDao.setCompleted(milestoneId, completed, if (completed) now else null)
            goalDao.touchProgress(milestone.goalId, now)

            val goal = goalDao.get(milestone.goalId)
            if (completed) {
                progressEventDao.insert(
                    ProgressEventEntity(
                        type = ProgressEventType.MilestoneCompleted,
                        title = milestone.title,
                        subtitle = goal?.title,
                        goalId = milestone.goalId,
                        milestoneId = milestoneId,
                        occurredAt = now,
                    ),
                )
            }

            val remaining = milestoneDao.openCountForGoal(milestone.goalId)
            val total = milestoneDao.countForGoal(milestone.goalId)

            when {
                remaining == 0 && total > 0 && goal?.status == GoalStatus.Active -> {
                    goalDao.markCompleted(milestone.goalId, now)
                    progressEventDao.insert(
                        ProgressEventEntity(
                            type = ProgressEventType.GoalCompleted,
                            title = goal.title,
                            subtitle = "Every milestone done",
                            goalId = goal.id,
                            occurredAt = now,
                        ),
                    )
                }

                remaining > 0 && goal?.status == GoalStatus.Completed -> {
                    goalDao.markReopened(milestone.goalId, now)
                }
            }
        }

        achievements.evaluateCounters()
    }

    // ----------------------------------------------------------- quest writes

    fun observeQuests(milestoneId: Long): Flow<List<QuestEntity>> =
        questDao.observeForMilestone(milestoneId)

    suspend fun getQuest(questId: Long): QuestEntity? = questDao.get(questId)

    suspend fun addQuest(milestoneId: Long, title: String): Long = questDao.insert(
        QuestEntity(
            milestoneId = milestoneId,
            title = title.trim(),
            sortOrder = questDao.maxSortOrder(milestoneId) + 1,
            createdAt = clock.now(),
        ),
    )

    /**
     * Renames a quest.
     *
     * Same read-then-copy and same blank-title refusal as [editMilestone]: a
     * cleared field must not leave an unnamed row sitting under a milestone.
     */
    suspend fun updateQuestTitle(questId: Long, title: String) {
        val existing = questDao.get(questId) ?: return
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) return
        questDao.update(existing.copy(title = cleanTitle))
    }

    suspend fun deleteQuest(questId: Long) = questDao.deleteById(questId)

    suspend fun reorderQuests(orderedIds: List<Long>) = questDao.reorder(orderedIds)

    /**
     * Ticks a quest. Touches the parent goal's progress stamp but never its
     * percentage — progress is measured in milestones only, so a user who splits
     * work into quests doesn't see their goal's completion number move differently
     * from a user who didn't.
     */
    suspend fun setQuestCompleted(questId: Long, completed: Boolean) {
        val quest = questDao.get(questId) ?: return
        val milestone = milestoneDao.get(quest.milestoneId) ?: return
        val now = clock.now()

        database.withTransaction {
            questDao.setCompleted(questId, completed, if (completed) now else null)
            goalDao.touchProgress(milestone.goalId, now)
            if (completed) {
                progressEventDao.insert(
                    ProgressEventEntity(
                        type = ProgressEventType.QuestCompleted,
                        title = quest.title,
                        subtitle = milestone.title,
                        goalId = milestone.goalId,
                        milestoneId = milestone.id,
                        questId = questId,
                        occurredAt = now,
                    ),
                )
            }
        }
    }
}
