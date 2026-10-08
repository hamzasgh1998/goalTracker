package com.milestone.app.data.repository

import androidx.room.withTransaction
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.MilestoneDatabase
import com.milestone.app.data.local.dao.BackupDao
import com.milestone.app.data.local.entity.AchievementEntity
import com.milestone.app.data.local.entity.GoalEntity
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.local.entity.LifeSeasonEntity
import com.milestone.app.data.local.entity.MilestoneEntity
import com.milestone.app.data.local.entity.ProgressEventEntity
import com.milestone.app.data.local.entity.QuestEntity
import com.milestone.app.data.local.entity.ReflectionEntity
import com.milestone.app.data.local.entity.UserProfileEntity
import com.milestone.app.domain.model.ClosingReason
import com.milestone.app.domain.model.GoalPriority
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.domain.model.ProgressEventType
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/** What a backup file turned out to contain. */
data class BackupSummary(
    val goals: Int,
    val milestones: Int,
    val reflections: Int,
    /** When the file was written, or null on an older file that didn't say. */
    val createdAt: Instant?,
)

/** Why a restore could not go ahead. The message is shown to the user as-is. */
class BackupFormatException(message: String) : Exception(message)

/**
 * Export and restore, as a single local JSON file.
 *
 * The app is offline by design, which means the device database is the only copy
 * of everything the user has written — their goals, and more to the point their
 * reflections. Android's own auto-backup helps, but it is silent, not under the
 * user's control, and does not survive moving to a device that isn't signed in to
 * the same account. This is the copy they can hold.
 *
 * Hand-rolled JSON via `org.json` rather than kotlinx-serialization: the format is
 * a stable, explicitly-versioned wire format that this file alone owns, and
 * writing it out by hand keeps the entity classes free to change their Kotlin
 * shape without silently changing the shape of everybody's backups.
 */
