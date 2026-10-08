package com.milestone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * A record that an achievement was earned.
 *
 * Only *unlocks* are stored. The catalogue itself lives in code
 * (`domain.achievement.AchievementCatalog`), so adding or re-wording an achievement
 * ships as a code change and needs no data migration — and an unlock the user
 * earned can never be lost by editing its description.
 */
@Entity(
    tableName = "achievement_unlock",
    indices = [Index(value = ["achievementKey"], unique = true)],
)
data class AchievementEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Stable identifier matching a catalogue entry, e.g. "first_step". */
    val achievementKey: String,

    val unlockedAt: Instant,

    /** True until the unlock has been shown to the user once. */
    val isNew: Boolean = true,
)
