package com.milestone.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * An empty state that reads as an invitation rather than an error.
 *
 * Every copy string in this app's empty states names what the user could do next
 * ("Your journey starts with one decision") instead of reporting absence ("No
 * goals yet"). The glyph is set large and low-contrast so it decorates without
 * competing with the heading.
 */
@Composable
fun EmptyState(
    glyph: String,
    headline: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    actionText: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Spacing.xxl, vertical = Spacing.section),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = glyph,
            style = MilestoneTheme.type.displayLarge,
            color = MilestoneTheme.colors.inkFaint,
        )

        Spacer(modifier = Modifier.height(Spacing.xl))

        Text(
            text = headline,
            style = MilestoneTheme.type.headlineLarge,
            color = MilestoneTheme.colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = MAX_TEXT_WIDTH),
        )

        if (body != null) {
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = body,
                style = MilestoneTheme.type.bodyMedium,
                color = MilestoneTheme.colors.inkMuted,
                textAlign = TextAlign.Center,
                modifier = Modifier.widthIn(max = MAX_TEXT_WIDTH),
            )
        }

        if (actionText != null && onAction != null) {
            Spacer(modifier = Modifier.height(Spacing.heading))
            SecondaryButton(text = actionText, onClick = onAction)
        }
    }
}

/** Keeps centred copy to a comfortable measure instead of edge-to-edge. */
private val MAX_TEXT_WIDTH = 300.dp
