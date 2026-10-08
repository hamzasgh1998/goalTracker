package com.milestone.app.domain.achievement

/**
 * One earnable achievement.
 *
 * @param key stable id persisted in `achievement_unlock`. Never change a key; it
 *   is the only link between a user's earned unlock and this definition.
 */
data class AchievementDefinition(
    val key: String,
    val glyph: String,
    val title: String,
    /** What earns it, phrased in the past tense once unlocked. */
    val description: String,
    /** Shown while still locked. Describes the act, not the reward. */
    val hint: String,
)

/**
 * The full set of achievements.
 *
 * Each one marks a *decision* rather than a volume of activity — starting, closing
 * honestly, coming back, keeping a season narrow. Nothing here rewards opening the
 * app, logging a streak, or collecting points, because those pull toward busywork
 * instead of the few things that actually matter.
 */
object AchievementCatalog {

    const val FIRST_STEP = "first_step"
    const val NEW_CHAPTER = "new_chapter"
    const val EXPLORER = "explorer"
    const val MOMENTUM = "momentum"
    const val FINISHER = "finisher"
    const val COMEBACK = "comeback"
    const val FOCUS = "focus"
    const val HONEST_CLOSE = "honest_close"
    const val WITNESS = "witness"

    val all: List<AchievementDefinition> = listOf(
        AchievementDefinition(
            key = FIRST_STEP,
            glyph = "🌱",
            title = "First Step",
            description = "Created your first goal.",
            hint = "Create a goal.",
        ),
        AchievementDefinition(
            key = NEW_CHAPTER,
            glyph = "🌅",
            title = "New Chapter",
            description = "Started a life season.",
            hint = "Begin a season.",
        ),
        AchievementDefinition(
            key = EXPLORER,
            glyph = "🧭",
            title = "Explorer",
            description = "Set goals in five different areas of your life.",
            hint = "Set goals in five life areas.",
        ),
        AchievementDefinition(
            key = MOMENTUM,
            glyph = "◆",
            title = "Momentum",
            description = "Completed ten milestones.",
            hint = "Complete ten milestones.",
        ),
        AchievementDefinition(
            key = FINISHER,
            glyph = "🏆",
            title = "Finisher",
            description = "Carried a goal all the way to done.",
            hint = "Finish a goal.",
        ),
        AchievementDefinition(
            key = COMEBACK,
            glyph = "🔥",
            title = "Comeback",
            description = "Picked up a goal you had set down.",
            hint = "Resume a paused goal.",
        ),
        AchievementDefinition(
            key = FOCUS,
            glyph = "🎯",
            title = "Focus",
            description = "Kept a season to three primary goals.",
            hint = "Run a season with no more than three primary goals.",
        ),
        AchievementDefinition(
            key = HONEST_CLOSE,
            glyph = "📕",
            title = "Honest Close",
            description = "Closed a chapter and said why.",
            hint = "Close a goal you are no longer pursuing.",
        ),
        AchievementDefinition(
            key = WITNESS,
            glyph = "✍",
            title = "Witness",
            description = "Wrote four weekly reflections.",
            hint = "Reflect on four weeks.",
        ),
    )

    private val byKey: Map<String, AchievementDefinition> = all.associateBy { it.key }

    fun find(key: String): AchievementDefinition? = byKey[key]
}
