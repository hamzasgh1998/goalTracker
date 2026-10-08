package com.milestone.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.milestone.app.ui.screens.achievements.AchievementsScreen
import com.milestone.app.ui.screens.creategoal.CreateGoalScreen
import com.milestone.app.ui.screens.goaldetail.GoalDetailScreen
import com.milestone.app.ui.screens.goals.GoalsScreen
import com.milestone.app.ui.screens.home.HomeScreen
import com.milestone.app.ui.screens.journey.JourneyScreen
import com.milestone.app.ui.screens.lifearea.LifeAreaScreen
import com.milestone.app.ui.screens.map.LifeMapScreen
import com.milestone.app.ui.screens.onboarding.OnboardingScreen
import com.milestone.app.ui.screens.pastchapters.PastChaptersScreen
import com.milestone.app.ui.screens.profile.ProfileScreen
import com.milestone.app.ui.screens.reflection.ReflectionHistoryScreen
import com.milestone.app.ui.screens.reflection.ReflectionScreen
import com.milestone.app.ui.screens.season.SeasonScreen
import com.milestone.app.ui.theme.MilestoneMotion

/**
 * The app's navigation graph.
 *
 * Tab switches cross-fade while pushes slide, which is the standard distinction
 * between "moving sideways" and "going deeper".
 *
 * Note that these transitions use the raw duration constants rather than the
 * reduce-motion-aware `durationScale`: Navigation's transition lambdas are not
 * composable, so they cannot read a composition local. The platform applies its
 * own window-animation scale to navigation anyway, which is the setting that
 * actually matters here — and in-screen animation, which the system does *not*
 * control, does honour the preference.
 */
@Composable
fun MilestoneNavHost(
    navController: NavHostController,
    startDestination: String,
    bottomBarPadding: PaddingValues,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = { fadeIn(tween(MilestoneMotion.DurationStandard)) },
        exitTransition = { fadeOut(tween(MilestoneMotion.DurationQuick)) },
    ) {
        composable(route = Routes.ONBOARDING) {
            OnboardingScreen(
                onFinished = {
                    navController.navigate(Routes.HOME) {
                        // Onboarding must not be reachable by back-press once done.
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                },
            )
        }

        composable(route = Routes.HOME) {
            HomeScreen(
                onOpenGoal = { navController.navigateToGoal(it) },
                onOpenArea = { navController.navigate(Routes.lifeArea(it)) },
                onCreateGoal = { navController.navigate(Routes.CREATE_GOAL) },
                onOpenSeason = { navController.navigate(Routes.SEASON) },
                onOpenReflection = { navController.navigate(Routes.reflection()) },
                onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                onSeeAllGoals = { navController.switchTab(TopLevelDestination.Goals) },
                contentPadding = bottomBarPadding,
            )
        }

        composable(route = Routes.GOALS) {
            GoalsScreen(
                onOpenGoal = { navController.navigateToGoal(it) },
                onCreateGoal = { navController.navigate(Routes.CREATE_GOAL) },
                contentPadding = bottomBarPadding,
            )
        }

        composable(route = Routes.MAP) {
            LifeMapScreen(
                onOpenGoal = { navController.navigateToGoal(it) },
                contentPadding = bottomBarPadding,
            )
        }

        composable(route = Routes.JOURNEY) {
            JourneyScreen(
                onOpenGoal = { navController.navigateToGoal(it) },
                contentPadding = bottomBarPadding,
            )
        }

        composable(route = Routes.PROFILE) {
            ProfileScreen(
                onOpenAchievements = { navController.navigate(Routes.ACHIEVEMENTS) },
                onOpenPastChapters = { navController.navigate(Routes.PAST_CHAPTERS) },
                onOpenReflection = { navController.navigate(Routes.reflection()) },
                onOpenReflectionHistory = {
                    navController.navigate(Routes.REFLECTION_HISTORY)
                },
                contentPadding = bottomBarPadding,
            )
        }

        composable(route = Routes.CREATE_GOAL, enterTransition = { slideUp() }) {
            CreateGoalScreen(
                onClose = { navController.popBackStack() },
                onCreated = { goalId ->
                    // Replace the wizard so back-press from the new goal returns
                    // to where the user started, not into the form they finished.
                    navController.navigate(Routes.goalDetail(goalId)) {
                        popUpTo(Routes.CREATE_GOAL) { inclusive = true }
                    }
                },
            )
        }

        composable(
            route = Routes.GOAL_DETAIL,
            arguments = listOf(navArgument(Routes.GOAL_ID_ARG) { type = NavType.StringType }),
            enterTransition = { slideFromEnd() },
            exitTransition = { fadeOut(tween(MilestoneMotion.DurationQuick)) },
            popExitTransition = { slideToEnd() },
        ) {
            GoalDetailScreen(onBack = { navController.popBackStack() })
        }

        placeholderRoutes(navController, bottomBarPadding)
    }
}

