package com.milestone.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

/**
 * When the weekly reminder actually fires.
 *
 * The arithmetic matters more than it looks: an off-by-one here means the nudge
 * arrives on a Monday morning, which is precisely too late to be useful and
 * exactly the sort of thing nobody notices for six weeks.
 */
class ReflectionReminderScheduleTest {

    private fun delayFrom(isoInstant: String, hour: Int = 19): Duration =
        delayUntilNextReminder(
            ZonedDateTime.ofInstant(Instant.parse(isoInstant), ZoneId.of("UTC")),
            hour,
        )

    @Test
    fun `a midweek evening waits for the coming Sunday`() {
        // Wednesday 4 March 2026, 09:00 UTC.
        val delay = delayFrom("2026-03-04T09:00:00Z")

        // Sunday the 8th at 19:00 is 4 days and 10 hours away.
        assertEquals(Duration.ofDays(4).plusHours(10), delay)
    }

    @Test
    fun `earlier on a Sunday waits only until the evening`() {
        val delay = delayFrom("2026-03-08T09:00:00Z")

        assertEquals(Duration.ofHours(10), delay)
    }

    /** The common trap: "next or same" lands on a Sunday that has already gone. */
    @Test
    fun `later on a Sunday rolls over to the following week`() {
        val delay = delayFrom("2026-03-08T21:30:00Z")

        assertEquals(Duration.ofDays(6).plusHours(21).plusMinutes(30), delay)
    }

    @Test
    fun `exactly on the hour counts as having passed`() {
        val delay = delayFrom("2026-03-08T19:00:00Z")

        // Not zero, and not negative: the next one is a week out.
        assertEquals(Duration.ofDays(7), delay)
    }

    @Test
    fun `the delay is never negative, whatever hour is asked for`() {
        for (hour in 0..23) {
            for (day in 1..7) {
                val delay = delayFrom("2026-03-0" + day + "T12:00:00Z", hour)
                assertTrue("hour=" + hour + " day=" + day, !delay.isNegative)
                assertTrue("hour=" + hour + " day=" + day, delay <= Duration.ofDays(7))
            }
        }
    }
}
