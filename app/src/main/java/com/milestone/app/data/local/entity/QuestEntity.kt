package com.milestone.app.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * An optional sub-step of a milestone.
 *
 * The third level exists for people who want it and is invisible to everyone else:
 * a milestone with no quests renders exactly as before, and the UI never asks the
 * user to create one. Goal progress is always measured in milestones, never
 * quests, so adding them can't dilute a goal's percentage.
 */
@Entity(
    tableName = "quest",
    foreignKeys = [
        ForeignKey(
            entity = MilestoneEntity::class,
            parentColumns = ["id"],
            childColumns = ["milestoneId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index(value = ["milestoneId", "sortOrder"])],
)
data class QuestEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val milestoneId: Long,

    val title: String,

    val sortOrder: Int,

    val isCompleted: Boolean = false,

    val completedAt: Instant? = null,

    val createdAt: Instant,
)
