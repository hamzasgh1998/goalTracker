package com.milestone.app.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.milestone.app.ui.model.LifeAreaUi
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * A life area as a progress ring with its emoji at the centre.
 *
 * The emoji sits inside the ring rather than beside it, which makes the whole
 * thing read as one object and lets a row of these work as a visual index of the
 * user's life. The percentage goes underneath so the glyph stays unobstructed.
 */
@Composable
fun LifeAreaOrb(
    area: LifeAreaUi,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: androidx.compose.ui.unit.Dp = 76.dp,
) {
    Column(
        modifier = modifier
            .width(ORB_COLUMN_WIDTH)
            .clip(MilestoneShapes.card)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Spacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ProgressRing(
            progress = area.progress,
            diameter = diameter,
            strokeWidth = 5.dp,
            color = area.color,
            label = "${area.name}: ${area.progressPercent}% complete, " +
                "${area.goalCount} goals",
        ) {
            Text(text = area.emoji, style = MilestoneTheme.type.headlineMedium)
        }

        Spacer(modifier = Modifier.height(Spacing.md))

        Text(
            text = area.name,
            style = MilestoneTheme.type.labelLarge,
            color = MilestoneTheme.colors.ink,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )

        Spacer(modifier = Modifier.height(2.dp))

        Text(
            // Goal count rather than percentage: for an area, "how much am I
            // carrying here" is the more useful number, and the ring already
            // shows the proportion.
            text = if (area.goalCount == 1) "1 goal" else "${area.goalCount} goals",
            style = MilestoneTheme.type.labelSmall,
            color = MilestoneTheme.colors.inkFaint,
        )
    }
}

private val ORB_COLUMN_WIDTH = 92.dp
