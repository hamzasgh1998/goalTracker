package com.milestone.app.ui.model

import androidx.compose.ui.graphics.Color
import com.milestone.app.data.local.relation.GoalSummary
import com.milestone.app.data.local.relation.GoalWithDetails
import com.milestone.app.data.local.relation.LifeAreaSummary
import com.milestone.app.domain.GoalHealthCalculator
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalHealth
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import java.time.LocalDate

/**
 * A goal, ready to render.
 *
 * Health is resolved during mapping rather than inside a composable: it depends on
 * the current date, and a composable that recomputed it would silently produce a
 * different answer on every recomposition.
 */
data class GoalUi(
    val id: Long,
    val title: String,
    val emoji: String,
    val why: String,
    /** Carried for the edit flow, which needs the id rather than the name. */
    val lifeAreaId: Long,
    val areaName: String,
    val areaColor: Color,
    val progress: Float,
    val completedMilestones: Int,
    val totalMilestones: Int,
    val health: GoalHealth,
    val healthHeadline: String,
    val nextStep: String?,
    val priority: GoalPriority,
    val status: GoalStatus,
    val targetDate: LocalDate?,
    val closingReason: ClosingReason?,
) {
    val progressPercent: Int get() = (progress * 100).toInt()

    /** "7 of 9 milestones", or an invitation when none exist yet. */
    val milestoneLabel: String
        get() = when {
            totalMilestones == 0 -> "No milestones yet"
            else -> "$completedMilestones of $totalMilestones milestones"
        }
}

/** A life area with its orb's numbers. */
data class LifeAreaUi(
    val id: Long,
    val name: String,
    val emoji: String,
    val color: Color,
    val progress: Float,
    val goalCount: Int,
    val milestoneCount: Int,
) {
    val progressPercent: Int get() = (progress * 100).toInt()
}

/** Default when a goal's life area was removed; keeps the UI from showing blanks. */
private const val FALLBACK_AREA_NAME = "Unsorted"
private const val FALLBACK_AREA_COLOR = 0xFF8D867B

fun GoalSummary.toUi(calculator: GoalHealthCalculator): GoalUi {
    val report = calculator.report(
        status = goal.status,
        createdAt = goal.createdAt,
        lastProgressAt = goal.lastProgressAt,
        targetDate = goal.targetDate,
        progress = progress,
    )
    return GoalUi(
        id = goal.id,
        title = goal.title,
        emoji = goal.emoji,
        why = goal.why,
        lifeAreaId = goal.lifeAreaId,
        areaName = areaName ?: FALLBACK_AREA_NAME,
        areaColor = Color(areaColorArgb ?: FALLBACK_AREA_COLOR),
        progress = progress,
        completedMilestones = completedMilestones,
        totalMilestones = totalMilestones,
        health = report.health,
        healthHeadline = report.headline,
        nextStep = nextMilestoneTitle,
        priority = goal.priority,
        status = goal.status,
        targetDate = goal.targetDate,
        closingReason = goal.closingReason,
    )
}

/**
 * Maps the fully loaded goal tree.
 *
 * Counts come from the loaded milestones rather than from a projection, so the
 * header's percentage and the timeline below it can never disagree — they are
 * derived from the same list in the same frame.
 */
fun GoalWithDetails.toUi(calculator: GoalHealthCalculator): GoalUi {
    val all = sortedMilestones
    val total = all.size
    val completed = all.count { it.milestone.isCompleted }
    val progress = if (total == 0) 0f else completed.toFloat() / total

    val report = calculator.report(
        status = goal.status,
        createdAt = goal.createdAt,
        lastProgressAt = goal.lastProgressAt,
        targetDate = goal.targetDate,
        progress = progress,
    )

    return GoalUi(
        id = goal.id,
        title = goal.title,
        emoji = goal.emoji,
        why = goal.why,
        lifeAreaId = goal.lifeAreaId,
        areaName = lifeArea?.name ?: FALLBACK_AREA_NAME,
        areaColor = Color(lifeArea?.colorArgb ?: FALLBACK_AREA_COLOR),
        progress = progress,
        completedMilestones = completed,
        totalMilestones = total,
        health = report.health,
        healthHeadline = report.headline,
        nextStep = all.firstOrNull { !it.milestone.isCompleted }?.milestone?.title,
        priority = goal.priority,
        status = goal.status,
        targetDate = goal.targetDate,
        closingReason = goal.closingReason,
    )
}

fun LifeAreaSummary.toUi(): LifeAreaUi = LifeAreaUi(
    id = area.id,
    name = area.name,
    emoji = area.emoji,
    color = Color(area.colorArgb),
    progress = progress,
    goalCount = goalCount,
    milestoneCount = milestoneCount,
)
