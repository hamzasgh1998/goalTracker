package com.milestone.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.milestone.app.ui.theme.MilestoneMotion
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import com.milestone.app.ui.theme.resolve

/**
 * The primary call to action. One per screen, at most.
 *
 * Full-width and pill-shaped, with a press scale instead of a ripple so it feels
 * physical rather than stock.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    trailingIcon: ImageVector? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) PRESSED_SCALE else 1f,
        animationSpec = MilestoneMotion.press<Float>().resolve(),
        label = "primaryPress",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .scale(scale)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(MilestoneShapes.pill)
            .background(MilestoneTheme.colors.accent, MilestoneShapes.pill)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = Spacing.minTouchTarget)
            .padding(horizontal = Spacing.xxl, vertical = Spacing.lg),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = text,
            style = MilestoneTheme.type.labelLarge,
            color = MilestoneTheme.colors.onAccent,
        )
        if (trailingIcon != null) {
            Spacer(modifier = Modifier.width(Spacing.sm))
            Icon(
                imageVector = trailingIcon,
                contentDescription = null,
                tint = MilestoneTheme.colors.onAccent,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

/** Outlined secondary action. Sits beside or under a [PrimaryButton]. */
@Composable
fun SecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    leadingIcon: ImageVector? = null,
    contentColor: Color = MilestoneTheme.colors.ink,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) PRESSED_SCALE else 1f,
        animationSpec = MilestoneMotion.press<Float>().resolve(),
        label = "secondaryPress",
    )

    Row(
        modifier = modifier
            .scale(scale)
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .clip(MilestoneShapes.pill)
            .border(
                BorderStroke(1.dp, MilestoneTheme.colors.hairline),
                MilestoneShapes.pill,
            )
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            )
            .defaultMinSize(minHeight = Spacing.minTouchTarget)
            .padding(horizontal = Spacing.xl, vertical = Spacing.md),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
        }
        Text(
            text = text,
            style = MilestoneTheme.type.labelLarge,
            color = contentColor,
        )
    }
}

/**
 * A bare icon button.
 *
 * The visible glyph is 22dp but the touch target is padded out to the 48dp
 * accessibility minimum.
 */
@Composable
fun IconActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MilestoneTheme.colors.inkMuted,
    enabled: Boolean = true,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) 0.88f else 1f,
        animationSpec = MilestoneMotion.press<Float>().resolve(),
        label = "iconPress",
    )

    Box(
        modifier = modifier
            .size(Spacing.minTouchTarget)
            .clip(MilestoneShapes.pill)
            .clickable(
                enabled = enabled,
                interactionSource = interactionSource,
                indication = null,
                role = Role.Button,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (enabled) tint else tint.copy(alpha = DISABLED_ALPHA),
            modifier = Modifier
                .size(22.dp)
                .scale(scale),
        )
    }
}

/** Low-emphasis inline action: "Skip", "Add milestone", "Not now". */
@Composable
fun TextActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MilestoneTheme.colors.accent,
    leadingIcon: ImageVector? = null,
    enabled: Boolean = true,
) {
    val contentColor = if (enabled) color else color.copy(alpha = DISABLED_ALPHA)

    Row(
        modifier = modifier
            .clip(MilestoneShapes.pill)
            .clickable(role = Role.Button, enabled = enabled, onClick = onClick)
            .defaultMinSize(minHeight = Spacing.minTouchTarget)
            .padding(horizontal = Spacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leadingIcon != null) {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.xs))
        }
        Text(text = text, style = MilestoneTheme.type.labelLarge, color = contentColor)
    }
}

/**
 * A selectable pill: life areas in onboarding, priorities, season themes.
 *
 * Selection is carried by fill *and* border rather than colour alone, so it
 * survives a colour-blind reading.
 */
@Composable
fun SelectableChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    leading: String? = null,
    accent: Color = MilestoneTheme.colors.accent,
) {
    Row(
        modifier = modifier
            .clip(MilestoneShapes.pill)
            .background(
                color = if (selected) accent.copy(alpha = 0.12f) else Color.Transparent,
                shape = MilestoneShapes.pill,
            )
            .border(
                BorderStroke(
                    width = if (selected) 1.5.dp else 1.dp,
                    color = if (selected) accent else MilestoneTheme.colors.hairline,
                ),
                MilestoneShapes.pill,
            )
            .clickable(role = Role.Checkbox, onClick = onClick)
            .defaultMinSize(minHeight = 44.dp)
            .padding(horizontal = Spacing.lg, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Text(text = leading, style = MilestoneTheme.type.bodyLarge)
            Spacer(modifier = Modifier.width(Spacing.sm))
        }
        Text(
            text = text,
            style = MilestoneTheme.type.labelLarge,
            color = if (selected) MilestoneTheme.colors.ink else MilestoneTheme.colors.inkMuted,
        )
    }
}

/** A read-only label: a life area name, a status, a date. */
@Composable
fun Tag(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = MilestoneTheme.colors.inkMuted,
    containerColor: Color = MilestoneTheme.colors.surfaceMuted,
    leading: String? = null,
) {
    Row(
        modifier = modifier
            .clip(MilestoneShapes.pill)
            .background(containerColor, MilestoneShapes.pill)
            .padding(horizontal = Spacing.md, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leading != null) {
            Text(text = leading, style = MilestoneTheme.type.labelSmall)
            Spacer(modifier = Modifier.width(Spacing.xs))
        }
        Text(text = text, style = MilestoneTheme.type.labelMedium, color = color)
    }
}

/** A thin divider at the design system's hairline colour. */
@Composable
fun Hairline(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MilestoneTheme.colors.hairline),
    )
}

private const val PRESSED_SCALE = 0.97f
private const val DISABLED_ALPHA = 0.38f
