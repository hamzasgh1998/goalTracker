package com.milestone.app.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.milestone.app.domain.model.ProgressEventType
import java.time.Instant

/**
 * An append-only entry in the user's history, feeding the Journey screen.
 *
 * [title] and [subtitle] are *denormalised snapshots* of how things read at the
 * time. That is intentional: if a goal is renamed or archived, the timeline should
 * still say what actually happened that day. Joining live rows instead would let
 * later edits rewrite the past, and the past is the entire value of this screen.
 *
 * The id columns are kept alongside so entries can still deep-link where the
 * target survives. They carry no foreign keys — history outlives its subjects.
 */
@Entity(
    tableName = "progress_event",
    indices = [
        Index(value = ["occurredAt"]),
        Index(value = ["goalId"]),
    ],
)
data class ProgressEventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    val type: ProgressEventType,

    /** What happened: "Completed Run 5 km". */
    val title: String,

    /** Context: the goal or season it belongs to. */
    val subtitle: String? = null,

    val goalId: Long? = null,
    val milestoneId: Long? = null,
    val questId: Long? = null,
    val seasonId: Long? = null,

    /** Colour of the originating life area, so the timeline can stay colour-coded. */
    val accentArgb: Long? = null,

    val occurredAt: Instant,
)
