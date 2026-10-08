package com.milestone.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.theme.LocalReduceMotion
import com.milestone.app.ui.theme.MilestoneMotion
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import com.milestone.app.ui.theme.durationScale

/** Where a milestone sits relative to the user's position in the journey. */
enum class MilestoneNodeState {
    Done,

    /** The next thing to do. Exactly one per goal, and visually dominant. */
    Current,

    Upcoming,
}

/** One row of the timeline. */
data class TimelineMilestone(
    val id: Long,
    val title: String,
    val state: MilestoneNodeState,
    val note: String? = null,
    val metadata: String? = null,
    /** Shows a trophy beside the final milestone of a goal. */
    val isFinal: Boolean = false,
    val quests: List<TimelineQuest> = emptyList(),
)

data class TimelineQuest(
    val id: Long,
    val title: String,
    val isCompleted: Boolean,
)

/**
 * The vertical journey a goal is made of.
 *
 * This is the app's signature component, so a few things are deliberate:
 *
 *  - The connector line is drawn *behind* the nodes by each row painting its own
 *    half-segments. That keeps one continuous line through a list of
 *    variable-height rows, which a single background line can't do.
 *  - Line colour is inherited from the node *above* for the upper half and from
 *    this node for the lower half, so the path visibly "fills in" as the user
 *    progresses rather than changing all at once.
 *  - Completing a node pops it with a single overshoot and a halo that fades out.
 *    Both are suppressed under reduce-motion.
 */
@Composable
fun MilestoneTimeline(
    milestones: List<TimelineMilestone>,
    onToggle: (Long) -> Unit,
    modifier: Modifier = Modifier,
    onLongPress: ((Long) -> Unit)? = null,
    showQuests: Boolean = true,
    onToggleQuest: ((Long) -> Unit)? = null,
    onLongPressQuest: ((Long) -> Unit)? = null,
    onAddQuest: ((Long) -> Unit)? = null,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        milestones.forEachIndexed { index, milestone ->
            MilestoneRow(
                milestone = milestone,
                isFirst = index == 0,
                isLast = index == milestones.lastIndex,
                previousDone = index > 0 &&
                    milestones[index - 1].state == MilestoneNodeState.Done,
                onToggle = { onToggle(milestone.id) },
                onLongPress = onLongPress?.let { { it(milestone.id) } },
                showQuests = showQuests,
                onToggleQuest = onToggleQuest,
                onLongPressQuest = onLongPressQuest,
                onAddQuest = onAddQuest,
            )
        }
    }
}

