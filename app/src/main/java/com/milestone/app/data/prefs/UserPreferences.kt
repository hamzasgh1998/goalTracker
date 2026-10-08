package com.milestone.app.data.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.milestone.app.ui.theme.Accent
import com.milestone.app.ui.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.preferencesStore: DataStore<Preferences> by preferencesDataStore(
    name = "milestone_settings",
)

/** The user's display settings. Everything relational lives in Room instead. */
data class AppSettings(
    val themeMode: ThemeMode = ThemeMode.Default,
    val accent: Accent = Accent.Default,
    /** `null` means "follow the system animation setting". */
    val reduceMotion: Boolean? = null,
    /** Whether the optional third tier (quests) is surfaced in the UI. */
    val questsEnabled: Boolean = true,
    /**
     * Whether to nudge for the weekly reflection.
     *
     * Off by default. A reminder the user did not ask for is the fastest way to
     * make an app about intention feel like an app about nagging.
     */
    val reflectionReminder: Boolean = false,
    /** Hour of the day, local time, the reminder fires on a Sunday. */
    val reminderHour: Int = DEFAULT_REMINDER_HOUR,
)

/** Early Sunday evening: the week is done, and there is still time to act on it. */
const val DEFAULT_REMINDER_HOUR = 19

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val store = context.preferencesStore

    val settings: Flow<AppSettings> = store.data.map { prefs ->
        AppSettings(
            themeMode = ThemeMode.fromName(prefs[Keys.ThemeMode]),
            accent = Accent.fromName(prefs[Keys.Accent]),
            reduceMotion = prefs[Keys.ReduceMotion],
            questsEnabled = prefs[Keys.QuestsEnabled] ?: true,
            reflectionReminder = prefs[Keys.ReflectionReminder] ?: false,
            reminderHour = prefs[Keys.ReminderHour] ?: DEFAULT_REMINDER_HOUR,
        )
    }

    /**
     * Guards one-time seeding of the sample dataset.
     *
     * Kept in preferences rather than inferred from "the database is empty",
     * because a user who deletes every sample goal must not have them silently
     * come back on next launch.
     */
    val hasSeededSampleData: Flow<Boolean> = store.data.map { it[Keys.SeededSample] ?: false }

    /** Week (epoch day of its Monday) whose reflection prompt has been dismissed. */
    val reflectionPromptDismissedWeek: Flow<Long?> =
        store.data.map { it[Keys.ReflectionDismissedWeek] }

    suspend fun setThemeMode(mode: ThemeMode) = edit { it[Keys.ThemeMode] = mode.name }

    suspend fun setAccent(accent: Accent) = edit { it[Keys.Accent] = accent.name }

    suspend fun setReduceMotion(value: Boolean?) = edit { prefs ->
        if (value == null) prefs.remove(Keys.ReduceMotion) else prefs[Keys.ReduceMotion] = value
    }

    suspend fun setQuestsEnabled(value: Boolean) = edit { it[Keys.QuestsEnabled] = value }

    suspend fun setReflectionReminder(value: Boolean) =
        edit { it[Keys.ReflectionReminder] = value }

    suspend fun setReminderHour(hour: Int) =
        edit { it[Keys.ReminderHour] = hour.coerceIn(0, 23) }

    suspend fun setSeededSampleData(value: Boolean) = edit { it[Keys.SeededSample] = value }

    suspend fun dismissReflectionPrompt(weekStartEpochDay: Long) =
        edit { it[Keys.ReflectionDismissedWeek] = weekStartEpochDay }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        store.edit(block)
    }

    private object Keys {
        val ThemeMode = stringPreferencesKey("theme_mode")
        val Accent = stringPreferencesKey("accent")
        val ReduceMotion = booleanPreferencesKey("reduce_motion")
        val QuestsEnabled = booleanPreferencesKey("quests_enabled")
        val SeededSample = booleanPreferencesKey("seeded_sample_data")
        val ReflectionDismissedWeek = longPreferencesKey("reflection_dismissed_week")
        val ReflectionReminder = booleanPreferencesKey("reflection_reminder")
        val ReminderHour = intPreferencesKey("reminder_hour")
    }
}
