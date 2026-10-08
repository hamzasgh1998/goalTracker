package com.milestone.app.ui.navigation

import androidx.compose.ui.graphics.vector.ImageVector
import com.milestone.app.ui.icons.MilestoneIcons
import java.time.LocalDate

/**
 * Route constants.
 *
 * Plain string routes rather than type-safe `@Serializable` destinations, which
 * would pull in the kotlinx-serialization plugin for the sake of two integer
 * arguments. If the argument surface grows, that trade flips.
 */
object Routes {
    const val ONBOARDING = "onboarding"

    const val HOME = "home"
    const val MAP = "map"
    const val GOALS = "goals"
    const val JOURNEY = "journey"
    const val PROFILE = "profile"

    const val CREATE_GOAL = "createGoal"
    const val SEASON = "season"
    const val ACHIEVEMENTS = "achievements"
    const val PAST_CHAPTERS = "pastChapters"

    /**
     * The weekly reflection, for the current week by default.
     *
     * The week is an optional query argument rather than a second destination, so
     * that re-reading a past entry from the history uses the same screen the user
     * already knows — including its text fields, which makes an old week editable
     * instead of read-only.
     */
    private const val REFLECTION_BASE = "reflection"
    const val REFLECTION_WEEK_ARG = "week"
    const val REFLECTION = "$REFLECTION_BASE?$REFLECTION_WEEK_ARG={$REFLECTION_WEEK_ARG}"

    /** Pass null for "this week"; [weekStart] is an ISO date. */
    fun reflection(weekStart: LocalDate? = null): String =
        if (weekStart == null) REFLECTION_BASE else "$REFLECTION_BASE?$REFLECTION_WEEK_ARG=$weekStart"

    const val REFLECTION_HISTORY = "reflections"

    private const val GOAL_DETAIL_BASE = "goal"
    const val GOAL_ID_ARG = "goalId"
    const val GOAL_DETAIL = "$GOAL_DETAIL_BASE/{$GOAL_ID_ARG}"

    fun goalDetail(goalId: Long): String = "$GOAL_DETAIL_BASE/$goalId"

    private const val LIFE_AREA_BASE = "lifeArea"
    const val AREA_ID_ARG = "areaId"
    const val LIFE_AREA_DETAIL = "$LIFE_AREA_BASE/{$AREA_ID_ARG}"

    fun lifeArea(areaId: Long): String = "$LIFE_AREA_BASE/$areaId"
}

/**
 * The five tabs.
 *
 * Five is the practical ceiling for a bottom bar; anything the user needs beyond
 * these (seasons, reflections, achievements, past chapters) is reached from within
 * a tab rather than by adding a sixth.
 */
enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    Home(Routes.HOME, "Home", MilestoneIcons.Home),
    Map(Routes.MAP, "Map", MilestoneIcons.Map),
    Goals(Routes.GOALS, "Goals", MilestoneIcons.Goals),
    Journey(Routes.JOURNEY, "Journey", MilestoneIcons.Journey),
    Profile(Routes.PROFILE, "You", MilestoneIcons.Profile),
    ;

    companion object {
        val routes: Set<String> = entries.map { it.route }.toSet()

        fun fromRoute(route: String?): TopLevelDestination? =
            entries.firstOrNull { it.route == route }
    }
}
