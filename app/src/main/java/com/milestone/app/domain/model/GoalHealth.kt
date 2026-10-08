package com.milestone.app.domain.model

/**
 * A goal's momentum, computed locally from timestamps — no model, no network.
 *
 * This is a *descriptive* signal, never a judgement. The copy attached to each
 * state states a fact ("no progress in 16 days") and the UI always pairs it with
 * an action the user can take, including pausing. Nothing in the app tells the
 * user they have failed.
 */
enum class GoalHealth {
    /** Recent progress, or a comfortable runway to the target date. */
    Healthy,

    /** Quiet for a while, or the target date is close relative to what's left. */
    AtRisk,

    /** No progress for a long stretch. */
    Stalled,

    /** Paused on purpose. Carries no urgency at all. */
    Paused,
    ;

    val isPaused: Boolean get() = this == Paused
}
