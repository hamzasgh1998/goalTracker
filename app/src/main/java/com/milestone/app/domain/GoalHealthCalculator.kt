package com.milestone.app.domain

import com.milestone.app.core.time.AppClock
import com.milestone.app.domain.model.GoalHealth
import com.milestone.app.domain.model.GoalStatus
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/** A health verdict plus the plain-language fact behind it. */
data class GoalHealthReport(
    val health: GoalHealth,
    /** One short sentence stating what was observed. Never evaluative. */
    val headline: String,
    val daysSinceProgress: Int,
    val daysUntilTarget: Int?,
)

/**
 * Works out whether a goal has momentum, using only dates already on the row.
 *
 * Two independent signals are combined: how long the goal has been quiet, and
 * whether its target date is close relative to how much is left. Quietness alone
 * decides the worst case, because a goal nobody has touched in three weeks is
 * stalled whether or not it has a deadline.
 *
 * The thresholds are deliberately forgiving. A week of silence is a normal week in
 * a person's life, not a problem, so nothing is flagged before day ten.
 */
@Singleton
class GoalHealthCalculator @Inject constructor(
    private val clock: AppClock,
) {

    fun report(
        status: GoalStatus,
        createdAt: Instant,
        lastProgressAt: Instant?,
        targetDate: LocalDate?,
        progress: Float,
    ): GoalHealthReport {
        val today = clock.today()
        // A goal that has never moved is measured from when it was created, which
        // gives a brand-new goal a full grace period rather than instant staleness.
        val since = lastProgressAt ?: createdAt
        val quietDays = ChronoUnit.DAYS
            .between(since.atZone(clock.zone()).toLocalDate(), today)
            .coerceAtLeast(0)
            .toInt()
        val daysUntilTarget = targetDate?.let {
            ChronoUnit.DAYS.between(today, it).toInt()
        }

        if (status == GoalStatus.Paused) {
            return GoalHealthReport(
                health = GoalHealth.Paused,
                headline = "Paused — waiting for you, no rush.",
                daysSinceProgress = quietDays,
                daysUntilTarget = daysUntilTarget,
            )
        }

        if (status == GoalStatus.Completed || status == GoalStatus.Archived) {
            return GoalHealthReport(
                health = GoalHealth.Healthy,
                headline = if (status == GoalStatus.Completed) "Finished." else "Closed chapter.",
                daysSinceProgress = quietDays,
                daysUntilTarget = daysUntilTarget,
            )
        }

        val quietHealth = when {
            quietDays >= STALLED_DAYS -> GoalHealth.Stalled
            quietDays >= AT_RISK_DAYS -> GoalHealth.AtRisk
            else -> GoalHealth.Healthy
        }

        val deadlineHealth = when {
            daysUntilTarget == null -> GoalHealth.Healthy
            // Past its date and not finished. Worth surfacing, not worth alarm.
            daysUntilTarget < 0 -> GoalHealth.AtRisk
            // Close to the date with most of the work still to do.
            daysUntilTarget <= DEADLINE_WINDOW_DAYS && progress < DEADLINE_PROGRESS_FLOOR ->
                GoalHealth.AtRisk
            else -> GoalHealth.Healthy
        }

        val health = worstOf(quietHealth, deadlineHealth)

        return GoalHealthReport(
            health = health,
            headline = headlineFor(
                health = health,
                quietDays = quietDays,
                daysUntilTarget = daysUntilTarget,
                hasEverProgressed = lastProgressAt != null,
            ),
            daysSinceProgress = quietDays,
            daysUntilTarget = daysUntilTarget,
        )
    }

    private fun worstOf(a: GoalHealth, b: GoalHealth): GoalHealth =
        if (severity(a) >= severity(b)) a else b

    private fun severity(health: GoalHealth): Int = when (health) {
        GoalHealth.Healthy -> 0
        GoalHealth.Paused -> 1
        GoalHealth.AtRisk -> 2
        GoalHealth.Stalled -> 3
    }

    /**
     * Copy for each state.
     *
     * Every string states an observation ("no progress in 16 days") rather than a
     * verdict about the person. There is no "you're falling behind" anywhere in
     * this app, by design.
     *
     * [hasEverProgressed] matters because quiet time is measured from creation
     * when a goal has never moved. Without it, a goal created five minutes ago
     * reads as "moved forward today", which is simply untrue and is the kind of
     * small lie that makes a progress tracker untrustworthy.
     */
    private fun headlineFor(
        health: GoalHealth,
        quietDays: Int,
        daysUntilTarget: Int?,
        hasEverProgressed: Boolean,
    ): String = when (health) {
        GoalHealth.Healthy -> when {
            !hasEverProgressed -> "Not started yet."
            quietDays == 0 -> "Moved forward today."
            quietDays == 1 -> "Moved forward yesterday."
            daysUntilTarget != null && daysUntilTarget in 0..DEADLINE_WINDOW_DAYS ->
                "On track, $daysUntilTarget days to go."
            else -> "Moving steadily."
        }

        GoalHealth.AtRisk -> when {
            daysUntilTarget != null && daysUntilTarget < 0 ->
                "Target date passed ${-daysUntilTarget} days ago."
            daysUntilTarget != null && daysUntilTarget <= DEADLINE_WINDOW_DAYS ->
                "$daysUntilTarget days left, and most of it still ahead."
            !hasEverProgressed -> "Still not started, $quietDays days on."
            else -> "Quiet for $quietDays days."
        }

        GoalHealth.Stalled ->
            if (hasEverProgressed) {
                "No progress in $quietDays days."
            } else {
                "Not started since you created it $quietDays days ago."
            }

        GoalHealth.Paused -> "Paused — waiting for you, no rush."
    }

    private companion object {
        /** A week of silence is just life; flag only after ten days. */
        const val AT_RISK_DAYS = 10
        const val STALLED_DAYS = 21

        /** How close a target date has to be before progress is judged against it. */
        const val DEADLINE_WINDOW_DAYS = 14
        const val DEADLINE_PROGRESS_FLOOR = 0.5f
    }
}
