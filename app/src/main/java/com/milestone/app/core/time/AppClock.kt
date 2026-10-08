package com.milestone.app.core.time

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Indirection over "now".
 *
 * Goal health, weekly reflections and the journey timeline are all defined in terms
 * of elapsed time, which makes them untestable against a hard-coded
 * `Instant.now()`. Everything in the app asks this instead.
 */
interface AppClock {
    fun now(): Instant

    fun today(): LocalDate

    fun zone(): ZoneId

    /** The Monday of the week containing [date]. The key for weekly reflections. */
    fun weekStart(date: LocalDate = today()): LocalDate =
        date.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
}

@Singleton
class SystemAppClock @Inject constructor() : AppClock {
    override fun now(): Instant = Instant.now()

    override fun today(): LocalDate = LocalDate.now()

    override fun zone(): ZoneId = ZoneId.systemDefault()
}
