package com.milestone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTextStyles
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * A small all-caps section marker: "CURRENT SEASON", "TODAY", "MILESTONES".
 *
 * Uppercasing happens here rather than in the source strings, so the copy stays
 * readable at the call site and screen readers get normal capitalisation.
 */
@Composable
fun Eyebrow(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MilestoneTheme.colors.inkFaint,
) {
    Text(
        text = text.uppercase(),
        style = MilestoneTextStyles.Eyebrow,
        color = color,
        modifier = modifier,
    )
}

/**
 * Section heading with an optional trailing action.
 *
 * The eyebrow-plus-title pairing is the app's standard way of opening a section.
 * Using it consistently is most of why unrelated screens feel like one product.
 */
@Composable
fun SectionHeader(
    eyebrow: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Eyebrow(text = eyebrow)
            if (title != null) {
                Spacer(modifier = Modifier.height(Spacing.sm))
                Text(
                    text = title,
                    style = MilestoneTheme.type.headlineMedium,
                    color = MilestoneTheme.colors.ink,
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            trailing()
        }
    }
}

/**
 * The user's own words, set in serif behind a vertical rule.
 *
 * Used for a goal's "why" and for reflection answers. Quoting them visually is a
 * deliberate signal: this sentence belongs to the user, not to the app.
 *
 * The row takes its height from the text ([IntrinsicSize.Min]) so the rule grows
 * with however much the user wrote.
 */
@Composable
fun QuotedText(
    text: String,
    modifier: Modifier = Modifier,
    accent: Color = MilestoneTheme.colors.accent,
    maxLines: Int = Int.MAX_VALUE,
) {
    Row(modifier = modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(RULE_WIDTH)
                .fillMaxHeight()
                .background(
                    color = accent.copy(alpha = 0.45f),
                    shape = MilestoneShapes.pill,
                ),
        )
        Spacer(modifier = Modifier.width(Spacing.lg))
        Text(
            text = text,
            style = MilestoneTextStyles.Quote,
            color = MilestoneTheme.colors.inkMuted,
            maxLines = maxLines,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
    }
}

private val RULE_WIDTH = 3.dp
