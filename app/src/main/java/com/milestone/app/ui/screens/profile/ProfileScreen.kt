package com.milestone.app.ui.screens.profile

import android.Manifest
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.milestone.app.data.repository.BackupSummary
import com.milestone.app.ui.components.Eyebrow
import com.milestone.app.ui.components.Hairline
import com.milestone.app.ui.components.IconActionButton
import com.milestone.app.ui.components.MilestoneCard
import com.milestone.app.ui.components.MilestoneTextField
import com.milestone.app.ui.components.PrimaryButton
import com.milestone.app.ui.components.SectionHeader
import com.milestone.app.ui.components.SelectableChip
import com.milestone.app.ui.components.SunkenPanel
import com.milestone.app.ui.components.TextActionButton
import com.milestone.app.ui.icons.MilestoneIcons
import com.milestone.app.ui.theme.Accent
import com.milestone.app.ui.theme.MilestoneShapes
import com.milestone.app.ui.theme.MilestoneTextStyles
import com.milestone.app.ui.theme.MilestoneTheme
import com.milestone.app.ui.theme.Spacing
import com.milestone.app.ui.theme.ThemeMode
import com.milestone.app.ui.theme.motionTween
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Profile: who the user is, what they have done, and how the app behaves.
 *
 * The record comes first and the settings after, because the interesting thing on
 * this screen is the history — "148 days, 23 milestones" is the payoff for using
 * the app at all. Settings are a utility you visit twice.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ProfileScreen(
    onOpenAchievements: () -> Unit,
    onOpenPastChapters: () -> Unit,
    onOpenReflection: () -> Unit,
    onOpenReflectionHistory: () -> Unit,
    contentPadding: PaddingValues,
    viewModel: ProfileViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val colors = MilestoneTheme.colors

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .statusBarsPadding()
            .padding(horizontal = Spacing.gutter)
            .padding(top = Spacing.screenTop),
    ) {
        Eyebrow(text = "You")
        Spacer(modifier = Modifier.height(Spacing.sm))

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = state.name.ifBlank { "Your journey" },
                style = MilestoneTheme.type.displayMedium,
                color = colors.ink,
                modifier = Modifier.weight(1f),
            )
            IconActionButton(
                icon = MilestoneIcons.Edit,
                contentDescription = "Change your name",
                onClick = viewModel::onStartEditingName,
            )
        }

        state.startedOn?.let { started ->
            Text(
                text = "Started " + DATE_FORMAT.format(started),
                style = MilestoneTheme.type.bodyMedium,
                color = colors.inkMuted,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.section))

        StatsBlock(stats = state.stats)

        Spacer(modifier = Modifier.height(Spacing.section))

        SectionHeader(eyebrow = "Look back")
        Spacer(modifier = Modifier.height(Spacing.lg))

        NavigationRow(
            icon = MilestoneIcons.Sparkle,
            title = "Achievements",
            subtitle = state.stats.achievementsUnlocked.toString() + " of " +
                state.stats.achievementsTotal + " earned",
            onClick = onOpenAchievements,
        )
        NavigationRow(
            icon = MilestoneIcons.Book,
            title = "Past chapters",
            subtitle = "Goals you finished or set down",
            onClick = onOpenPastChapters,
        )
        // The Home prompt is dismissible and only appears late in the week, so
        // without this there are days when reflection is unreachable entirely.
        NavigationRow(
            icon = MilestoneIcons.Pen,
            title = "Weekly reflection",
            subtitle = "Sit down with this week",
            onClick = onOpenReflection,
        )
        // Without this the reflections were write-only: the entry point always
        // opened the current week, so everything written earlier was unreachable.
        if (state.stats.reflectionsWritten > 0) {
            NavigationRow(
                icon = MilestoneIcons.Journey,
                title = "Past reflections",
                subtitle = if (state.stats.reflectionsWritten == 1) {
                    "1 week written"
                } else {
                    state.stats.reflectionsWritten.toString() + " weeks written"
                },
                onClick = onOpenReflectionHistory,
            )
        }

        Spacer(modifier = Modifier.height(Spacing.section))

        SectionHeader(eyebrow = "Appearance")
        Spacer(modifier = Modifier.height(Spacing.lg))

        Text(text = "Theme", style = MilestoneTheme.type.titleMedium, color = colors.ink)
        Spacer(modifier = Modifier.height(Spacing.md))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            ThemeMode.entries.forEach { mode ->
                SelectableChip(
                    text = mode.label,
                    selected = state.settings.themeMode == mode,
                    onClick = { viewModel.onThemeModeChange(mode) },
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.xxl))

        Text(text = "Accent", style = MilestoneTheme.type.titleMedium, color = colors.ink)
        Spacer(modifier = Modifier.height(Spacing.md))
        AccentPicker(selected = state.settings.accent, onSelect = viewModel::onAccentChange)

        Spacer(modifier = Modifier.height(Spacing.section))

        SectionHeader(eyebrow = "Behaviour")
        Spacer(modifier = Modifier.height(Spacing.lg))

        SettingSwitch(
            title = "Quests",
            description = "Break a milestone into smaller steps. Off keeps the app " +
                "to goals and milestones only.",
            checked = state.settings.questsEnabled,
            onCheckedChange = viewModel::onQuestsEnabledChange,
        )

        Hairline()

        SettingSwitch(
            title = "Reduce motion",
            description = when (state.settings.reduceMotion) {
                null -> "Following your system animation setting. Long-press to reset."
                true -> "Animations are off."
                false -> "Animations are on, whatever the system says."
            },
            checked = state.settings.reduceMotion == true,
            onCheckedChange = { viewModel.onReduceMotionChange(it) },
            // Long-press hands control back to the system, rather than hiding a
            // third state inside a two-state switch.
            onLongPress = { viewModel.onReduceMotionChange(null) },
        )

        if (state.hiddenAreas.isNotEmpty()) {
            Spacer(modifier = Modifier.height(Spacing.section))
            SectionHeader(eyebrow = "Hidden life areas")
            Spacer(modifier = Modifier.height(Spacing.sm))
            Text(
                text = "Their goals are untouched. Bring one back any time.",
                style = MilestoneTheme.type.bodySmall,
                color = colors.inkFaint,
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            state.hiddenAreas.forEach { area ->
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(text = area.emoji, style = MilestoneTheme.type.bodyLarge)
                        Spacer(modifier = Modifier.width(Spacing.md))
                        Text(
                            text = area.name,
                            style = MilestoneTheme.type.bodyLarge,
                            color = colors.inkMuted,
                            modifier = Modifier.weight(1f),
                        )
                        TextActionButton(
                            text = "Show again",
                            onClick = { viewModel.onRestoreArea(area.id) },
                        )
                    }
                    Hairline()
                }
            }
        }

        Spacer(modifier = Modifier.height(Spacing.section))

        DataAndRemindersSection(state = state, viewModel = viewModel)

        Spacer(modifier = Modifier.height(Spacing.section))

        AboutBlock(syncAvailable = state.syncAvailable)

        Spacer(
            modifier = Modifier.height(
                contentPadding.calculateBottomPadding() + Spacing.section,
            ),
        )
    }

    if (state.isEditingName) {
        NameSheet(
            draft = state.nameDraft,
            onDraftChange = viewModel::onNameDraftChange,
            onConfirm = viewModel::onConfirmName,
            onDismiss = viewModel::onCancelEditingName,
        )
    }
}

