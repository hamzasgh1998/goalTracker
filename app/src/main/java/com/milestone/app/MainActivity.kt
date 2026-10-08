package com.milestone.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.milestone.app.notifications.EXTRA_OPEN_REFLECTION
import com.milestone.app.ui.AppViewModel
import com.milestone.app.ui.MilestoneApp
import com.milestone.app.ui.theme.MilestoneTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val appViewModel: AppViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Holds the splash until first-run seeding has settled, so a returning
        // user never sees onboarding flash past.
        splashScreen.setKeepOnScreenCondition { !appViewModel.state.value.isReady }

        setContent {
            val state by appViewModel.state.collectAsStateWithLifecycle()

            MilestoneTheme(
                themeMode = state.settings.themeMode,
                accent = state.settings.accent,
                reduceMotionOverride = state.settings.reduceMotion,
            ) {
                if (state.isReady) {
                    MilestoneApp(
                        state = state,
                        // Only honoured on the first composition: re-opening the
                        // app later should land where the user left off, not back
                        // on a reflection they already dealt with.
                        openReflection = intent?.getBooleanExtra(
                            EXTRA_OPEN_REFLECTION,
                            false,
                        ) == true,
                    )
                }
            }
        }
    }
}
