package com.milestone.app.data.local.relation

import androidx.room.Embedded
import androidx.room.Relation
import com.milestone.app.data.local.entity.GoalEntity
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.local.entity.MilestoneEntity
import com.milestone.app.data.local.entity.QuestEntity

/** A milestone together with its optional quests, ordered. */
data class MilestoneWithQuests(
    @Embedded val milestone: MilestoneEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "milestoneId",
    )
    val quests: List<QuestEntity>,
) {
    val sortedQuests: List<QuestEntity> get() = quests.sortedBy { it.sortOrder }
}

/**
 * Everything the goal detail screen needs, in one observed query.
 *
 * Loading the full milestone + quest tree is deliberate here: a single goal's tree
 * is tens of rows at most, and having it arrive atomically means the timeline never
 * renders a half-updated state after a completion.
 */
data class GoalWithDetails(
    @Embedded val goal: GoalEntity,
    @Relation(
        parentColumn = "lifeAreaId",
        entityColumn = "id",
    )
    val lifeArea: LifeAreaEntity?,
    @Relation(
        entity = MilestoneEntity::class,
        parentColumn = "id",
        entityColumn = "goalId",
    )
    val milestones: List<MilestoneWithQuests>,
) {
    val sortedMilestones: List<MilestoneWithQuests>
        get() = milestones.sortedBy { it.milestone.sortOrder }
}

/**
 * A goal flattened for list rendering.
 *
 * Counts arrive as scalar subqueries rather than by loading every milestone: Home
 * and the Goals list only need "7 of 9", and fetching the whole tree per row is
 * what makes list screens janky once a user has real history.
 */
data class GoalSummary(
    @Embedded val goal: GoalEntity,
    val areaName: String?,
    val areaEmoji: String?,
    val areaColorArgb: Long?,
    val totalMilestones: Int,
    val completedMilestones: Int,
    /** Title of the first incomplete milestone — the "next step" on Home. */
    val nextMilestoneTitle: String?,
) {
    /** 0f..1f. A goal with no milestones yet reads as 0 rather than complete. */
    val progress: Float
        get() = if (totalMilestones == 0) 0f else completedMilestones.toFloat() / totalMilestones
}

/** A life area with the counts its orb displays. */
data class LifeAreaSummary(
    @Embedded val area: LifeAreaEntity,
    val goalCount: Int,
    val milestoneCount: Int,
    val completedMilestoneCount: Int,
) {
    val progress: Float
        get() = if (milestoneCount == 0) 0f else completedMilestoneCount.toFloat() / milestoneCount
}