@Singleton
class BackupRepository @Inject constructor(
    private val database: MilestoneDatabase,
    private val backupDao: BackupDao,
    private val clock: AppClock,
) {

    // ---------------------------------------------------------------- export

    /** The whole database as JSON text. */
    suspend fun export(): String {
        val root = JSONObject()
        root.put(KEY_FORMAT, FORMAT_VERSION)
        root.put(KEY_CREATED_AT, clock.now().toEpochMilli())
        root.put(KEY_APP, "Milestone")

        root.put(
            "profile",
            backupDao.profiles().toArray { profile ->
                JSONObject().apply {
                    put("id", profile.id)
                    put("name", profile.name)
                    put("startedAt", profile.startedAt.toEpochMilli())
                    put("hasOnboarded", profile.hasOnboarded)
                }
            },
        )

        root.put(
            "lifeAreas",
            backupDao.lifeAreas().toArray { area ->
                JSONObject().apply {
                    put("id", area.id)
                    put("name", area.name)
                    put("emoji", area.emoji)
                    put("colorArgb", area.colorArgb)
                    put("isTracked", area.isTracked)
                    put("isBuiltIn", area.isBuiltIn)
                    put("sortOrder", area.sortOrder)
                }
            },
        )

        root.put(
            "seasons",
            backupDao.seasons().toArray { season ->
                JSONObject().apply {
                    put("id", season.id)
                    put("title", season.title)
                    put("theme", season.theme)
                    put("intention", season.intention)
                    put("startDate", season.startDate.toString())
                    putOrNull("endDate", season.endDate?.toString())
                    put("isActive", season.isActive)
                }
            },
        )

        root.put(
            "goals",
            backupDao.goals().toArray { goal ->
                JSONObject().apply {
                    put("id", goal.id)
                    put("title", goal.title)
                    put("why", goal.why)
                    put("lifeAreaId", goal.lifeAreaId)
                    putOrNull("seasonId", goal.seasonId)
                    put("emoji", goal.emoji)
                    putOrNull("targetDate", goal.targetDate?.toString())
                    put("status", goal.status.name)
                    put("priority", goal.priority.name)
                    put("createdAt", goal.createdAt.toEpochMilli())
                    putOrNull("lastProgressAt", goal.lastProgressAt?.toEpochMilli())
                    putOrNull("completedAt", goal.completedAt?.toEpochMilli())
                    putOrNull("closingReason", goal.closingReason?.name)
                    putOrNull("closingNote", goal.closingNote)
                    putOrNull("archivedAt", goal.archivedAt?.toEpochMilli())
                }
            },
        )

        root.put(
            "milestones",
            backupDao.milestones().toArray { milestone ->
                JSONObject().apply {
                    put("id", milestone.id)
                    put("goalId", milestone.goalId)
                    put("title", milestone.title)
                    putOrNull("note", milestone.note)
                    put("sortOrder", milestone.sortOrder)
                    put("isCompleted", milestone.isCompleted)
                    putOrNull("completedAt", milestone.completedAt?.toEpochMilli())
                    putOrNull("dueDate", milestone.dueDate?.toString())
                    put("createdAt", milestone.createdAt.toEpochMilli())
                }
            },
        )

        root.put(
            "quests",
            backupDao.quests().toArray { quest ->
                JSONObject().apply {
                    put("id", quest.id)
                    put("milestoneId", quest.milestoneId)
                    put("title", quest.title)
                    put("sortOrder", quest.sortOrder)
                    put("isCompleted", quest.isCompleted)
                    putOrNull("completedAt", quest.completedAt?.toEpochMilli())
                    put("createdAt", quest.createdAt.toEpochMilli())
                }
            },
        )

        root.put(
            "reflections",
            backupDao.reflections().toArray { reflection ->
                JSONObject().apply {
                    put("id", reflection.id)
                    put("weekStart", reflection.weekStart.toString())
                    putOrNull("movedForward", reflection.movedForward)
                    putOrNull("didNotMove", reflection.didNotMove)
                    putOrNull("surprised", reflection.surprised)
                    putOrNull("proudOf", reflection.proudOf)
                    putOrNull("nextFocus", reflection.nextFocus)
                    put("createdAt", reflection.createdAt.toEpochMilli())
                    put("updatedAt", reflection.updatedAt.toEpochMilli())
                }
            },
        )

        root.put(
            "achievements",
            backupDao.achievements().toArray { unlock ->
                JSONObject().apply {
                    put("id", unlock.id)
                    put("achievementKey", unlock.achievementKey)
                    put("unlockedAt", unlock.unlockedAt.toEpochMilli())
                    put("isNew", unlock.isNew)
                }
            },
        )

        root.put(
            "progressEvents",
            backupDao.progressEvents().toArray { event ->
                JSONObject().apply {
                    put("id", event.id)
                    put("type", event.type.name)
                    put("title", event.title)
                    putOrNull("subtitle", event.subtitle)
                    putOrNull("goalId", event.goalId)
                    putOrNull("milestoneId", event.milestoneId)
                    putOrNull("questId", event.questId)
                    putOrNull("seasonId", event.seasonId)
                    putOrNull("accentArgb", event.accentArgb)
                    put("occurredAt", event.occurredAt.toEpochMilli())
                }
            },
        )

        return root.toString(2)
    }

    /** "milestone-backup-2026-10-08.json" */
    fun suggestedFileName(): String = "milestone-backup-" + clock.today() + ".json"

    // --------------------------------------------------------------- restore

    /**
     * Reads a file without changing anything, so the user can be told what they
     * are about to replace before they agree to it.
     */
    fun inspect(json: String): BackupSummary {
        val root = parse(json)
        return BackupSummary(
            goals = root.optJSONArray("goals")?.length() ?: 0,
            milestones = root.optJSONArray("milestones")?.length() ?: 0,
            reflections = root.optJSONArray("reflections")?.length() ?: 0,
            createdAt = root.optLong(KEY_CREATED_AT, 0L)
                .takeIf { it > 0L }
                ?.let(Instant::ofEpochMilli),
        )
    }

    /**
     * Replaces the database with the contents of [json].
     *
     * Replace, not merge. Merging two histories would mean inventing answers to
     * questions the user never asked — which of two edits to the same goal wins,
     * what to do with a milestone whose goal exists on only one side. A backup
     * restores the device to a moment in time, and the confirmation says so.
     *
     * The whole thing is one transaction, so a file that turns out to be corrupt
     * halfway through leaves the existing data exactly as it was.
     */
    suspend fun restore(json: String): BackupSummary {
        val root = parse(json)

        val profiles = root.rows("profile") { row ->
            UserProfileEntity(
                id = row.getLong("id"),
                name = row.getString("name"),
                startedAt = Instant.ofEpochMilli(row.getLong("startedAt")),
                hasOnboarded = row.optBoolean("hasOnboarded", false),
            )
        }
        val lifeAreas = root.rows("lifeAreas") { row ->
            LifeAreaEntity(
                id = row.getLong("id"),
                name = row.getString("name"),
                emoji = row.getString("emoji"),
                colorArgb = row.getLong("colorArgb"),
                isTracked = row.optBoolean("isTracked", true),
                isBuiltIn = row.optBoolean("isBuiltIn", false),
                sortOrder = row.optInt("sortOrder", 0),
            )
        }
        val seasons = root.rows("seasons") { row ->
            LifeSeasonEntity(
                id = row.getLong("id"),
                title = row.getString("title"),
                theme = row.getString("theme"),
                intention = row.getString("intention"),
                startDate = LocalDate.parse(row.getString("startDate")),
                endDate = row.optStringOrNull("endDate")?.let(LocalDate::parse),
                isActive = row.optBoolean("isActive", false),
            )
        }
        val goals = root.rows("goals") { row ->
            GoalEntity(
                id = row.getLong("id"),
                title = row.getString("title"),
                why = row.optString("why", ""),
                lifeAreaId = row.getLong("lifeAreaId"),
                seasonId = row.optLongOrNull("seasonId"),
                emoji = row.optString("emoji", "🎯"),
                targetDate = row.optStringOrNull("targetDate")?.let(LocalDate::parse),
                status = enumOf(row.optString("status"), GoalStatus.Active),
                priority = enumOf(row.optString("priority"), GoalPriority.Secondary),
                createdAt = Instant.ofEpochMilli(row.getLong("createdAt")),
                lastProgressAt = row.optLongOrNull("lastProgressAt")?.let(Instant::ofEpochMilli),
                completedAt = row.optLongOrNull("completedAt")?.let(Instant::ofEpochMilli),
                closingReason = row.optStringOrNull("closingReason")
                    ?.let { enumOf<ClosingReason>(it, ClosingReason.Other) },
                closingNote = row.optStringOrNull("closingNote"),
                archivedAt = row.optLongOrNull("archivedAt")?.let(Instant::ofEpochMilli),
            )
        }
        val milestones = root.rows("milestones") { row ->
            MilestoneEntity(
                id = row.getLong("id"),
                goalId = row.getLong("goalId"),
                title = row.getString("title"),
                note = row.optStringOrNull("note"),
                sortOrder = row.optInt("sortOrder", 0),
                isCompleted = row.optBoolean("isCompleted", false),
                completedAt = row.optLongOrNull("completedAt")?.let(Instant::ofEpochMilli),
                dueDate = row.optStringOrNull("dueDate")?.let(LocalDate::parse),
                createdAt = Instant.ofEpochMilli(row.getLong("createdAt")),
            )
        }
        val quests = root.rows("quests") { row ->
            QuestEntity(
                id = row.getLong("id"),
                milestoneId = row.getLong("milestoneId"),
                title = row.getString("title"),
                sortOrder = row.optInt("sortOrder", 0),
                isCompleted = row.optBoolean("isCompleted", false),
                completedAt = row.optLongOrNull("completedAt")?.let(Instant::ofEpochMilli),
                createdAt = Instant.ofEpochMilli(row.getLong("createdAt")),
            )
        }
        val reflections = root.rows("reflections") { row ->
            ReflectionEntity(
                id = row.getLong("id"),
                weekStart = LocalDate.parse(row.getString("weekStart")),
                movedForward = row.optStringOrNull("movedForward"),
                didNotMove = row.optStringOrNull("didNotMove"),
                surprised = row.optStringOrNull("surprised"),
                proudOf = row.optStringOrNull("proudOf"),
                nextFocus = row.optStringOrNull("nextFocus"),
                createdAt = Instant.ofEpochMilli(row.getLong("createdAt")),
                updatedAt = Instant.ofEpochMilli(row.getLong("updatedAt")),
            )
        }
        val achievements = root.rows("achievements") { row ->
            AchievementEntity(
                id = row.getLong("id"),
                achievementKey = row.getString("achievementKey"),
                unlockedAt = Instant.ofEpochMilli(row.getLong("unlockedAt")),
                isNew = row.optBoolean("isNew", false),
            )
        }
        val progressEvents = root.rows("progressEvents") { row ->
            ProgressEventEntity(
                id = row.getLong("id"),
                type = enumOf(row.optString("type"), ProgressEventType.GoalCreated),
                title = row.getString("title"),
                subtitle = row.optStringOrNull("subtitle"),
                goalId = row.optLongOrNull("goalId"),
                milestoneId = row.optLongOrNull("milestoneId"),
                questId = row.optLongOrNull("questId"),
                seasonId = row.optLongOrNull("seasonId"),
                accentArgb = row.optLongOrNull("accentArgb"),
                occurredAt = Instant.ofEpochMilli(row.getLong("occurredAt")),
            )
        }

        database.withTransaction {
            backupDao.clearProgressEvents()
            backupDao.clearAchievements()
            backupDao.clearReflections()
            backupDao.clearQuests()
            backupDao.clearMilestones()
            backupDao.clearGoals()
            backupDao.clearSeasons()
            backupDao.clearLifeAreas()
            backupDao.clearProfiles()

            // Parents before children, so every foreign key has something to
            // point at by the time it is written.
            backupDao.insertProfiles(profiles)
            backupDao.insertLifeAreas(lifeAreas)
            backupDao.insertSeasons(seasons)
            backupDao.insertGoals(goals)
            backupDao.insertMilestones(milestones)
            backupDao.insertQuests(quests)
            backupDao.insertReflections(reflections)
            backupDao.insertAchievements(achievements)
            backupDao.insertProgressEvents(progressEvents)
        }

        return BackupSummary(
            goals = goals.size,
            milestones = milestones.size,
            reflections = reflections.size,
            createdAt = root.optLong(KEY_CREATED_AT, 0L)
                .takeIf { it > 0L }
                ?.let(Instant::ofEpochMilli),
        )
    }

    /**
     * Turns the text into JSON, refusing anything that isn't one of ours.
     *
     * The checks are there so that picking the wrong file produces a sentence the
     * user can act on, rather than a parse error from three layers down.
     */
    private fun parse(json: String): JSONObject {
        val root = try {
            JSONObject(json)
        } catch (e: Exception) {
            throw BackupFormatException("That file isn't a Milestone backup.")
        }

        if (!root.has(KEY_FORMAT)) {
            throw BackupFormatException("That file isn't a Milestone backup.")
        }
        val format = root.optInt(KEY_FORMAT, 0)
        if (format > FORMAT_VERSION) {
            throw BackupFormatException(
                "That backup was made by a newer version of Milestone. " +
                    "Update the app and try again.",
            )
        }
        return root
    }

    private companion object {
        const val KEY_FORMAT = "formatVersion"
        const val KEY_CREATED_AT = "createdAt"
        const val KEY_APP = "app"

        /** Bump when the shape changes; older files must keep restoring. */
        const val FORMAT_VERSION = 1
    }
}

// --------------------------------------------------------------------- helpers

private fun <T> List<T>.toArray(transform: (T) -> JSONObject): JSONArray =
    JSONArray().also { array -> forEach { array.put(transform(it)) } }

/** `put(key, null)` stores the literal string "null", so absence is explicit. */
private fun JSONObject.putOrNull(key: String, value: Any?) {
    if (value != null) put(key, value)
}

private fun <T> JSONObject.rows(key: String, transform: (JSONObject) -> T): List<T> {
    val array = optJSONArray(key) ?: return emptyList()
    return (0 until array.length()).map { index -> transform(array.getJSONObject(index)) }
}

private fun JSONObject.optStringOrNull(key: String): String? =
    if (isNull(key)) null else optString(key).takeIf { it.isNotEmpty() }

private fun JSONObject.optLongOrNull(key: String): Long? =
    if (isNull(key)) null else optLong(key).takeIf { has(key) }

/**
 * Resolves an enum by name, falling back rather than throwing.
 *
 * A backup written by a build that knew about a status this one doesn't should
 * still restore the user's goals; losing one field is better than losing the file.
 */
private inline fun <reified T : Enum<T>> enumOf(name: String?, fallback: T): T =
    enumValues<T>().firstOrNull { it.name == name } ?: fallback
