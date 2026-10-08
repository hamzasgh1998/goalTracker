package com.milestone.app.ui.screens.profile

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.core.time.AppClock
import com.milestone.app.data.local.entity.LifeAreaEntity
import com.milestone.app.data.local.relation.GoalSummary
import com.milestone.app.data.prefs.AppSettings
import com.milestone.app.data.prefs.UserPreferences
import com.milestone.app.data.backup.BackupFiles
import com.milestone.app.data.repository.AchievementRepository
import com.milestone.app.data.repository.BackupRepository
import com.milestone.app.data.repository.BackupSummary
import com.milestone.app.data.repository.GoalRepository
import com.milestone.app.data.repository.LifeAreaRepository
import com.milestone.app.data.repository.ProfileRepository
import com.milestone.app.data.repository.ReflectionRepository
import com.milestone.app.data.sync.RemoteSyncGateway
import com.milestone.app.domain.model.GoalStatus
import com.milestone.app.notifications.ReflectionReminderScheduler
import com.milestone.app.ui.theme.Accent
import com.milestone.app.ui.theme.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject

/** The handful of numbers worth showing back to someone about their own journey. */
data class JourneyStats(
    val daysSinceStart: Long,
    val goalsCompleted: Int,
    val goalsActive: Int,
    val milestonesCompleted: Int,
    val achievementsUnlocked: Int,
    val achievementsTotal: Int,
    val reflectionsWritten: Int,
)

/** A backup the user has chosen but not yet agreed to restore. */
data class PendingRestore(val uri: Uri, val summary: BackupSummary)

/** Everything the export/restore rows need to render. */
data class BackupUiState(
    val isBusy: Boolean = false,
    /**
     * Set for exactly one frame after an export, for the screen to hand to the
     * share sheet. Cleared by the screen, because only the screen knows whether
     * the chooser actually launched.
     */
    val shareUri: Uri? = null,
    val pendingRestore: PendingRestore? = null,
    /** A sentence to show the user: what happened, or what went wrong. */
    val message: String? = null,
)