@Composable
private fun MilestoneRow(
    milestone: TimelineMilestone,
    isFirst: Boolean,
    isLast: Boolean,
    previousDone: Boolean,
    onToggle: () -> Unit,
    onLongPress: (() -> Unit)?,
    showQuests: Boolean,
    onToggleQuest: ((Long) -> Unit)?,
    onLongPressQuest: ((Long) -> Unit)?,
    onAddQuest: ((Long) -> Unit)?,
) {
    val colors = MilestoneTheme.colors
    val isDone = milestone.state == MilestoneNodeState.Done
    val isCurrent = milestone.state == MilestoneNodeState.Current

    val nodeColor = when (milestone.state) {
        MilestoneNodeState.Done -> colors.success
        MilestoneNodeState.Current -> colors.accent
        MilestoneNodeState.Upcoming -> colors.hairline
    }

    // Transient celebration state, driven imperatively because it has to fire on
    // the *transition* into Done rather than track a steady value.
    val reduceMotion = LocalReduceMotion.current
    val pop = remember { Animatable(1f) }
    val halo = remember { Animatable(0f) }
    var wasDone by remember { mutableStateOf(isDone) }

    LaunchedEffect(isDone) {
        if (isDone && !wasDone && !reduceMotion) {
            halo.snapTo(1f)
            pop.snapTo(POP_FROM)
            pop.animateTo(1f, spring(dampingRatio = 0.5f, stiffness = 340f))
            halo.animateTo(0f, tween(durationMillis = HALO_FADE_MS))
        }
        wasDone = isDone
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            // Min intrinsic height lets the connector fill exactly this row,
            // however tall the title wraps to.
            .height(IntrinsicSize.Min),
    ) {
        Box(
            modifier = Modifier
                .width(NODE_COLUMN_WIDTH)
                .fillMaxHeight(),
        ) {
            TimelineConnector(
                topColor = if (previousDone) colors.success else colors.hairline,
                bottomColor = if (isDone) colors.success else colors.hairline,
                drawTop = !isFirst,
                drawBottom = !isLast,
            )

            MilestoneNode(
                state = milestone.state,
                nodeColor = nodeColor,
                haloProgress = halo.value,
                scale = pop.value,
                onClick = onToggle,
                onLongPress = onLongPress,
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }

        Spacer(modifier = Modifier.width(Spacing.md))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else Spacing.xl),
        ) {
            // Nudges the first text line onto the node's centre line.
            Spacer(modifier = Modifier.height(TEXT_TOP_OFFSET))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = milestone.title,
                    style = if (isCurrent) {
                        MilestoneTheme.type.titleLarge
                    } else {
                        MilestoneTheme.type.bodyLarge
                    },
                    color = when (milestone.state) {
                        MilestoneNodeState.Done -> colors.inkFaint
                        MilestoneNodeState.Current -> colors.ink
                        MilestoneNodeState.Upcoming -> colors.inkMuted
                    },
                    textDecoration = if (isDone) TextDecoration.LineThrough else null,
                    fontWeight = if (isCurrent) FontWeight.SemiBold else null,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (milestone.isFinal) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    Text(text = "🏆", style = MilestoneTheme.type.bodyMedium)
                }
            }

            if (isCurrent && milestone.metadata != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = milestone.metadata,
                    style = MilestoneTheme.type.labelMedium,
                    color = colors.accent,
                )
            } else if (milestone.metadata != null) {
                Spacer(modifier = Modifier.height(Spacing.xxs))
                Text(
                    text = milestone.metadata,
                    style = MilestoneTheme.type.labelSmall,
                    color = colors.inkFaint,
                )
            }

            if (milestone.note != null) {
                Spacer(modifier = Modifier.height(Spacing.xs))
                Text(
                    text = milestone.note,
                    style = MilestoneTheme.type.bodySmall,
                    color = colors.inkFaint,
                )
            }

            // Quests only ever appear under the current milestone: showing every
            // sub-task of every step would bury the one thing to do next. The add
            // action lives here for the same reason — offering to add sub-steps to
            // a row whose sub-steps are hidden would be a trap.
            AnimatedVisibility(
                visible = showQuests && isCurrent &&
                    (milestone.quests.isNotEmpty() || onAddQuest != null),
                enter = fadeIn(tween(durationScale(MilestoneMotion.DurationStandard))) +
                    expandVertically(tween(durationScale(MilestoneMotion.DurationStandard))),
                exit = fadeOut(tween(durationScale(MilestoneMotion.DurationQuick))) +
                    shrinkVertically(tween(durationScale(MilestoneMotion.DurationQuick))),
            ) {
                Column(modifier = Modifier.padding(top = Spacing.md)) {
                    milestone.quests.forEach { quest ->
                        QuestRow(
                            quest = quest,
                            onToggle = { onToggleQuest?.invoke(quest.id) },
                            onLongPress = onLongPressQuest?.let { { it(quest.id) } },
                            enabled = onToggleQuest != null,
                        )
                    }
                    if (onAddQuest != null) {
                        TextActionButton(
                            // The first one is an invitation, not a plus sign: a
                            // user who has never used quests needs telling what
                            // this is for.
                            text = if (milestone.quests.isEmpty()) {
                                "Break this into smaller steps"
                            } else {
                                "Add a step"
                            },
                            leadingIcon = MilestoneIcons.Plus,
                            color = MilestoneTheme.colors.inkFaint,
                            onClick = { onAddQuest(milestone.id) },
                            modifier = Modifier.padding(start = QUEST_ACTION_INSET),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Draws the vertical path through this row.
 *
 * A gap is left around the node so the line appears to pass behind it rather than
 * terminate at its edge.
 */
@Composable
private fun TimelineConnector(
    topColor: Color,
    bottomColor: Color,
    drawTop: Boolean,
    drawBottom: Boolean,
) {
    val density = LocalDensity.current
    Canvas(modifier = Modifier.fillMaxHeight().width(NODE_COLUMN_WIDTH)) {
        val centerX = size.width / 2f
        val nodeCenterY = with(density) { NODE_CENTER_Y.toPx() }
        val gap = with(density) { CONNECTOR_GAP.toPx() }
        val strokeWidth = with(density) { CONNECTOR_WIDTH.toPx() }

        if (drawTop) {
            drawLine(
                color = topColor,
                start = Offset(centerX, 0f),
                end = Offset(centerX, nodeCenterY - gap),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
        if (drawBottom) {
            drawLine(
                color = bottomColor,
                start = Offset(centerX, nodeCenterY + gap),
                end = Offset(centerX, size.height),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

/**
 * The node itself: a filled check when done, a ringed dot when current, a hollow
 * circle when still ahead.
 *
 * The current node is the largest of the three and the only one with a halo, which
 * is what makes "where am I" readable at a glance down the list.
 */
@Composable
private fun MilestoneNode(
    state: MilestoneNodeState,
    nodeColor: Color,
    haloProgress: Float,
    scale: Float,
    onClick: () -> Unit,
    onLongPress: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val colors = MilestoneTheme.colors
    val diameter = when (state) {
        MilestoneNodeState.Done -> DONE_DIAMETER
        MilestoneNodeState.Current -> CURRENT_DIAMETER
        MilestoneNodeState.Upcoming -> UPCOMING_DIAMETER
    }

    Box(
        modifier = modifier
            .size(NODE_TOUCH_SIZE)
            .clip(CircleShape)
            .combinedClickable(
                role = Role.Checkbox,
                onClick = onClick,
                onLongClick = onLongPress,
            ),
        contentAlignment = Alignment.Center,
    ) {
        // Fades out after a completion. Also the permanent soft ring on the
        // current node, so it reads as "active" when nothing is animating.
        val haloAlpha = when {
            haloProgress > 0f -> HALO_MAX_ALPHA * haloProgress
            state == MilestoneNodeState.Current -> CURRENT_HALO_ALPHA
            else -> 0f
        }
        if (haloAlpha > 0f) {
            Box(
                modifier = Modifier
                    .size(HALO_DIAMETER)
                    .background(nodeColor.copy(alpha = haloAlpha), CircleShape),
            )
        }

        Box(
            modifier = Modifier
                .size(diameter)
                .scale(scale)
                .background(
                    color = when (state) {
                        MilestoneNodeState.Done -> nodeColor
                        // Opaque background so the connector line is hidden
                        // behind, not visible through, the node.
                        else -> colors.background
                    },
                    shape = CircleShape,
                )
                .border(
                    width = if (state == MilestoneNodeState.Current) 2.dp else 1.5.dp,
                    color = nodeColor,
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            when (state) {
                MilestoneNodeState.Done -> Icon(
                    imageVector = MilestoneIcons.Check,
                    contentDescription = null,
                    tint = if (colors.isDark) colors.background else Color.White,
                    modifier = Modifier.size(12.dp),
                )

                MilestoneNodeState.Current -> Box(
                    modifier = Modifier
                        .size(9.dp)
                        .background(nodeColor, CircleShape),
                )

                MilestoneNodeState.Upcoming -> Unit
            }
        }
    }
}

/** A quest: deliberately small, secondary, and clearly subordinate. */
@Composable
private fun QuestRow(
    quest: TimelineQuest,
    onToggle: () -> Unit,
    onLongPress: (() -> Unit)?,
    enabled: Boolean,
) {
    val colors = MilestoneTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MilestoneTheme.shapes.chip)
            // Long-press to rename or remove, matching how milestones behave one
            // level up.
            .combinedClickable(
                enabled = enabled,
                role = Role.Checkbox,
                onClick = onToggle,
                onLongClick = onLongPress,
            )
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier.size(QUEST_NODE_TOUCH),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(QUEST_NODE_SIZE)
                    .background(
                        color = if (quest.isCompleted) colors.success else colors.background,
                        shape = CircleShape,
                    )
                    .border(
                        width = 1.2.dp,
                        color = if (quest.isCompleted) colors.success else colors.hairline,
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (quest.isCompleted) {
                    Icon(
                        imageVector = MilestoneIcons.Check,
                        contentDescription = null,
                        tint = if (colors.isDark) colors.background else Color.White,
                        modifier = Modifier.size(8.dp),
                    )
                }
            }
        }
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = quest.title,
            style = MilestoneTheme.type.bodyMedium,
            color = if (quest.isCompleted) colors.inkFaint else colors.inkMuted,
            textDecoration = if (quest.isCompleted) TextDecoration.LineThrough else null,
        )
    }
}

private val NODE_COLUMN_WIDTH = 34.dp
private val NODE_TOUCH_SIZE = 34.dp

/** Centre of the node, measured from the top of the row. */
private val NODE_CENTER_Y = 17.dp
private val CONNECTOR_GAP = 16.dp
private val CONNECTOR_WIDTH = 1.5.dp

private val DONE_DIAMETER = 22.dp
private val CURRENT_DIAMETER = 26.dp
private val UPCOMING_DIAMETER = 16.dp
private val HALO_DIAMETER = 34.dp

/** Aligns the first line of the title with [NODE_CENTER_Y]. */
private val TEXT_TOP_OFFSET = 5.dp

/** Lines the add action up with the quest titles rather than their bullets. */
private val QUEST_ACTION_INSET = 4.dp

private val QUEST_NODE_TOUCH = 20.dp
private val QUEST_NODE_SIZE = 14.dp

private const val POP_FROM = 1.3f
private const val HALO_FADE_MS = 650
private const val HALO_MAX_ALPHA = 0.35f
private const val CURRENT_HALO_ALPHA = 0.12f
