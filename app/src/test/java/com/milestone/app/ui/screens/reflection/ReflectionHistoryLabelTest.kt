package com.milestone.app.ui.screens.reflection

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * The week ranges on the reflection history.
 *
 * Asserted structurally rather than against literal month names: the labels are
 * localised on purpose, so a hard-coded "October" only passes on an English
 * machine.
 */
class ReflectionHistoryLabelTest {

    private val today: LocalDate = LocalDate.of(2026, 10, 8)

    /** Monday 5 October 2026 to Sunday the 11th: one month, one year. */
    @Test
    fun `a week inside one month names the month once`() {
        val label = rangeLabel(LocalDate.of(2026, 10, 5), today)

        assertTrue(label, label.startsWith("5 – 11 "))
        assertFalse("the current year is implied, not printed", label.endsWith("2026"))
    }

    /** Monday 28 September to Sunday 4 October: the month has to be said twice. */
    @Test
    fun `a week straddling two months names both`() {
        val label = rangeLabel(LocalDate.of(2026, 9, 28), today)
        val parts = label.split(" – ")

        assertEquals(2, parts.size)
        assertTrue(label, parts[0].startsWith("28 "))
        assertTrue(label, parts[1].startsWith("4 "))
        // Two different months, so the first half cannot be a bare day number.
        assertTrue(label, parts[0].length > 2)
    }

    @Test
    fun `an older year is stated`() {
        val label = rangeLabel(LocalDate.of(2025, 10, 6), today)

        assertTrue(label, label.startsWith("6 – 12 "))
        assertTrue(label, label.endsWith(" 2025"))
    }

    /**
     * A week that begins in December and ends in January.
     *
     * The year is decided by the end of the week, not its start, so this one
     * counts as the current year and stays unlabelled — the alternative reads as
     * "29 December – 4 January 2025", which is wrong about the January.
     */
    @Test
    fun `a week spanning new year is dated by the year it ends in`() {
        val label = rangeLabel(LocalDate.of(2025, 12, 29), today)

        val parts = label.split(" – ")
        assertEquals(2, parts.size)
        assertTrue(label, parts[0].startsWith("29 "))
        assertTrue(label, parts[1].startsWith("4 "))
        assertFalse("ends inside the current year, so no year is printed", label.endsWith("2026"))
        assertFalse("and certainly not the year it started in", label.endsWith("2025"))
    }
}
