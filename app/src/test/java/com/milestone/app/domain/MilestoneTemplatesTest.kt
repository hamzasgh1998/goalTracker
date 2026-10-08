package com.milestone.app.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The template matcher is the app's offline stand-in for "help me break this
 * down". It is only keyword matching, so these tests pin the behaviour that
 * matters: a sensible hit for common phrasings, and a usable fallback rather than
 * an empty list when nothing matches.
 */
class MilestoneTemplatesTest {

    @Test
    fun `matches an instrument goal`() {
        val suggestions = MilestoneTemplates.suggestFor("Learn guitar")

        assertEquals("Learning an instrument", suggestions.first().label)
    }

    @Test
    fun `matches a running goal from a distance alone`() {
        val suggestions = MilestoneTemplates.suggestFor("Run my first 10K")

        assertEquals("Running a distance", suggestions.first().label)
    }

    @Test
    fun `matching ignores case and punctuation`() {
        val suggestions = MilestoneTemplates.suggestFor("HOLD A CONVERSATION IN SPANISH!")

        assertEquals("Learning a language", suggestions.first().label)
    }

    @Test
    fun `an unmatched goal still gets a usable starting point`() {
        val suggestions = MilestoneTemplates.suggestFor("Restore my grandfather's clock")

        // Never empty: the caller renders this directly and has no empty case.
        assertEquals(listOf(MilestoneTemplates.generic), suggestions)
        assertTrue(MilestoneTemplates.generic.milestones.isNotEmpty())
    }

    @Test
    fun `an empty title falls back rather than throwing`() {
        assertEquals(listOf(MilestoneTemplates.generic), MilestoneTemplates.suggestFor("   "))
    }

    @Test
    fun `suggestions are capped at the requested limit`() {
        // "build a fitness app" touches both the project and fitness templates.
        val suggestions = MilestoneTemplates.suggestFor("Build a fitness app", limit = 1)

        assertEquals(1, suggestions.size)
    }

    @Test
    fun `the better keyword match is ranked first`() {
        val suggestions = MilestoneTemplates.suggestFor("Launch and ship my product")

        assertEquals("Shipping a project", suggestions.first().label)
    }

    @Test
    fun `every template has a stable non-empty milestone list`() {
        val all = MilestoneTemplates.suggestFor("run") + MilestoneTemplates.generic
        all.forEach { template ->
            assertTrue(template.label.isNotBlank())
            assertTrue(template.milestones.isNotEmpty())
            assertTrue(template.milestones.none(String::isBlank))
        }
    }
}
