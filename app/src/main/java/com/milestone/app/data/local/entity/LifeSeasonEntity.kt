package com.milestone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

/**
 * A bounded stretch of time with a theme: "Autumn 2026 — Build & Grow".
 *
 * Seasons are the mechanism that stops this app becoming a backlog of forty goals.
 * A goal belongs to at most one season, and the Home screen only ever surfaces the
 * active season's primary goals.
 *
 * Exactly one season should have [isActive] set; the DAO enforces that by clearing
 * the flag on all others inside a transaction when a new season starts.
 */
@Entity(
    tableName = "life_season",
    indices = [Index(value = ["isActive"])],
)
data class LifeSeasonEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Period name: "Autumn 2026", "Q4 2026". */
    val title: String,

    /** One-word theme shown large under the title: "Build", "Explore", "Focus". */
    val theme: String,

    /** A sentence on what this season is for, in the user's own words. */
    val intention: String,

    val startDate: LocalDate,

    /** Null for an open-ended season. */
    val endDate: LocalDate?,

    val isActive: Boolean = false,
)