/**
 * Export, restore, and the weekly reminder.
 *
 * Grouped together because they are the three settings that reach outside the
 * app: two that move the user's data off the device at their own request, and one
 * that lets the app reach back in.
 */
@Composable
private fun DataAndRemindersSection(
    state: ProfileUiState,
    viewModel: ProfileViewModel,
) {
    val colors = MilestoneTheme.colors
    val context = LocalContext.current

    // Android 13 made notifications opt-in. The switch only turns on once the
    // permission is actually granted, so it can never sit on while silently
    // posting nothing.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted -> viewModel.onReminderChange(granted) }

    val pickBackup = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let(viewModel::onBackupChosen) }

    // Handing the file to the share sheet is the screen's job: only it has an
    // Activity to start the chooser from.
    LaunchedEffect(state.backup.shareUri) {
        val uri = state.backup.shareUri ?: return@LaunchedEffect
        val send = Intent(Intent.ACTION_SEND).apply {
            type = BACKUP_MIME_TYPE
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Milestone backup")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        runCatching {
            context.startActivity(Intent.createChooser(send, "Save your backup"))
        }
        viewModel.onShareHandled()
    }

    SectionHeader(eyebrow = "Your data")
    Spacer(modifier = Modifier.height(Spacing.sm))
    Text(
        text = "Everything lives on this device. A backup is the only copy you " +
            "keep if you lose it.",
        style = MilestoneTheme.type.bodySmall,
        color = colors.inkFaint,
    )
    Spacer(modifier = Modifier.height(Spacing.lg))

    NavigationRow(
        icon = MilestoneIcons.Share,
        title = "Export a backup",
        subtitle = if (state.backup.isBusy) "Working…" else "A single file you can keep anywhere",
        onClick = { if (!state.backup.isBusy) viewModel.onExport() },
    )
    NavigationRow(
        icon = MilestoneIcons.Restore,
        title = "Restore from a backup",
        subtitle = "Replaces everything on this device",
        onClick = { if (!state.backup.isBusy) pickBackup.launch(BACKUP_PICKER_TYPES) },
    )

    state.backup.message?.let { message ->
        Spacer(modifier = Modifier.height(Spacing.lg))
        SunkenPanel {
            Text(
                text = message,
                style = MilestoneTheme.type.bodyMedium,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.height(Spacing.sm))
            TextActionButton(text = "OK", onClick = viewModel::onDismissBackupMessage)
        }
    }

    Spacer(modifier = Modifier.height(Spacing.section))

    SectionHeader(eyebrow = "Reminders")
    Spacer(modifier = Modifier.height(Spacing.lg))

    SettingSwitch(
        title = "Weekly reflection",
        description = if (state.settings.reflectionReminder) {
            "Sunday evening, unless you have already written that week."
        } else {
            "A single nudge on a Sunday evening. Nothing else."
        },
        checked = state.settings.reflectionReminder,
        onCheckedChange = { wantsOn ->
            when {
                !wantsOn -> viewModel.onReminderChange(false)
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                    notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
                else -> viewModel.onReminderChange(true)
            }
        },
    )

    state.backup.pendingRestore?.let { pending ->
        RestoreSheet(
            summary = pending.summary,
            onConfirm = viewModel::onConfirmRestore,
            onDismiss = viewModel::onCancelRestore,
        )
    }
}

