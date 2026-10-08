package com.milestone.app.data.seed

import androidx.room.withTransaction
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.MilestoneDatabase
import com.milestone.app.data.local.dao.AchievementDao
import com.milestone.app.data.local.dao.GoalDao
import com.milestone.app.data.local.dao.LifeAreaDao
import com.milestone.app.data.local.dao.MilestoneDao
import com.milestone.app.data.local.dao.ProgressEventDao
import com.milestone.app.data.local.dao.QuestDao
import com.milestone.app.data.local.dao.ReflectionDao
import com.milestone.app.data.local.dao.SeasonDao
import com.milestone.app.data.local.dao.UserProfileDao
import com.milestone.app.data.local.entity.AchievementEntity
import com.milestone.app.data.local.entity.GoalEntity
import com.milestone.app.data.local.entity.LifeSeasonEntity
import com.milestone.app.data.local.entity.MilestoneEntity
import com.milestone.app.data.local.entity.ProgressEventEntity
import com.milestone.app.data.local.entity.QuestEntity
import com.milestone.app.data.local.entity.ReflectionEntity
import com.milestone.app.data.local.entity.UserProfileEntity
import com.milestone.app.domain.achievement.AchievementCatalog
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.domain.model.ProgressEventType
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Populates a realistic dataset so the UI can be judged with real content in it.
 *
 * This is development scaffolding, gated by `BuildConfig.SEED_SAMPLE_DATA` (true
 * only in debug) *and* a one-shot preference flag, so it can never resurrect
 * itself after a user clears it. Release builds start empty.
 *
 * The data is shaped to exercise the states that are easy to get wrong and hard to
 * reach by hand: a goal at 78%, a goal that has been quiet long enough to read as
 * Stalled, a paused goal, a finished goal, and a closed chapter with a reason.
 * Every timestamp is relative to "now", so the screenshots never go stale.
 */
