package com.milestone.app.domain.model

/**
 * The kinds of thing that show up on the Journey timeline.
 *
 * Every entry is an append-only record of something the user actually did. The
 * timeline exists because people genuinely forget how much ground they covered,
 * so these are written eagerly and never pruned.
 */
enum class ProgressEventType(val glyph: String) {
    GoalCreated("🎯"),
    GoalCompleted("🏆"),
    GoalPaused("⏸"),
    GoalResumed("🔥"),
    GoalArchived("📕"),
    MilestoneCompleted("✓"),
    MilestoneAdded("◆"),
    QuestCompleted("·"),
    SeasonStarted("🌱"),
    SeasonEnded("🌅"),
    ReflectionWritten("✍"),
    AchievementUnlocked("★"),
}