/**
 * Confirms a restore by describing both sides of the trade.
 *
 * A restore is a replacement, not a merge, so the thing the user most needs to
 * know is what they are giving up — which is stated first, before what they get.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RestoreSheet(
    summary: BackupSummary,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val colors = MilestoneTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        shape = MilestoneShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.section),
        ) {
            Text(
                text = "Replace everything with this backup?",
                style = MilestoneTheme.type.headlineLarge,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.height(Spacing.md))
            Text(
                text = "Whatever is on this device now will be gone. There is no " +
                    "merge — a backup puts the app back to the moment it was made.",
                style = MilestoneTheme.type.bodyMedium,
                color = colors.inkMuted,
            )

            Spacer(modifier = Modifier.height(Spacing.xxl))

            SunkenPanel {
                summary.createdAt?.let { created ->
                    Eyebrow(text = "Made " + DATE_FORMAT.format(created.atZone(ZoneId.systemDefault()).toLocalDate()))
                    Spacer(modifier = Modifier.height(Spacing.sm))
                }
                Text(
                    text = summary.goals.toString() + " goals · " +
                        summary.milestones + " milestones · " +
                        summary.reflections + " reflections",
                    style = MilestoneTheme.type.bodyMedium,
                    color = colors.ink,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.xxl))

            PrimaryButton(text = "Keep what I have", onClick = onDismiss)
            Spacer(modifier = Modifier.height(Spacing.md))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextActionButton(
                    text = "Replace with this backup",
                    color = colors.stalled,
                    onClick = onConfirm,
                )
            }
        }
    }
}

/** What the export writes. */
private const val BACKUP_MIME_TYPE = "application/json"

/**
 * What the picker will accept.
 *
 * Offering "application/json" alone hides the file on devices whose file manager
 * types a .json as plain text, so plain text is listed too. A wildcard would show
 * every file on the device, which only makes picking the wrong one easier.
 */
private val BACKUP_PICKER_TYPES = arrayOf("application/json", "text/plain")

/**
 * The record, as four numbers.
 *
 * Days-since-start leads because it is the one number that only ever grows, and it
 * reframes everything under it: twenty-three milestones means something different
 * on day 12 than on day 400.
 */
