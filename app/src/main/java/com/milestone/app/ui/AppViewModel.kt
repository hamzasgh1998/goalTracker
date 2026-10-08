package com.milestone.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.milestone.app.AppInitializer
import com.milestone.app.data.prefs.AppSettings
import com.milestone.app.data.prefs.UserPreferences
import com.milestone.app.data.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/**
 * App-level state: theming, and whether the user has been through onboarding.
 *
 * [isReady] gates the whole UI. It is false until first-run seeding has finished,
 * because [hasOnboarded] is only trustworthy once the profile row has had its
 * chance to be written — rendering before then would flash onboarding at a
 * returning user.
 */
data class AppUiState(
    val isReady: Boolean = false,
    val settings: AppSettings = AppSettings(),
    val hasOnboarded: Boolean = false,
)

@HiltViewModel
class AppViewModel @Inject constructor(
    preferences: UserPreferences,
    profileRepository: ProfileRepository,
    appInitializer: AppInitializer,
) : ViewModel() {

    val state: StateFlow<AppUiState> = combine(
        appInitializer.isReady,
        preferences.settings,
        profileRepository.observe(),
    ) { isReady, settings, profile ->
        AppUiState(
            isReady = isReady,
            settings = settings,
            hasOnboarded = profile?.hasOnboarded == true,
        )
    }.stateIn(
        scope = viewModelScope,
        // Eagerly: this drives the splash-screen gate, so it must be producing a
        // value before the first frame rather than on first collection.
        started = SharingStarted.Eagerly,
        initialValue = AppUiState(),
    )
}
