package com.milestone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * A grid of emoji to stand in for a goal's icon.
 *
 * Deliberately a fixed palette rather than the system emoji keyboard: a goal's
 * symbol shows up at small sizes all over the app, and a hand-picked set keeps
 * them visually consistent — no skin-tone variants, no glyphs that render as a
 * tofu box on older devices.
 *
 * Shared by goal creation and goal editing so the two never drift apart.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmojiPicker(
    choices: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MilestoneTheme.colors

    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        choices.forEach { emoji ->
            val isSelected = selected == emoji
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(MilestoneShapes.control)
                    .background(
                        color = if (isSelected) colors.accentWash else colors.surface,
                        shape = MilestoneShapes.control,
                    )
                    .border(
                        width = if (isSelected) 1.5.dp else 1.dp,
                        color = if (isSelected) colors.accent else colors.hairline,
                        shape = MilestoneShapes.control,
                    )
                    .selectable(
                        selected = isSelected,
                        onClick = { onSelect(emoji) },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = emoji, style = MilestoneTheme.type.headlineSmall)
            }
        }
    }
}
