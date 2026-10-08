package com.milestone.app

import com.milestone.app.data.prefs.UserPreferences
import com.milestone.app.data.repository.LifeAreaRepository
import com.milestone.app.data.seed.SampleDataSeeder
import com.milestone.app.di.ApplicationScope
import com.milestone.app.notifications.ReflectionReminderScheduler
import com.milestone.app.notifications.ensureNotificationChannel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * First-run setup: seed the default life areas, and in debug builds the sample
 * dataset.
 *
 * [isReady] exists to close a real race. Seeding writes a profile with
 * `hasOnboarded = true`, so a UI that renders before it finishes would briefly
 * decide the user is new and start onboarding, then yank it away mid-animation.
 * The splash screen is held until this flips instead, which costs a few frames on
 * first launch and nothing afterwards.
 */
@Singleton
class AppInitializer @Inject constructor(
    private val lifeAreaRepository: LifeAreaRepository,
    private val sampleDataSeeder: SampleDataSeeder,
    private val preferences: UserPreferences,
    private val reminders: ReflectionReminderScheduler,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val _isReady = MutableStateFlow(false)
    val isReady: StateFlow<Boolean> = _isReady.asStateFlow()

    fun initialize() {
        scope.launch {
            try {
                lifeAreaRepository.ensureDefaults()

                // Both guards matter: the build flag keeps samples out of release,
                // and the preference means clearing them is permanent.
                if (BuildConfig.SEED_SAMPLE_DATA && !preferences.hasSeededSampleData.first()) {
                    sampleDataSeeder.seed()
                    preferences.setSeededSampleData(true)
                }

                // Re-applied every launch rather than only when the setting
                // changes: the system drops scheduled work on a force-stop or a
                // restore to a new device, and a reminder that quietly stopped
                // coming is worse than one that was never offered.
                ensureNotificationChannel(context)
                val settings = preferences.settings.first()
                reminders.apply(settings.reflectionReminder, settings.reminderHour)
            } finally {
                // Never leave the splash screen up because setup failed; an empty
                // app the user can still use beats a permanent loading state.
                _isReady.value = true
            }
        }
    }
}
