package com.milestone.app.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.MilestoneDatabase
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.local.entity.ReflectionEntity
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Export and restore, round-tripped through a real database.
 *
 * A backup is only worth anything if it comes back, and the only way to know it
 * comes back is to put it back. These tests write real data, export it, wipe the
 * database, restore the file, and compare — which also exercises the type
 * converters and the foreign-key ordering of the restore, both of which are easy
 * to get subtly wrong and impossible to notice until someone needs their data.
 */
@RunWith(AndroidJUnit4::class)
class BackupRepositoryTest {

    private class TestClock(var instant: Instant) : AppClock {
        override fun now(): Instant = instant
        override fun today(): LocalDate = instant.atZone(zone()).toLocalDate()
        override fun zone(): ZoneId = ZoneId.of("UTC")
    }

    private lateinit var database: MilestoneDatabase
    private lateinit var goals: GoalRepository
    private lateinit var backup: BackupRepository
    private val clock = TestClock(Instant.parse("2026-03-02T09:00:00Z"))
    private var areaId: Long = 0

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MilestoneDatabase::class.java).build()

        val achievements = AchievementRepository(
            achievementDao = database.achievementDao(),
            goalDao = database.goalDao(),
            milestoneDao = database.milestoneDao(),
            reflectionDao = database.reflectionDao(),
            seasonDao = database.seasonDao(),
            progressEventDao = database.progressEventDao(),
            clock = clock,
        )
        goals = GoalRepository(
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
        backup = BackupRepository(
            database = database,
            backupDao = database.backupDao(),
            clock = clock,
        )

        areaId = database.lifeAreaDao().insert(
            LifeAreaEntity(name = "Health", emoji = "🌿", colorArgb = 0xFF4C7A5A),
        )
    }

    @After
    fun tearDown() = database.close()

    @Test
    fun a_backup_restores_every_goal_exactly_as_it_was() = runTest {
        val goalId = goals.createGoal(
            GoalDraft(
                title = "Run a half marathon",
                why = "To finish something hard",
                lifeAreaId = areaId,
                emoji = "🏃",
                targetDate = LocalDate.of(2026, 9, 13),
                priority = GoalPriority.Primary,
                seasonId = null,
                milestoneTitles = listOf("Train", "Long run", "Race"),
            ),
        )
        val milestones = database.milestoneDao().getForGoal(goalId)
        goals.editMilestone(milestones[0].id, "Train", "Three times a week", LocalDate.of(2026, 5, 1))
        goals.setMilestoneCompleted(milestones[0].id, completed = true)
        goals.addQuest(milestones[1].id, "Plan the route")
        val before = database.goalDao().get(goalId)!!

        val json = backup.export()
        wipe()
        assertEquals(0, database.goalDao().count())

        backup.restore(json)

        val after = database.goalDao().get(goalId)!!
        assertEquals(before, after)

        val restoredMilestones = database.milestoneDao().getForGoal(goalId)
        assertEquals(3, restoredMilestones.size)
        assertEquals(listOf("Train", "Long run", "Race"), restoredMilestones.map { it.title })
        // The converted types are the ones most likely to come back wrong.
        assertEquals("Three times a week", restoredMilestones[0].note)
        assertEquals(LocalDate.of(2026, 5, 1), restoredMilestones[0].dueDate)
        assertEquals(LocalDate.of(2026, 9, 13), after.targetDate)
        assertEquals(GoalPriority.Primary, after.priority)
        assertTrue(restoredMilestones[0].isCompleted)

        // Three steps, one of them ticked off before the backup was taken.
        assertEquals(2, database.milestoneDao().openCountForGoal(goalId))

        // The quest still hangs off the right milestone, two foreign keys deep.
        val quests = database.questDao()
            .observeForMilestone(restoredMilestones[1].id)
            .first()
        assertEquals(listOf("Plan the route"), quests.map { it.title })
    }

    @Test
    fun an_archived_goal_keeps_its_reason_and_note_through_a_backup() = runTest {
        val goalId = goals.createGoal(draft("Learn Japanese", "Hiragana"))
        goals.archive(goalId, ClosingReason.WrongTiming, "Maybe next year")

        val json = backup.export()
        wipe()
        backup.restore(json)

        val restored = database.goalDao().get(goalId)!!
        assertEquals(GoalStatus.Archived, restored.status)
        assertEquals(ClosingReason.WrongTiming, restored.closingReason)
        assertEquals("Maybe next year", restored.closingNote)
        assertNotNull(restored.archivedAt)
    }

    @Test
    fun reflections_survive_verbatim() = runTest {
        database.reflectionDao().put(
            ReflectionEntity(
                weekStart = LocalDate.of(2026, 2, 23),
                movedForward = "Two long runs.",
                didNotMove = null,
                surprised = "How easy it felt.",
                proudOf = "Went out in the rain.",
                nextFocus = "Protect Tuesday.",
                createdAt = clock.instant,
                updatedAt = clock.instant,
            ),
        )

        val json = backup.export()
        wipe()
        backup.restore(json)

        val restored = database.reflectionDao().getForWeek(LocalDate.of(2026, 2, 23))!!
        assertEquals("Two long runs.", restored.movedForward)
        // An unanswered question must come back unanswered, not as "null".
        assertNull(restored.didNotMove)
        assertEquals("Went out in the rain.", restored.proudOf)
    }

    /**
     * Restore replaces; it does not merge.
     *
     * This is the behaviour the confirmation promises the user, so it is worth
     * pinning: anything created after the backup was taken is expected to go.
     */
    @Test
    fun restoring_replaces_whatever_is_there_rather_than_merging() = runTest {
        goals.createGoal(draft("In the backup", "Step one"))
        val json = backup.export()

        goals.createGoal(draft("Added afterwards", "Step one"))
        assertEquals(2, database.goalDao().count())

        backup.restore(json)

        assertEquals(1, database.goalDao().count())
        val remaining = database.backupDao().goals().single()
        assertEquals("In the backup", remaining.title)
    }

    @Test
    fun a_file_that_is_not_a_backup_is_refused_and_changes_nothing() = runTest {
        goals.createGoal(draft("Keep me", "Step one"))

        assertThrows(BackupFormatException::class.java) {
            kotlinx.coroutines.runBlocking { backup.restore("{\"hello\":\"world\"}") }
        }
        assertThrows(BackupFormatException::class.java) {
            kotlinx.coroutines.runBlocking { backup.restore("not json at all") }
        }

        assertEquals(1, database.goalDao().count())
    }

    @Test
    fun a_backup_from_a_newer_version_is_refused_rather_than_half_read() = runTest {
        goals.createGoal(draft("Keep me", "Step one"))
        val future = backup.export().replace("\"formatVersion\": 1", "\"formatVersion\": 99")

        val error = assertThrows(BackupFormatException::class.java) {
            kotlinx.coroutines.runBlocking { backup.restore(future) }
        }

        assertTrue(error.message!!.contains("newer version"))
        assertEquals(1, database.goalDao().count())
    }

    @Test
    fun inspecting_a_backup_reports_what_is_in_it_without_touching_anything() = runTest {
        goals.createGoal(draft("Goal one", "a", "b"))
        goals.createGoal(draft("Goal two", "c"))
        val json = backup.export()

        val summary = backup.inspect(json)

        assertEquals(2, summary.goals)
        assertEquals(3, summary.milestones)
        assertEquals(clock.instant, summary.createdAt)
        // Still there: inspecting is a read.
        assertEquals(2, database.goalDao().count())
    }

    @Test
    fun an_empty_database_round_trips_without_complaint() = runTest {
        wipe()

        val json = backup.export()
        backup.restore(json)

        assertEquals(0, database.goalDao().count())
    }

    private suspend fun wipe() {
        val dao = database.backupDao()
        dao.clearProgressEvents()
        dao.clearAchievements()
        dao.clearReflections()
        dao.clearQuests()
        dao.clearMilestones()
        dao.clearGoals()
        dao.clearSeasons()
        dao.clearLifeAreas()
        dao.clearProfiles()
    }

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
}
