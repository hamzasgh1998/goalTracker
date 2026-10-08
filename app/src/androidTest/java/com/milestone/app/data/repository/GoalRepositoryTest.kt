package com.milestone.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.MilestoneDatabase
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.local.entity.QuestEntity
import com.milestone.app.domain.achievement.AchievementCatalog
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.domain.model.ProgressEventType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * The repository's transactional writes, against a real SQLite database.
 *
 * These are the riskiest methods in the app and the ones unit tests cannot reach:
 * each is several statements across several tables wrapped in
 * `database.withTransaction`, and the invariants they maintain — a goal's status
 * being *derived* from its milestones, `lastProgressAt` keeping goal health
 * honest, the journey log recording what happened — are only observable once the
 * transaction has actually committed. Room's query generation, the type
 * converters and the foreign-key cascades are all exercised for free.
 *
 * In-memory, so each test starts from an empty schema.
 */
@RunWith(AndroidJUnit4::class)
class GoalRepositoryTest {

    /**
     * A clock the tests move by hand.
     *
     * Goal health and the "has it moved recently?" stamps are defined in terms of
     * elapsed time, so a test that cannot advance the clock cannot assert on them.
     */
    private class TestClock(var instant: Instant) : AppClock {
        override fun now(): Instant = instant
        override fun today(): LocalDate = instant.atZone(zone()).toLocalDate()
        override fun zone(): ZoneId = ZoneId.of("UTC")

        fun advanceDays(days: Long) {
            instant = instant.plusSeconds(days * 86_400)
        }
    }

    private lateinit var database: MilestoneDatabase
    private lateinit var repository: GoalRepository
    private lateinit var achievements: AchievementRepository
    private val clock = TestClock(Instant.parse("2026-03-02T09:00:00Z"))