/**
 * Routes that exist so navigation from the built screens doesn't dead-end.
 *
 * Season, Reflection, Achievements, Past Chapters and Life Area are all linked to
 * from Home and the Goals list. Registering them now keeps those links honest —
 * the alternative is a tap that silently does nothing.
 */
private fun NavGraphBuilder.placeholderRoutes(
    navController: NavHostController,
    bottomBarPadding: PaddingValues,
) {
    composable(route = Routes.SEASON) {
        SeasonScreen(
            onBack = { navController.popBackStack() },
            onOpenGoal = { navController.navigateToGoal(it) },
            contentPadding = bottomBarPadding,
        )
    }

    composable(
        route = Routes.REFLECTION,
        // Optional: the base route still matches, which is how "this week"
        // is expressed.
        arguments = listOf(
            navArgument(Routes.REFLECTION_WEEK_ARG) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            },
        ),
    ) {
        ReflectionScreen(
            onBack = { navController.popBackStack() },
            contentPadding = bottomBarPadding,
        )
    }

    composable(route = Routes.REFLECTION_HISTORY) {
        ReflectionHistoryScreen(
            onBack = { navController.popBackStack() },
            onOpenWeek = { weekStart ->
                navController.navigate(Routes.reflection(weekStart))
            },
            contentPadding = bottomBarPadding,
        )
    }

    composable(route = Routes.ACHIEVEMENTS) {
        AchievementsScreen(
            onBack = { navController.popBackStack() },
            contentPadding = bottomBarPadding,
        )
    }

    composable(route = Routes.PAST_CHAPTERS) {
        PastChaptersScreen(
            onBack = { navController.popBackStack() },
            onOpenGoal = { navController.navigateToGoal(it) },
            contentPadding = bottomBarPadding,
        )
    }

    composable(
        route = Routes.LIFE_AREA_DETAIL,
        arguments = listOf(navArgument(Routes.AREA_ID_ARG) { type = NavType.StringType }),
    ) {
        LifeAreaScreen(
            onBack = { navController.popBackStack() },
            onOpenGoal = { navController.navigateToGoal(it) },
            onCreateGoal = { navController.navigate(Routes.CREATE_GOAL) },
            contentPadding = bottomBarPadding,
        )
    }
}

/** Pushes a goal, avoiding a duplicate entry when it's already on top. */
private fun NavHostController.navigateToGoal(goalId: Long) {
    navigate(Routes.goalDetail(goalId)) { launchSingleTop = true }
}

/**
 * Switches tabs.
 *
 * `popUpTo(startDestination)` with `saveState` is what keeps each tab's scroll
 * position and stops the back stack growing by one entry per tab tap.
 */
fun NavHostController.switchTab(destination: TopLevelDestination) {
    navigate(destination.route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideFromEnd(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Start,
        animationSpec = tween(MilestoneMotion.DurationStandard),
    ) + fadeIn(tween(MilestoneMotion.DurationStandard))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideToEnd(): ExitTransition =
    slideOutOfContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.End,
        animationSpec = tween(MilestoneMotion.DurationStandard),
    ) + fadeOut(tween(MilestoneMotion.DurationQuick))

/** The creation wizard arrives from the bottom, like a sheet. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.slideUp(): EnterTransition =
    slideIntoContainer(
        towards = AnimatedContentTransitionScope.SlideDirection.Up,
        animationSpec = tween(MilestoneMotion.DurationSlow),
    ) + fadeIn(tween(MilestoneMotion.DurationStandard))
