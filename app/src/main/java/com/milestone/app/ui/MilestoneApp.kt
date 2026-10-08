package com.milestone.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.onConsumedWindowInsetsChanged
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.milestone.app.ui.navigation.MilestoneBottomBar
import com.milestone.app.ui.navigation.MilestoneNavHost
import com.milestone.app.ui.navigation.Routes
import com.milestone.app.ui.navigation.TopLevelDestination
import com.milestone.app.ui.navigation.switchTab
import com.milestone.app.ui.theme.MilestoneTheme

/**
 * The app shell: background, navigation host and the floating bottom bar.
 *
 * Deliberately not a Material `Scaffold`. The bar floats over the content so that
 * lists scroll behind it, which means the content needs the bar's *measured*
 * height as bottom padding rather than a guessed constant — hence
 * [onSizeChanged] feeding [MilestoneNavHost]. A Scaffold would instead inset the
 * content and leave a dead strip under the bar.
 */
@Composable
fun MilestoneApp(state: AppUiState, openReflection: Boolean = false) {
    val navController = rememberNavController()

    // Keyed on Unit so a configuration change does not push the reflection a
    // second time on top of wherever the user has since navigated.
    LaunchedEffect(Unit) {
        if (openReflection && state.hasOnboarded) {
            navController.navigate(Routes.reflection())
        }
    }
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val currentTab = TopLevelDestination.fromRoute(currentRoute)
    val showBottomBar = currentRoute in TopLevelDestination.routes

    val density = LocalDensity.current
    var barHeight by remember { mutableStateOf(0.dp) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MilestoneTheme.colors.background),
    ) {
        MilestoneNavHost(
            navController = navController,
            startDestination = if (state.hasOnboarded) Routes.HOME else Routes.ONBOARDING,
            // Only the tab screens sit under the bar; pushed screens manage their
            // own insets, so reporting zero for them avoids a phantom gap.
            bottomBarPadding = PaddingValues(
                bottom = if (showBottomBar) barHeight else 0.dp,
            ),
            modifier = Modifier.fillMaxSize(),
        )

        if (showBottomBar) {
            MilestoneBottomBar(
                current = currentTab,
                onSelect = navController::switchTab,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { size ->
                        val measured = with(density) { size.height.toDp() }
                        if (measured != barHeight) barHeight = measured
                    },
            )
        }
    }
}
