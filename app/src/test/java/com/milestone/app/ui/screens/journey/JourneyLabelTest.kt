package com.milestone.app.ui.screens.journey

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The Journey's date headings.
 *
 * These coarsen as they go back, so the boundaries are worth pinning: an
 * off-by-one here is the difference between "7 days ago" and "Last week" on the
 * same row, which is exactly the sort of wobble that makes a timeline feel
 * untrustworthy.
 */
class JourneyLabelTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 8)

    private fun labelFor(daysAgo: Long) = relativeLabel(today.minusDays(daysAgo), today)

    @Test
    fun `today and yesterday are named, not counted`() {
        assertEquals("Today", labelFor(0))
        assertEquals("Yesterday", labelFor(1))
    }

    @Test
    fun `a future date never reads as being in the past`() {
        assertEquals("Today", relativeLabel(today.plusDays(3), today))
    }

    @Test
    fun `days are counted up to the week boundary`() {
        assertEquals("2 days ago", labelFor(2))
        assertEquals("6 days ago", labelFor(6))
    }

    @Test
    fun `the week boundary switches to weeks`() {
        assertEquals("Last week", labelFor(7))
        assertEquals("Last week", labelFor(13))
        assertEquals("2 weeks ago", labelFor(14))
        assertEquals("4 weeks ago", labelFor(30))
    }

    @Test
    fun `the month boundary switches to months`() {
        assertEquals("Last month", labelFor(31))
        assertEquals("Last month", labelFor(61))
        assertEquals("2 months ago", labelFor(62))
    }

    @Test
    fun `beyond a year reads in years`() {
        assertEquals("Last year", labelFor(365))
        assertEquals("2 years ago", labelFor(730))
    }

    /**
     * Asserted structurally rather than against literal month names: the labels
     * are localised on purpose, so a hard-coded "September" only passes on an
     * English machine and fails on, say, a French one.
     */
    @Test
    fun `the absolute label adds a year only when it is not the current one`() {
        val thisYear = absoluteLabel(LocalDate.of(2026, 9, 14), today)
        val lastYear = absoluteLabel(LocalDate.of(2025, 9, 14), today)

        assertTrue(thisYear.startsWith("14 "))
        assertFalse("the current year is implied, not printed", thisYear.endsWith("2026"))

        assertTrue(lastYear.startsWith("14 "))
        assertTrue("an older year has to be stated", lastYear.endsWith(" 2025"))
        // Same day and month either way; only the year differs.
        assertEquals(lastYear.removeSuffix(" 2025"), thisYear)
    }
}
