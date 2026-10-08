package com.milestone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A domain of life that goals hang off: Health, Career, Creativity…
 *
 * The eight defaults are seeded on first run, but they are ordinary rows — the user
 * can add their own, rename them, or stop tracking one. Nothing in the code should
 * assume a fixed set.
 */
@Entity(
    tableName = "life_area",
    indices = [Index(value = ["sortOrder"])],
)
data class LifeAreaEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val name: String,

    /** A single emoji. Cheap, expressive, and renders without any icon assets. */
    val emoji: String,

    /** ARGB packed as a Long, because Room can't persist Compose's `Color`. */
    val colorArgb: Long,

    /**
     * Whether the user tracks this area. Unselected defaults stay in the table so
     * that turning one back on in settings restores its goals and colour.
     */
    val isTracked: Boolean = true,

    /** False for areas the user created, which protects defaults from deletion. */
    val isBuiltIn: Boolean = false,

    val sortOrder: Int = 0,
)
