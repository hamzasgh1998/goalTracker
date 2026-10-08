package com.milestone.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing

/**
 * The app's text input.
 *
 * Built on [BasicTextField] rather than Material's `OutlinedTextField`, which
 * brings a floating label, fixed 56dp height and its own type scale — three things
 * this design replaces. Here the label sits above the field as an eyebrow, the
 * box grows with the text, and the serif/sans pairing is preserved.
 *
 * @param maxLines use a high value for reflective answers; 1 for names and titles.
 */
@Composable
fun MilestoneTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    maxLines: Int = 1,
    minHeight: Dp = 56.dp,
    textStyle: TextStyle = MilestoneTheme.type.bodyLarge,
    imeAction: ImeAction = ImeAction.Done,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    supportingText: String? = null,
    trailing: @Composable (() -> Unit)? = null,
) {
    val colors = MilestoneTheme.colors
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()

    val selectionColors = TextSelectionColors(
        handleColor = colors.accent,
        backgroundColor = colors.accent.copy(alpha = 0.25f),
    )

    Column(modifier = modifier) {
        if (label != null) {
            Eyebrow(text = label)
            Spacer(modifier = Modifier.height(Spacing.sm))
        }

        CompositionLocalProvider(LocalTextSelectionColors provides selectionColors) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colors.surface, MilestoneShapes.control)
                    .border(
                        width = if (isFocused) 1.5.dp else 1.dp,
                        color = if (isFocused) colors.accent else colors.hairline,
                        shape = MilestoneShapes.control,
                    )
                    .heightIn(min = minHeight)
                    .padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        textStyle = textStyle.copy(color = colors.ink),
                        cursorBrush = SolidColor(colors.accent),
                        maxLines = maxLines,
                        singleLine = maxLines == 1,
                        interactionSource = interactionSource,
                        keyboardOptions = KeyboardOptions(
                            capitalization = capitalization,
                            imeAction = imeAction,
                        ),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = textStyle,
                            color = colors.inkFaint,
                        )
                    }
                }
                if (trailing != null) {
                    Spacer(modifier = Modifier.width(Spacing.sm))
                    trailing()
                }
            }
        }

        if (supportingText != null) {
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = supportingText,
                style = MilestoneTheme.type.labelSmall,
                color = colors.inkFaint,
                modifier = Modifier.padding(horizontal = Spacing.xs),
            )
        }
    }
}