    /** The life area every test goal hangs off; goals have a non-null FK to one. */
    private var areaId: Long = 0

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MilestoneDatabase::class.java).build()

        achievements = AchievementRepository(
            achievementDao = database.achievementDao(),
            goalDao = database.goalDao(),
            milestoneDao = database.milestoneDao(),
            reflectionDao = database.reflectionDao(),
            seasonDao = database.seasonDao(),
            progressEventDao = database.progressEventDao(),
            clock = clock,
        )
        repository = GoalRepository(
            database = database,
            goalDao = database.goalDao(),
            milestoneDao = database.milestoneDao(),
            questDao = database.questDao(),
            lifeAreaDao = database.lifeAreaDao(),
            progressEventDao = database.progressEventDao(),
            seasonDao = database.seasonDao(),
            achievements = achievements,
            clock = clock,
        )

        areaId = database.lifeAreaDao().insert(
            LifeAreaEntity(name = "Health", emoji = "🌿", colorArgb = 0xFF4C7A5A),
        )
    }

    @After
    fun tearDown() {
        database.close()
    }

    // ------------------------------------------------------------------ create

    @Test
    fun creating_a_goal_writes_its_milestones_and_a_journey_entry() = runTest {
        val goalId = repository.createGoal(draft("Run a half marathon", "Train", "Race"))

        val goal = database.goalDao().get(goalId)
        assertNotNull(goal)
        assertEquals("Run a half marathon", goal!!.title)
        assertEquals(GoalStatus.Active, goal.status)
        // A brand-new goal has not moved yet; health copy depends on this.
        assertNull(goal.lastProgressAt)

        val milestones = database.milestoneDao().getForGoal(goalId)
        assertEquals(listOf("Train", "Race"), milestones.map { it.title })
        // Order is carried by sortOrder, not by row id.
        assertEquals(listOf(0, 1), milestones.map { it.sortOrder })

        // Exactly one creation entry. The log may also hold an achievement unlock
        // (the first goal earns one), so this counts the type rather than the rows.
        val created = database.progressEventDao().getBetween(EPOCH, FAR_FUTURE)
            .filter { it.type == ProgressEventType.GoalCreated }
        assertEquals(1, created.size)
        assertEquals(goalId, created.single().goalId)
        assertEquals("Run a half marathon", created.single().title)
    }

    @Test
    fun creating_a_goal_drops_blank_milestone_titles() = runTest {
        val goalId = repository.createGoal(draft("Read more", "Pick a book", "   ", ""))

        assertEquals(
            listOf("Pick a book"),
            database.milestoneDao().getForGoal(goalId).map { it.title },
        )
    }

    // --------------------------------------------------------------- milestones

    @Test
    fun completing_a_milestone_stamps_progress_and_logs_it() = runTest {
        val goalId = repository.createGoal(draft("Learn to swim", "Lesson one", "Lesson two"))
        val first = database.milestoneDao().getForGoal(goalId).first()

        clock.advanceDays(3)
        repository.setMilestoneCompleted(first.id, completed = true)

        val stored = database.milestoneDao().get(first.id)!!
        assertTrue(stored.isCompleted)
        assertEquals(clock.instant, stored.completedAt)
        // Same transaction: the goal's own stamp has to move with it, or the goal
        // reads as stalled while its milestones are being ticked off.
        assertEquals(clock.instant, database.goalDao().get(goalId)!!.lastProgressAt)

        val types = database.progressEventDao().getBetween(EPOCH, FAR_FUTURE).map { it.type }
        assertTrue(types.contains(ProgressEventType.MilestoneCompleted))
    }

    @Test
    fun completing_the_last_milestone_completes_the_goal() = runTest {
        val goalId = repository.createGoal(draft("Fix the bike", "Buy parts", "Fit them"))
        val milestones = database.milestoneDao().getForGoal(goalId)

        repository.setMilestoneCompleted(milestones[0].id, completed = true)
        assertEquals(GoalStatus.Active, database.goalDao().get(goalId)!!.status)

        repository.setMilestoneCompleted(milestones[1].id, completed = true)

        val goal = database.goalDao().get(goalId)!!
        assertEquals(GoalStatus.Completed, goal.status)
        assertNotNull(goal.completedAt)

        val types = database.progressEventDao().getBetween(EPOCH, FAR_FUTURE).map { it.type }
        assertEquals(1, types.count { it == ProgressEventType.GoalCompleted })
    }

    /**
     * The reverse case, which is the one that gets forgotten.
     *
     * Without it, a mis-tap on the final milestone leaves the goal marked done for
     * good — correcting the milestone would no longer correct the goal.
     */
    @Test
    fun un_ticking_a_milestone_reopens_a_completed_goal() = runTest {
        val goalId = repository.createGoal(draft("Tidy the garage", "Clear shelves"))
        val only = database.milestoneDao().getForGoal(goalId).single()

        repository.setMilestoneCompleted(only.id, completed = true)
        assertEquals(GoalStatus.Completed, database.goalDao().get(goalId)!!.status)

        repository.setMilestoneCompleted(only.id, completed = false)

        val goal = database.goalDao().get(goalId)!!
        assertEquals(GoalStatus.Active, goal.status)
        assertNull(goal.completedAt)
        assertFalse(database.milestoneDao().get(only.id)!!.isCompleted)
    }

    @Test
    fun a_goal_with_no_milestones_is_never_auto_completed() = runTest {
        val goalId = repository.createGoal(draft("Write more"))
        val added = repository.addMilestone(goalId, "First page")

        repository.setMilestoneCompleted(added, completed = true)
        assertEquals(GoalStatus.Completed, database.goalDao().get(goalId)!!.status)

        // And deleting the only milestone must not, by itself, change the status:
        // that is a destructive edit, not progress.
        repository.deleteMilestone(added)
        assertEquals(GoalStatus.Completed, database.goalDao().get(goalId)!!.status)
    }

    @Test
    fun adding_a_milestone_appends_it_and_logs_it() = runTest {
        val goalId = repository.createGoal(draft("Cook properly", "Knife skills"))

        val addedId = repository.addMilestone(goalId, "  Make stock  ")

        val milestones = database.milestoneDao().getForGoal(goalId)
        assertEquals(listOf("Knife skills", "Make stock"), milestones.map { it.title })
        assertEquals(1, milestones.last().sortOrder)
        assertEquals(addedId, milestones.last().id)

        val events = database.progressEventDao().getBetween(EPOCH, FAR_FUTURE)
        assertTrue(events.any { it.type == ProgressEventType.MilestoneAdded })
    }

    @Test
    fun reordering_rewrites_every_sort_order_in_one_pass() = runTest {
        val goalId = repository.createGoal(draft("Move house", "Pack", "Hire van", "Clean"))
        val original = database.milestoneDao().getForGoal(goalId)

        val reversed = original.map { it.id }.reversed()
        repository.reorderMilestones(reversed)

        val reordered = database.milestoneDao().getForGoal(goalId)
        assertEquals(reversed, reordered.map { it.id })
        assertEquals(listOf(0, 1, 2), reordered.map { it.sortOrder })
        assertEquals(listOf("Clean", "Hire van", "Pack"), reordered.map { it.title })
    }

    // -------------------------------------------------------------------- quests

    /**
     * Quests move the clock on a goal without moving its percentage.
     *
     * The point is that splitting work finely must not inflate progress: a user
     * who uses quests and one who doesn't should see the same number.
     */
    @Test
    fun completing_a_quest_touches_progress_but_not_the_goal_status() = runTest {
        val goalId = repository.createGoal(draft("Build a shed", "Lay the base"))
        val milestone = database.milestoneDao().getForGoal(goalId).single()
        val questId = database.questDao().insert(
            QuestEntity(
                milestoneId = milestone.id,
                title = "Order timber",
                sortOrder = 0,
                createdAt = clock.instant,
            ),
        )

        clock.advanceDays(2)
        repository.setQuestCompleted(questId, completed = true)

        val goal = database.goalDao().get(goalId)!!
        assertEquals(clock.instant, goal.lastProgressAt)
        assertEquals(GoalStatus.Active, goal.status)
        // The milestone itself is untouched: progress is measured in milestones.
        assertFalse(database.milestoneDao().get(milestone.id)!!.isCompleted)

        val events = database.progressEventDao().getBetween(EPOCH, FAR_FUTURE)
        assertTrue(events.any { it.type == ProgressEventType.QuestCompleted })
    }

    // -------------------------------------------------------------- status moves

    @Test
    fun pausing_and_resuming_logs_both_and_earns_the_comeback() = runTest {
        val goalId = repository.createGoal(draft("Learn guitar", "Chords"))

        repository.pause(goalId)
        assertEquals(GoalStatus.Paused, database.goalDao().get(goalId)!!.status)
        assertFalse(database.achievementDao().isUnlocked(AchievementCatalog.COMEBACK))

        clock.advanceDays(30)
        repository.resume(goalId)

        val goal = database.goalDao().get(goalId)!!
        assertEquals(GoalStatus.Active, goal.status)
        // Resuming counts as movement, so the goal does not immediately read stalled.
        assertEquals(clock.instant, goal.lastProgressAt)
        assertTrue(database.achievementDao().isUnlocked(AchievementCatalog.COMEBACK))

        val types = database.progressEventDao().getBetween(EPOCH, FAR_FUTURE).map { it.type }
        assertTrue(types.contains(ProgressEventType.GoalPaused))
        assertTrue(types.contains(ProgressEventType.GoalResumed))
    }

    /** Resuming a goal that was never paused is not a comeback. */
    @Test
    fun resuming_an_active_goal_does_not_earn_the_comeback() = runTest {
        val goalId = repository.createGoal(draft("Stretch daily", "Day one"))

        repository.resume(goalId)

        assertFalse(database.achievementDao().isUnlocked(AchievementCatalog.COMEBACK))
    }

    @Test
    fun archiving_keeps_the_goal_with_its_reason_and_restoring_brings_it_back() = runTest {
        val goalId = repository.createGoal(draft("Learn Japanese", "Hiragana"))

        repository.archive(goalId, ClosingReason.WrongTiming, "  Maybe next year  ")

        val archived = database.goalDao().get(goalId)!!
        assertEquals(GoalStatus.Archived, archived.status)
        assertEquals(ClosingReason.WrongTiming, archived.closingReason)
        assertEquals("Maybe next year", archived.closingNote)
        assertNotNull(archived.archivedAt)
        // Nothing is deleted: the milestones survive the close.
        assertEquals(1, database.milestoneDao().countForGoal(goalId))
        assertTrue(database.achievementDao().isUnlocked(AchievementCatalog.HONEST_CLOSE))

        repository.restore(goalId)

        val restored = database.goalDao().get(goalId)!!
        assertEquals(GoalStatus.Active, restored.status)
        assertNull(restored.archivedAt)
        assertNull(restored.closingReason)
    }

    @Test
    fun an_empty_closing_note_is_stored_as_nothing_at_all() = runTest {
        val goalId = repository.createGoal(draft("Run an ultra", "Base miles"))

        repository.archive(goalId, ClosingReason.NoLongerImportant, "   ")

        assertNull(database.goalDao().get(goalId)!!.closingNote)
    }

    // --------------------------------------------------------------------- edits

    @Test
    fun editing_a_goal_leaves_its_history_alone() = runTest {
        val goalId = repository.createGoal(draft("Run 10k", "Couch to 5k"))
        val milestone = database.milestoneDao().getForGoal(goalId).single()
        repository.setMilestoneCompleted(milestone.id, completed = true)

        val before = database.goalDao().get(goalId)!!
        clock.advanceDays(5)

        repository.editGoal(
            goalId = goalId,
            title = "  Run a 10k race  ",
            why = "  Because I said I would  ",
            emoji = "🏃",
            lifeAreaId = areaId,
            targetDate = LocalDate.of(2026, 9, 1),
            priority = GoalPriority.Primary,
        )

        val after = database.goalDao().get(goalId)!!
        assertEquals("Run a 10k race", after.title)
        assertEquals("Because I said I would", after.why)
        assertEquals("🏃", after.emoji)
        assertEquals(LocalDate.of(2026, 9, 1), after.targetDate)
        assertEquals(GoalPriority.Primary, after.priority)

        // The derived fields are the whole point of the read-then-copy: an edit
        // must not reset progress, status or the creation date.
        assertEquals(before.status, after.status)
        assertEquals(before.createdAt, after.createdAt)
        assertEquals(before.lastProgressAt, after.lastProgressAt)
        assertEquals(before.completedAt, after.completedAt)
        assertEquals(1, database.milestoneDao().countForGoal(goalId))
    }

    @Test
    fun a_blank_title_is_refused_rather_than_saved() = runTest {
        val goalId = repository.createGoal(draft("Swim weekly", "Find a pool"))

        repository.editGoal(
            goalId = goalId,
            title = "   ",
            why = "changed",
            emoji = "🏊",
            lifeAreaId = areaId,
            targetDate = null,
            priority = GoalPriority.Secondary,
        )

        val goal = database.goalDao().get(goalId)!!
        assertEquals("Swim weekly", goal.title)
        // Rejected whole, not field by field.
        assertEquals("Why not", goal.why)
    }

    @Test
    fun editing_a_goal_that_is_already_gone_is_a_no_op() = runTest {
        repository.editGoal(
            goalId = 9_999,
            title = "Ghost",
            why = "",
            emoji = "👻",
            lifeAreaId = areaId,
            targetDate = null,
            priority = GoalPriority.Secondary,
        )

        assertEquals(0, database.goalDao().count())
    }

    @Test
    fun clearing_a_target_date_is_an_edit_like_any_other() = runTest {
        val goalId = repository.createGoal(
            draft("Save for a trip", "Open an account")
                .copy(targetDate = LocalDate.of(2026, 12, 1)),
        )
        assertNotNull(database.goalDao().get(goalId)!!.targetDate)

        repository.editGoal(
            goalId = goalId,
            title = "Save for a trip",
            why = "Why not",
            emoji = "💰",
            lifeAreaId = areaId,
            targetDate = null,
            priority = GoalPriority.Secondary,
        )

        assertNull(database.goalDao().get(goalId)!!.targetDate)
    }

    // --------------------------------------------------------- milestone edits

    @Test
    fun editing_a_milestone_saves_its_name_note_and_due_date_together() = runTest {
        val goalId = repository.createGoal(draft("Learn to sail", "Book the course"))
        val milestone = database.milestoneDao().getForGoal(goalId).single()

        repository.editMilestone(
            milestoneId = milestone.id,
            title = "  Book the RYA course  ",
            note = "  Ask about the weekend option  ",
            dueDate = LocalDate.of(2026, 5, 9),
        )

        val saved = database.milestoneDao().get(milestone.id)!!
        assertEquals("Book the RYA course", saved.title)
        assertEquals("Ask about the weekend option", saved.note)
        assertEquals(LocalDate.of(2026, 5, 9), saved.dueDate)
        // Position and completion are not the edit sheet's business.
        assertEquals(milestone.sortOrder, saved.sortOrder)
        assertEquals(milestone.isCompleted, saved.isCompleted)
        assertEquals(milestone.createdAt, saved.createdAt)
    }

    @Test
    fun a_blank_milestone_title_is_refused_rather_than_saved() = runTest {
        val goalId = repository.createGoal(draft("Grow tomatoes", "Sow the seeds"))
        val milestone = database.milestoneDao().getForGoal(goalId).single()

        repository.editMilestone(
            milestoneId = milestone.id,
            title = "   ",
            note = "this should not land either",
            dueDate = LocalDate.of(2026, 6, 1),
        )

        val saved = database.milestoneDao().get(milestone.id)!!
        assertEquals("Sow the seeds", saved.title)
        // Refused whole, not field by field.
        assertNull(saved.note)
        assertNull(saved.dueDate)
    }

    @Test
    fun clearing_a_note_or_a_due_date_stores_nothing_at_all() = runTest {
        val goalId = repository.createGoal(draft("Redecorate", "Pick a colour"))
        val milestone = database.milestoneDao().getForGoal(goalId).single()
        repository.editMilestone(
            milestoneId = milestone.id,
            title = "Pick a colour",
            note = "Something warm",
            dueDate = LocalDate.of(2026, 4, 2),
        )

        repository.editMilestone(
            milestoneId = milestone.id,
            title = "Pick a colour",
            // Whitespace, which is what an emptied text field actually sends.
            note = "   ",
            dueDate = null,
        )

        val saved = database.milestoneDao().get(milestone.id)!!
        assertNull(saved.note)
        assertNull(saved.dueDate)
    }

    @Test
    fun editing_a_milestone_that_is_already_gone_is_a_no_op() = runTest {
        repository.editMilestone(
            milestoneId = 9_999,
            title = "Ghost step",
            note = null,
            dueDate = null,
        )

        assertEquals(0, database.milestoneDao().completedCount())
    }

    // ---------------------------------------------------------------- quests

    @Test
    fun quests_are_appended_in_order_under_their_milestone() = runTest {
        val goalId = repository.createGoal(draft("Run a workshop", "Write the outline"))
        val milestone = database.milestoneDao().getForGoal(goalId).single()

        val first = repository.addQuest(milestone.id, "  Draft the agenda  ")
        val second = repository.addQuest(milestone.id, "Book the room")

        val quests = database.questDao().observeForMilestone(milestone.id).first()
        assertEquals(listOf(first, second), quests.map { it.id })
        assertEquals(listOf("Draft the agenda", "Book the room"), quests.map { it.title })
        // Each one lands after the last, rather than every quest claiming slot 0.
        assertTrue(quests[1].sortOrder > quests[0].sortOrder)
        assertFalse(quests.any { it.isCompleted })
    }

    @Test
    fun renaming_a_quest_keeps_everything_else_about_it() = runTest {
        val goalId = repository.createGoal(draft("Learn to bake", "Make a loaf"))
        val milestone = database.milestoneDao().getForGoal(goalId).single()
        val questId = repository.addQuest(milestone.id, "Buy flour")
        repository.setQuestCompleted(questId, completed = true)
        val before = database.questDao().get(questId)!!

        repository.updateQuestTitle(questId, "  Buy strong white flour  ")

        val after = database.questDao().get(questId)!!
        assertEquals("Buy strong white flour", after.title)
        assertTrue(after.isCompleted)
        assertEquals(before.completedAt, after.completedAt)
        assertEquals(before.sortOrder, after.sortOrder)
    }

    @Test
    fun a_blank_quest_title_is_refused() = runTest {
        val goalId = repository.createGoal(draft("Start running", "First parkrun"))
        val milestone = database.milestoneDao().getForGoal(goalId).single()
        val questId = repository.addQuest(milestone.id, "Find a local one")

        repository.updateQuestTitle(questId, "  ")

        assertEquals("Find a local one", database.questDao().get(questId)!!.title)
    }

    @Test
    fun deleting_a_quest_leaves_its_milestone_untouched() = runTest {
        val goalId = repository.createGoal(draft("Plan a trip", "Pick the dates"))
        val milestone = database.milestoneDao().getForGoal(goalId).single()
        val keep = repository.addQuest(milestone.id, "Check the calendar")
        val remove = repository.addQuest(milestone.id, "Ask about time off")

        repository.deleteQuest(remove)

        val quests = database.questDao().observeForMilestone(milestone.id).first()
        assertEquals(listOf(keep), quests.map { it.id })
        // Deleting a sub-step is not progress, and must not touch the step above.
        assertNotNull(database.milestoneDao().get(milestone.id))
        assertEquals(1, database.milestoneDao().countForGoal(goalId))
    }

    // ------------------------------------------------------------------ deletes

    @Test
    fun deleting_a_goal_forever_takes_its_whole_tree() = runTest {
        val goalId = repository.createGoal(draft("Old plan", "Step one"))
        val milestone = database.milestoneDao().getForGoal(goalId).single()
        val questId = database.questDao().insert(
            QuestEntity(
                milestoneId = milestone.id,
                title = "Sub-step",
                sortOrder = 0,
                createdAt = clock.instant,
            ),
        )

        repository.deleteForever(goalId)

        assertEquals(0, database.goalDao().count())
        assertEquals(0, database.milestoneDao().countForGoal(goalId))
        // Cascaded through the milestone, two foreign keys deep.
        assertNull(database.questDao().get(questId))
    }

    // -------------------------------------------------------------------- helper

    private fun draft(title: String, vararg milestones: String) = GoalDraft(
        title = title,
        why = "Why not",
        lifeAreaId = areaId,
        emoji = "🎯",
        targetDate = null,
        priority = GoalPriority.Secondary,
        seasonId = null,
        milestoneTitles = milestones.toList(),
    )

    private companion object {
        val EPOCH: Instant = Instant.EPOCH
        val FAR_FUTURE: Instant = Instant.parse("2100-01-01T00:00:00Z")
    }
}