data class ProfileUiState(
    val isLoading: Boolean = true,
    val name: String = "",
    val startedOn: LocalDate? = null,
    val settings: AppSettings = AppSettings(),
    val stats: JourneyStats = JourneyStats(0, 0, 0, 0, 0, 0, 0),
    /** True while the rename sheet is open. */
    val isEditingName: Boolean = false,
    val nameDraft: String = "",
    val syncAvailable: Boolean = false,
    /** Areas the user has hidden. Listed so hiding one is never a one-way door. */
    val hiddenAreas: List<LifeAreaEntity> = emptyList(),
    val backup: BackupUiState = BackupUiState(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val profileRepository: ProfileRepository,
    private val preferences: UserPreferences,
    private val lifeAreaRepository: LifeAreaRepository,
    private val backupRepository: BackupRepository,
    private val backupFiles: BackupFiles,
    private val reminders: ReflectionReminderScheduler,
    goalRepository: GoalRepository,
    achievementRepository: AchievementRepository,
    reflectionRepository: ReflectionRepository,
    syncGateway: RemoteSyncGateway,
    private val clock: AppClock,
) : ViewModel() {

    /** Rename sheet state: whether it is open, and the text being typed. */
    private val editing = MutableStateFlow(false to "")

    private val backupState = MutableStateFlow(BackupUiState())

    private val hiddenAreas = lifeAreaRepository.observeAll()
        .map { areas -> areas.filterNot { it.isTracked } }

    private val statsFlow = combine(
        goalRepository.observeOpenSummaries(),
        goalRepository.observeClosedSummaries(),
        achievementRepository.observeAll(),
        reflectionRepository.observeAll(),
        profileRepository.observe(),
    ) { open, closed, achievements, reflections, profile ->
        profile to buildStats(open, closed, achievements.count { it.isUnlocked }, achievements.size, reflections.size)
    }

    val state: StateFlow<ProfileUiState> = combine(
        statsFlow,
        preferences.settings,
        editing,
        hiddenAreas,
        backupState,
    ) { (profile, stats), settings, (isEditing, draft), hidden, backup ->
        val startedOn = profile?.startedAt?.atZone(clock.zone())?.toLocalDate()
        ProfileUiState(
            isLoading = false,
            name = profile?.name.orEmpty(),
            startedOn = startedOn,
            settings = settings,
            // Resolved here rather than in buildStats, which has no profile.
            stats = stats.copy(
                daysSinceStart = startedOn?.let { daysBetween(it, clock.today()) } ?: 0L,
            ),
            isEditingName = isEditing,
            nameDraft = draft,
            hiddenAreas = hidden,
            syncAvailable = syncGateway.isConfigured,
            backup = backup,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = ProfileUiState(),
    )

    private fun buildStats(
        open: List<GoalSummary>,
        closed: List<GoalSummary>,
        unlocked: Int,
        totalAchievements: Int,
        reflections: Int,
    ): JourneyStats {
        val everything = open + closed
        return JourneyStats(
            daysSinceStart = 0,
            goalsCompleted = closed.count { it.goal.status == GoalStatus.Completed },
            goalsActive = open.count { it.goal.status == GoalStatus.Active },
            // Counted across every goal including closed ones: work done on a
            // chapter you later closed is still work you did.
            milestonesCompleted = everything.sumOf { it.completedMilestones },
            achievementsUnlocked = unlocked,
            achievementsTotal = totalAchievements,
            reflectionsWritten = reflections,
        )
    }

    fun onStartEditingName() {
        editing.value = true to state.value.name
    }

    fun onNameDraftChange(value: String) {
        editing.value = true to value
    }

    fun onCancelEditingName() {
        editing.value = false to ""
    }

    fun onConfirmName() {
        val draft = editing.value.second.trim()
        if (draft.isEmpty()) return
        viewModelScope.launch {
            profileRepository.updateName(draft)
            editing.value = false to ""
        }
    }

    /** Brings a hidden life area back. Its goals were never touched. */
    fun onRestoreArea(areaId: Long) {
        viewModelScope.launch { lifeAreaRepository.setTracked(areaId, tracked = true) }
    }

    fun onThemeModeChange(mode: ThemeMode) {
        viewModelScope.launch { preferences.setThemeMode(mode) }
    }

    fun onAccentChange(accent: Accent) {
        viewModelScope.launch { preferences.setAccent(accent) }
    }

    /** `null` hands control back to the system animation setting. */
    fun onReduceMotionChange(value: Boolean?) {
        viewModelScope.launch { preferences.setReduceMotion(value) }
    }

    fun onQuestsEnabledChange(value: Boolean) {
        viewModelScope.launch { preferences.setQuestsEnabled(value) }
    }

    // ------------------------------------------------------------- reminders

    /**
     * Turns the weekly nudge on or off.
     *
     * The notification permission is the screen's job, not this one's: by the
     * time this is called the user has either granted it or declined, and the
     * setting is honoured either way — a reminder that cannot post is a setting
     * that looks broken, so the screen only switches it on once it can.
     */
    fun onReminderChange(enabled: Boolean) {
        viewModelScope.launch {
            preferences.setReflectionReminder(enabled)
            val hour = preferences.settings.first().reminderHour
            reminders.apply(enabled = enabled, hour = hour)
        }
    }

    // ---------------------------------------------------------------- backup

    /**
     * Writes the whole database to a file and hands the screen a URI to share.
     *
     * Everything the user has written lives on this device and nowhere else, so
     * this is the only way they get a copy they control.
     */
    fun onExport() {
        if (backupState.value.isBusy) return
        backupState.value = BackupUiState(isBusy = true)

        viewModelScope.launch {
            val result = runCatching {
                backupFiles.write(backupRepository.suggestedFileName(), backupRepository.export())
            }
            backupState.value = result.fold(
                onSuccess = { uri -> BackupUiState(shareUri = uri) },
                onFailure = { BackupUiState(message = "Could not write the backup file.") },
            )
        }
    }

    /** Called once the share sheet has been launched, so it is not launched twice. */
    fun onShareHandled() {
        backupState.value = backupState.value.copy(shareUri = null)
    }

    /**
     * Reads a chosen file and describes it, without changing anything yet.
     *
     * Restoring replaces everything, so the user is told what is in the file and
     * what it will cost before they agree rather than after.
     */
    fun onBackupChosen(uri: Uri) {
        backupState.value = BackupUiState(isBusy = true)

        viewModelScope.launch {
            val result = runCatching {
                backupRepository.inspect(backupFiles.read(uri))
            }
            backupState.value = result.fold(
                onSuccess = { summary ->
                    BackupUiState(pendingRestore = PendingRestore(uri, summary))
                },
                onFailure = { error ->
                    BackupUiState(message = error.message ?: "That file could not be read.")
                },
            )
        }
    }

    fun onCancelRestore() {
        backupState.value = BackupUiState()
    }

    fun onConfirmRestore() {
        val pending = backupState.value.pendingRestore ?: return
        backupState.value = backupState.value.copy(isBusy = true)

        viewModelScope.launch {
            val result = runCatching {
                backupRepository.restore(backupFiles.read(pending.uri))
            }
            backupState.value = result.fold(
                onSuccess = { summary ->
                    // The sample data must not come back over the top of a
                    // restored database on the next launch.
                    preferences.setSeededSampleData(true)
                    BackupUiState(message = restoredMessage(summary))
                },
                onFailure = { error ->
                    BackupUiState(message = error.message ?: "The restore did not finish.")
                },
            )
        }
    }

    fun onDismissBackupMessage() {
        backupState.value = backupState.value.copy(message = null)
    }

    private fun restoredMessage(summary: BackupSummary): String = when (summary.goals) {
        0 -> "Restored. That backup had no goals in it."
        1 -> "Restored 1 goal and " + summary.milestones + " milestones."
        else -> "Restored " + summary.goals + " goals and " +
            summary.milestones + " milestones."
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}

/** Days between two dates, floored at zero. */
private fun daysBetween(from: LocalDate, to: LocalDate): Long =
    ChronoUnit.DAYS.between(from, to).coerceAtLeast(0)
