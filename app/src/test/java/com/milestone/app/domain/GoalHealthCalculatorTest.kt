package com.milestone.app.domain

import com.milestone.app.core.time.AppClock
import com.milestone.app.domain.model.GoalHealth
import com.milestone.app.domain.model.GoalStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * Goal health is the one piece of inference in the app, and it is entirely
 * date-driven — which makes it both worth testing and impossible to test against
 * a real clock. [FixedClock] pins "now" so the thresholds can be exercised
 * directly.
 */
class GoalHealthCalculatorTest {

    private val zone: ZoneId = ZoneId.of("UTC")
    private val today: LocalDate = LocalDate.of(2026, 10, 8)
    private val clock = FixedClock(today, zone)
    private val calculator = GoalHealthCalculator(clock)

    private fun daysAgo(days: Long): Instant =
        clock.now().minus(days, ChronoUnit.DAYS)

    @Test
    fun `a goal created today with no progress reads as not started, not as progress`() {
        val report = calculator.report(
            status = GoalStatus.Active,
            createdAt = clock.now(),
            lastProgressAt = null,
            targetDate = null,
            progress = 0f,
        )

        assertEquals(GoalHealth.Healthy, report.health)
        // The bug this guards: measuring quiet time from createdAt made a brand
        // new goal claim it had "moved forward today".
        assertEquals("Not started yet.", report.headline)
    }

    @Test
    fun `progress today is reported as today`() {
        val report = calculator.report(
            status = GoalStatus.Active,
            createdAt = daysAgo(30),
            lastProgressAt = clock.now(),
            targetDate = null,
            progress = 0.5f,
        )

        assertEquals(GoalHealth.Healthy, report.health)
        assertEquals("Moved forward today.", report.headline)
    }

    @Test
    fun `a week of silence is still healthy`() {
        val report = calculator.report(
            status = GoalStatus.Active,
            createdAt = daysAgo(40),
            lastProgressAt = daysAgo(7),
            targetDate = null,
            progress = 0.4f,
        )

        assertEquals(GoalHealth.Healthy, report.health)
    }

    @Test
    fun `ten quiet days is at risk`() {
        val report = calculator.report(
            status = GoalStatus.Active,
            createdAt = daysAgo(40),
            lastProgressAt = daysAgo(10),
            targetDate = null,
            progress = 0.4f,
        )

        assertEquals(GoalHealth.AtRisk, report.health)
        assertEquals(10, report.daysSinceProgress)
    }

    @Test
    fun `three weeks of silence is stalled`() {
        val report = calculator.report(
            status = GoalStatus.Active,
            createdAt = daysAgo(60),
            lastProgressAt = daysAgo(26),
            targetDate = null,
            progress = 0.4f,
        )

        assertEquals(GoalHealth.Stalled, report.health)
        assertTrue(report.headline.contains("26 days"))
    }

    @Test
    fun `a near deadline with little done is at risk even when recently touched`() {
        val report = calculator.report(
            status = GoalStatus.Active,
            createdAt = daysAgo(30),
            lastProgressAt = clock.now(),
            targetDate = today.plusDays(5),
            progress = 0.2f,
        )

        assertEquals(GoalHealth.AtRisk, report.health)
        assertEquals(5, report.daysUntilTarget)
    }

    @Test
    fun `a near deadline with most of the work done stays healthy`() {
        val report = calculator.report(
            status = GoalStatus.Active,
            createdAt = daysAgo(30),
            lastProgressAt = clock.now(),
            targetDate = today.plusDays(5),
            progress = 0.9f,
        )

        assertEquals(GoalHealth.Healthy, report.health)
    }

    @Test
    fun `an overdue goal is at risk, never worse for being late alone`() {
        val report = calculator.report(
            status = GoalStatus.Active,
            createdAt = daysAgo(60),
            lastProgressAt = daysAgo(1),
            targetDate = today.minusDays(3),
            progress = 0.8f,
        )

        assertEquals(GoalHealth.AtRisk, report.health)
        assertTrue(report.headline.contains("3 days ago"))
    }

    @Test
    fun `a paused goal carries no urgency however long it has been quiet`() {
        val report = calculator.report(
            status = GoalStatus.Paused,
            createdAt = daysAgo(200),
            lastProgressAt = daysAgo(180),
            targetDate = today.minusDays(60),
            progress = 0.1f,
        )

        assertEquals(GoalHealth.Paused, report.health)
        assertTrue(report.health.isPaused)
    }

    @Test
    fun `quiet time wins over a comfortable deadline`() {
        val report = calculator.report(
            status = GoalStatus.Active,
            createdAt = daysAgo(90),
            lastProgressAt = daysAgo(30),
            targetDate = today.plusDays(300),
            progress = 0.5f,
        )

        assertEquals(GoalHealth.Stalled, report.health)
    }

    private class FixedClock(
        private val today: LocalDate,
        private val zone: ZoneId,
    ) : AppClock {
        override fun now(): Instant = today.atStartOfDay(zone).toInstant()
        override fun today(): LocalDate = today
        override fun zone(): ZoneId = zone
    }
}