@Composable
private fun StatsBlock(stats: JourneyStats) {
    val colors = MilestoneTheme.colors

    MilestoneCard(contentPadding = Spacing.xxl) {
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                text = stats.daysSinceStart.toString(),
                style = MilestoneTextStyles.Metric,
                color = colors.ink,
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = if (stats.daysSinceStart == 1L) "day in" else "days in",
                style = MilestoneTheme.type.bodyMedium,
                color = colors.inkMuted,
                modifier = Modifier.padding(bottom = 5.dp),
            )
        }

        Spacer(modifier = Modifier.height(Spacing.xl))
        Hairline()
        Spacer(modifier = Modifier.height(Spacing.xl))

        Row(modifier = Modifier.fillMaxWidth()) {
            StatCell(
                value = stats.milestonesCompleted.toString(),
                label = "milestones",
                modifier = Modifier.weight(1f),
            )
            StatCell(
                value = stats.goalsCompleted.toString(),
                label = "goals finished",
                modifier = Modifier.weight(1f),
            )
            StatCell(
                value = stats.goalsActive.toString(),
                label = "in progress",
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun StatCell(value: String, label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(
            text = value,
            style = MilestoneTheme.type.headlineLarge,
            color = MilestoneTheme.colors.ink,
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MilestoneTheme.type.labelSmall,
            color = MilestoneTheme.colors.inkFaint,
        )
    }
}

/**
 * Accent swatches.
 *
 * Each dot is drawn in its own colour so the choice is previewed directly rather
 * than described, and the whole app re-themes the instant one is tapped.
 */
@Composable
private fun AccentPicker(selected: Accent, onSelect: (Accent) -> Unit) {
    val isDark = MilestoneTheme.colors.isDark

    Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
        Accent.entries.forEach { accent ->
            val swatch = if (isDark) accent.dark else accent.light
            val isSelected = accent == selected
            val ring by animateColorAsState(
                targetValue = if (isSelected) swatch else Color.Transparent,
                animationSpec = motionTween(),
                label = "accentRing",
            )

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .border(width = 2.dp, color = ring, shape = CircleShape)
                    .selectable(
                        selected = isSelected,
                        indication = null,
                        interactionSource = null,
                        onClick = { onSelect(accent) },
                    )
                    .semantics { contentDescription = accent.label },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 28.dp else 32.dp)
                        .background(swatch, CircleShape),
                )
            }
        }
    }
}

@Composable
private fun SettingSwitch(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    onLongPress: (() -> Unit)? = null,
) {
    val colors = MilestoneTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MilestoneShapes.control)
            .combinedClickable(
                onClick = { onCheckedChange(!checked) },
                onLongClick = onLongPress,
            )
            .padding(vertical = Spacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MilestoneTheme.type.titleMedium, color = colors.ink)
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = description,
                style = MilestoneTheme.type.bodySmall,
                color = colors.inkFaint,
            )
        }
        Spacer(modifier = Modifier.width(Spacing.lg))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.onAccent,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.inkFaint,
                uncheckedTrackColor = colors.surfaceMuted,
                uncheckedBorderColor = colors.hairline,
            ),
        )
    }
}

@Composable
private fun NavigationRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    val colors = MilestoneTheme.colors

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(MilestoneShapes.control)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.inkMuted,
                modifier = Modifier.size(20.dp),
            )
            Spacer(modifier = Modifier.width(Spacing.lg))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MilestoneTheme.type.titleMedium, color = colors.ink)
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = subtitle,
                    style = MilestoneTheme.type.bodySmall,
                    color = colors.inkFaint,
                )
            }
            Icon(
                imageVector = MilestoneIcons.ChevronRight,
                contentDescription = null,
                tint = colors.inkFaint,
                modifier = Modifier.size(18.dp),
            )
        }
        Hairline()
    }
}

@Composable
private fun AboutBlock(syncAvailable: Boolean) {
    val colors = MilestoneTheme.colors

    Column {
        Eyebrow(text = "About")
        Spacer(modifier = Modifier.height(Spacing.md))
        Text(
            text = "Build your life, one milestone at a time.",
            style = MilestoneTextStyles.Quote,
            color = colors.inkMuted,
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = if (syncAvailable) {
                "Your data is on this device and backed up."
            } else {
                // Said plainly. Local-only is the feature, and it is also why the
                // export above exists: nothing leaves this device unless the user
                // sends it somewhere themselves.
                "Everything lives on this device. Nothing is uploaded anywhere, " +
                    "ever — a backup only leaves if you send it."
            },
            style = MilestoneTheme.type.bodySmall,
            color = colors.inkFaint,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NameSheet(
    draft: String,
    onDraftChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MilestoneTheme.colors.surface,
        shape = MilestoneShapes.sheet,
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = Spacing.gutter)
                .padding(bottom = Spacing.section)
                .imePadding(),
        ) {
            Text(
                text = "What should we call you?",
                style = MilestoneTheme.type.headlineMedium,
                color = MilestoneTheme.colors.ink,
            )
            Spacer(modifier = Modifier.height(Spacing.xl))
            MilestoneTextField(
                value = draft,
                onValueChange = onDraftChange,
                placeholder = "Your name",
                imeAction = ImeAction.Done,
            )
            Spacer(modifier = Modifier.height(Spacing.xxl))
            PrimaryButton(text = "Save", enabled = draft.isNotBlank(), onClick = onConfirm)
            Spacer(modifier = Modifier.height(Spacing.sm))
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                TextActionButton(
                    text = "Cancel",
                    color = MilestoneTheme.colors.inkFaint,
                    onClick = onDismiss,
                )
            }
        }
    }
}

private val DATE_FORMAT: DateTimeFormatter = DateTimeFormatter.ofPattern("d MMMM yyyy")