@Singleton
class SampleDataSeeder @Inject constructor(
    private val database: MilestoneDatabase,
    private val userProfileDao: UserProfileDao,
    private val lifeAreaDao: LifeAreaDao,
    private val seasonDao: SeasonDao,
    private val goalDao: GoalDao,
    private val milestoneDao: MilestoneDao,
    private val questDao: QuestDao,
    private val reflectionDao: ReflectionDao,
    private val progressEventDao: ProgressEventDao,
    private val achievementDao: AchievementDao,
    private val clock: AppClock,
) {

    /** A goal to create, with how many of its milestones are already done. */
    private data class SampleGoal(
        val title: String,
        val why: String,
        val emoji: String,
        val areaName: String,
        val priority: GoalPriority,
        val status: GoalStatus = GoalStatus.Active,
        val daysAgoCreated: Long,
        /** Days since the last milestone was ticked. Drives goal health. */
        val daysSinceProgress: Long?,
        val targetInDays: Long?,
        val milestones: List<String>,
        val completedCount: Int,
        val closingReason: ClosingReason? = null,
        /** Quests for the first incomplete milestone, to show the third tier. */
        val questsForCurrent: List<String> = emptyList(),
        val completedQuestCount: Int = 0,
    )

    suspend fun seed() {
        val now = clock.now()
        val today = clock.today()
        val areas = lifeAreaDao.getAll().associateBy { it.name }

        database.withTransaction {
            userProfileDao.put(
                UserProfileEntity(
                    name = "Alex",
                    startedAt = now.minusDaysAgo(96),
                    hasOnboarded = true,
                ),
            )

            val seasonId = seasonDao.startSeason(
                LifeSeasonEntity(
                    title = "Autumn ${today.year}",
                    theme = "Build & Grow",
                    intention = "Focus on creating, learning and growing. Fewer things, further.",
                    startDate = today.minusDays(38),
                    endDate = today.plusDays(53),
                ),
            )
            progressEventDao.insert(
                ProgressEventEntity(
                    type = ProgressEventType.SeasonStarted,
                    title = "Autumn ${today.year}",
                    subtitle = "Build & Grow",
                    seasonId = seasonId,
                    occurredAt = now.minusDaysAgo(38),
                ),
            )

            sampleGoals().forEach { sample ->
                val area = areas[sample.areaName] ?: return@forEach
                val createdAt = now.minusDaysAgo(sample.daysAgoCreated)
                val lastProgress = sample.daysSinceProgress?.let { now.minusDaysAgo(it) }

                val goalId = goalDao.insert(
                    GoalEntity(
                        title = sample.title,
                        why = sample.why,
                        lifeAreaId = area.id,
                        // Closed chapters predate this season, so they aren't in it.
                        seasonId = if (sample.status.isOpen) seasonId else null,
                        emoji = sample.emoji,
                        targetDate = sample.targetInDays?.let { today.plusDays(it) },
                        status = sample.status,
                        priority = sample.priority,
                        createdAt = createdAt,
                        lastProgressAt = lastProgress,
                        completedAt = if (sample.status == GoalStatus.Completed) {
                            lastProgress
                        } else {
                            null
                        },
                        closingReason = sample.closingReason,
                        archivedAt = if (sample.status == GoalStatus.Archived) {
                            lastProgress
                        } else {
                            null
                        },
                    ),
                )

                progressEventDao.insert(
                    ProgressEventEntity(
                        type = ProgressEventType.GoalCreated,
                        title = sample.title,
                        subtitle = "A new goal begins",
                        goalId = goalId,
                        accentArgb = area.colorArgb,
                        occurredAt = createdAt,
                    ),
                )

                seedMilestones(sample, goalId, area.colorArgb, createdAt)

                if (sample.status == GoalStatus.Completed) {
                    progressEventDao.insert(
                        ProgressEventEntity(
                            type = ProgressEventType.GoalCompleted,
                            title = sample.title,
                            subtitle = "Every milestone done",
                            goalId = goalId,
                            accentArgb = area.colorArgb,
                            occurredAt = lastProgress ?: now,
                        ),
                    )
                }
                if (sample.status == GoalStatus.Archived) {
                    progressEventDao.insert(
                        ProgressEventEntity(
                            type = ProgressEventType.GoalArchived,
                            title = sample.title,
                            subtitle = sample.closingReason?.label,
                            goalId = goalId,
                            occurredAt = lastProgress ?: now,
                        ),
                    )
                }
            }

            seedAchievements(now)

            // A short run of reflections rather than a single one: the history
            // screen is about noticing how the weeks read next to each other, and
            // one card cannot show that. The third week is deliberately
            // half-answered — most real weeks are.
            seedReflections(today, now)
        }
    }

    /**
     * Writes a goal's milestones, back-dating each completion.
     *
     * Completions are spread evenly between the goal's creation and its last
     * progress date so the Journey timeline has entries across many days rather
     * than a single clump, which is what makes that screen worth looking at.
     */
    private suspend fun seedMilestones(
        sample: SampleGoal,
        goalId: Long,
        areaColor: Long,
        createdAt: Instant,
    ) {
        val lastProgress = sample.daysSinceProgress?.let { clock.now().minusDaysAgo(it) }
        val span = if (lastProgress != null) {
            ChronoUnit.DAYS.between(createdAt, lastProgress).coerceAtLeast(1)
        } else {
            1L
        }

        sample.milestones.forEachIndexed { index, title ->
            val isCompleted = index < sample.completedCount
            val completedAt = if (isCompleted && sample.completedCount > 0) {
                val step = span * (index + 1) / sample.completedCount
                createdAt.plus(step, ChronoUnit.DAYS)
            } else {
                null
            }

            val milestoneId = milestoneDao.insert(
                MilestoneEntity(
                    goalId = goalId,
                    title = title,
                    sortOrder = index,
                    isCompleted = isCompleted,
                    completedAt = completedAt,
                    createdAt = createdAt,
                ),
            )

            if (isCompleted && completedAt != null) {
                progressEventDao.insert(
                    ProgressEventEntity(
                        type = ProgressEventType.MilestoneCompleted,
                        title = title,
                        subtitle = sample.title,
                        goalId = goalId,
                        milestoneId = milestoneId,
                        accentArgb = areaColor,
                        occurredAt = completedAt,
                    ),
                )
            }

            // Quests hang off the current milestone only — enough to show the tier
            // exists without implying every milestone needs them.
            val isCurrent = index == sample.completedCount
            if (isCurrent && sample.questsForCurrent.isNotEmpty()) {
                sample.questsForCurrent.forEachIndexed { questIndex, questTitle ->
                    val questDone = questIndex < sample.completedQuestCount
                    questDao.insert(
                        QuestEntity(
                            milestoneId = milestoneId,
                            title = questTitle,
                            sortOrder = questIndex,
                            isCompleted = questDone,
                            completedAt = if (questDone) lastProgress else null,
                            createdAt = createdAt,
                        ),
                    )
                }
            }
        }
    }

    private fun sampleGoals(): List<SampleGoal> = listOf(
        SampleGoal(
            title = "Launch my side project",
            why = "I want to finish something and put it in front of real people, " +
                "instead of another folder of half-built ideas.",
            emoji = "🚀",
            areaName = "Career",
            priority = GoalPriority.Primary,
            daysAgoCreated = 36,
            daysSinceProgress = 1,
            targetInDays = 44,
            milestones = listOf(
                "Decide what it actually is",
                "Sketch the three core screens",
                "Set up the project",
                "Build the main flow",
                "Write the landing page",
                "Get five people to try it",
                "Fix what they struggled with",
                "Ship it publicly",
                "Tell people it exists",
            ),
            completedCount = 5,
            questsForCurrent = listOf(
                "Draft the headline",
                "Write the feature list",
                "Take screenshots",
                "Publish the page",
            ),
            completedQuestCount = 2,
        ),
        SampleGoal(
            title = "Run my first 10K",
            why = "I want to prove to myself that I can finish something difficult, " +
                "a bit at a time.",
            emoji = "🏃",
            areaName = "Health",
            priority = GoalPriority.Primary,
            daysAgoCreated = 62,
            daysSinceProgress = 2,
            targetInDays = 30,
            milestones = listOf(
                "Buy running shoes",
                "Run 3 km",
                "Run 5 km",
                "Run 7 km",
                "Run 8 km",
                "Run 9 km",
                "Run 10 km",
            ),
            completedCount = 5,
        ),
        SampleGoal(
            title = "Hold a conversation in Spanish",
            why = "My partner's family speaks Spanish and I am tired of smiling and nodding.",
            emoji = "🇪🇸",
            areaName = "Knowledge",
            priority = GoalPriority.Secondary,
            daysAgoCreated = 48,
            daysSinceProgress = 9,
            targetInDays = 120,
            milestones = listOf(
                "Learn 100 everyday words",
                "Get through present tense",
                "Watch a film with Spanish subtitles",
                "Have one five-minute conversation",
                "Order dinner entirely in Spanish",
                "Call my partner's mother",
            ),
            completedCount = 2,
        ),
        SampleGoal(
            title = "Read twelve books",
            why = "Reading is the first thing I drop when I get busy, and I always regret it.",
            emoji = "📚",
            areaName = "Knowledge",
            priority = GoalPriority.Maintenance,
            daysAgoCreated = 70,
            daysSinceProgress = 4,
            targetInDays = 80,
            milestones = (1..12).map { "Book $it" },
            completedCount = 5,
        ),
        SampleGoal(
            title = "Learn film photography",
            why = "I want one hobby that has nothing to do with a screen.",
            emoji = "📷",
            areaName = "Creativity",
            priority = GoalPriority.Secondary,
            daysAgoCreated = 54,
            // Long enough quiet to render as Stalled, which is a state worth seeing.
            daysSinceProgress = 26,
            targetInDays = null,
            milestones = listOf(
                "Buy a second-hand camera",
                "Shoot a first roll",
                "Get it developed",
                "Learn to read the light",
                "Print something worth framing",
            ),
            completedCount = 2,
        ),
        SampleGoal(
            title = "Get my savings in order",
            why = "I'd like to stop guessing whether I can afford things.",
            emoji = "💰",
            areaName = "Finance",
            priority = GoalPriority.Maintenance,
            status = GoalStatus.Paused,
            daysAgoCreated = 58,
            daysSinceProgress = 17,
            targetInDays = null,
            milestones = listOf(
                "List every subscription",
                "Cancel what I don't use",
                "Set up an automatic transfer",
                "Build one month of buffer",
            ),
            completedCount = 2,
        ),
        SampleGoal(
            title = "Run 5K without stopping",
            why = "Start somewhere.",
            emoji = "🏅",
            areaName = "Health",
            priority = GoalPriority.Primary,
            status = GoalStatus.Completed,
            daysAgoCreated = 88,
            daysSinceProgress = 6,
            targetInDays = null,
            milestones = listOf(
                "Walk 5 km",
                "Run 1 km",
                "Run 2 km",
                "Run 3 km",
                "Run 5 km",
            ),
            completedCount = 5,
        ),
        SampleGoal(
            title = "Learn guitar",
            why = "I have wanted to play since I was fifteen.",
            emoji = "🎸",
            areaName = "Creativity",
            priority = GoalPriority.Secondary,
            status = GoalStatus.Archived,
            daysAgoCreated = 94,
            daysSinceProgress = 31,
            targetInDays = null,
            milestones = listOf(
                "Learn basic chords",
                "Learn three songs",
                "Practise twenty hours",
                "Record myself playing",
                "Play for friends",
            ),
            completedCount = 1,
            closingReason = ClosingReason.WrongTiming,
        ),
    )

    /**
     * Back-dates the achievements this dataset has clearly already earned.
     *
     * Without this they stay locked until the user's first action, then all fire
     * at once — five unlocks in a single second, none of which the user did. The
     * dates here are spread to match roughly when each one would really have
     * happened against the seeded history.
     */
    private suspend fun seedAchievements(now: Instant) {
        val earned = listOf(
            AchievementCatalog.FIRST_STEP to 94L,
            AchievementCatalog.NEW_CHAPTER to 38L,
            AchievementCatalog.EXPLORER to 54L,
            AchievementCatalog.MOMENTUM to 44L,
            AchievementCatalog.FINISHER to 6L,
            AchievementCatalog.WITNESS to 6L,
        )
        earned.forEach { (key, daysAgo) ->
            val at = now.minusDaysAgo(daysAgo)
            achievementDao.insert(
                AchievementEntity(achievementKey = key, unlockedAt = at, isNew = false),
            )
            progressEventDao.insert(
                ProgressEventEntity(
                    type = ProgressEventType.AchievementUnlocked,
                    title = AchievementCatalog.find(key)?.title.orEmpty(),
                    subtitle = AchievementCatalog.find(key)?.description,
                    occurredAt = at,
                ),
            )
        }
    }

    private fun Instant.minusDaysAgo(days: Long): Instant = minus(days, ChronoUnit.DAYS)

    /**
     * The last few weeks of written reflections, newest first.
     *
     * Back-dated the same way the achievements are, so the history, the journey
     * and the weekly tallies all agree about when these were written.
     */
    private suspend fun seedReflections(today: LocalDate, now: Instant) {
        data class SeedWeek(
            val weeksAgo: Long,
            val daysAgo: Long,
            val movedForward: String?,
            val didNotMove: String?,
            val surprised: String?,
            val proudOf: String?,
            val nextFocus: String?,
        )

        val weeks = listOf(
            SeedWeek(
                weeksAgo = 1,
                daysAgo = 6,
                movedForward = "Got the landing page copy finished and ran three times.",
                didNotMove = "Spanish. Opened the app twice and closed it twice.",
                surprised = "The 8km run felt easier than the 7km did last week.",
                proudOf = "Finished my first 5K without stopping.",
                nextFocus = "Consistency over intensity. Three runs, two study sessions.",
            ),
            SeedWeek(
                weeksAgo = 2,
                daysAgo = 13,
                movedForward = "Two long runs and the first real draft of the side project.",
                didNotMove = "Reading. The book has been on the same page for a fortnight.",
                surprised = "How much easier the week went after planning it on Sunday.",
                proudOf = "Went out in the rain instead of finding a reason not to.",
                nextFocus = "Protect the Tuesday evening. That is where the work happens.",
            ),
            SeedWeek(
                weeksAgo = 3,
                daysAgo = 20,
                movedForward = "Not much, honestly. Work took the whole week.",
                didNotMove = null,
                surprised = null,
                proudOf = "Kept the Saturday run even after a bad week.",
                nextFocus = "Start smaller. One thing a day.",
            ),
        )

        weeks.forEach { week ->
            reflectionDao.put(
                ReflectionEntity(
                    weekStart = clock.weekStart(today.minusWeeks(week.weeksAgo)),
                    movedForward = week.movedForward,
                    didNotMove = week.didNotMove,
                    surprised = week.surprised,
                    proudOf = week.proudOf,
                    nextFocus = week.nextFocus,
                    createdAt = now.minusDaysAgo(week.daysAgo),
                    updatedAt = now.minusDaysAgo(week.daysAgo),
                ),
            )
            progressEventDao.insert(
                ProgressEventEntity(
                    type = ProgressEventType.ReflectionWritten,
                    title = "Weekly reflection",
                    subtitle = week.proudOf,
                    occurredAt = now.minusDaysAgo(week.daysAgo),
                ),
            )
        }
    }

}
