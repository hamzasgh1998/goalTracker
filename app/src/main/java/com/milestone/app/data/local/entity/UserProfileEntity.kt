package com.milestone.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant

/**
 * The single local user. There is exactly one row, pinned at [SINGLETON_ID].
 *
 * Kept as a table rather than a preference because the profile is relational — it
 * is the anchor the Journey counts from, and a future sync layer will want to
 * version it alongside everything else.
 */
@Entity(tableName = "user_profile")
data class UserProfileEntity(
    @PrimaryKey
    val id: Long = SINGLETON_ID,

    /** What the user wants to be called. Greeted by name on Home. */
    val name: String,

    /** When they started using the app — "your journey began" on Profile. */
    val startedAt: Instant,

    /** True once onboarding has been completed at least once. */
    val hasOnboarded: Boolean = false,
) {
    companion object {
        const val SINGLETON_ID = 1L
    }
}
