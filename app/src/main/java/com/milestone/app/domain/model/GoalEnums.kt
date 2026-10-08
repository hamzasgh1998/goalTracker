package com.milestone.app.domain.model

/**
 * Where a goal sits in its lifecycle.
 *
 * Note that nothing is ever deleted outright: [Archived] is how a goal leaves the
 * active set, and it stays readable in "Past Chapters" forever.
 */
enum class GoalStatus {
    Active,
    Completed,

    /** Deliberately set down, expected to return. Not a failure state. */
    Paused,

    /** Closed out. Lives in Past Chapters with a [ClosingReason]. */
    Archived,
    ;

    val isOpen: Boolean get() = this == Active || this == Paused
}

/**
 * How much of the user's attention a goal claims *this season*.
 *
 * The whole point of seasons is that [Primary] is scarce — the UI nudges toward at
 * most three, and the "Focus" achievement rewards finishing a season that kept to
 * that. Maintenance goals are things being kept alive, not pushed forward.
 */
enum class GoalPriority(val label: String) {
    Primary("Primary"),
    Secondary("Secondary"),
    Maintenance("Maintenance"),
}

/**
 * Why a chapter was closed.
 *
 * Captured because the reason is the interesting part when looking back — "wrong
 * timing" six months ago is an invitation to restart, "no longer important" is not.
 * The wording is deliberately neutral; none of these are confessions.
 */
enum class ClosingReason(val label: String) {
    NoLongerImportant("No longer important"),
    WrongTiming("Wrong timing"),
    TooDifficult("Too difficult"),
    LostInterest("Lost interest"),
    GoalChanged("The goal changed"),
    Other("Something else"),
}
