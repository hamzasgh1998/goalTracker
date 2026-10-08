package com.milestone.app.domain

/** A worked example the user can adopt wholesale or cherry-pick from. */
data class MilestoneTemplate(
    val label: String,
    val glyph: String,
    val milestones: List<String>,
    /** Lower-case words that make this template relevant to a goal title. */
    val keywords: Set<String>,
)

/**
 * Local milestone suggestions, matched on words in the goal's title.
 *
 * This is the deliberate, offline answer to "help me break this down". It is
 * keyword matching over a hand-written catalogue — no model, no network, nothing
 * to pay for — and it is honest about being examples rather than advice: the UI
 * offers them as a starting point and the user edits freely.
 *
 * Matching is intentionally shallow. A wrong suggestion costs a glance, while
 * anything cleverer would need either a model or a grammar nobody will maintain.
 */
object MilestoneTemplates {

    private val catalog = listOf(
        MilestoneTemplate(
            label = "Running a distance",
            glyph = "🏃",
            keywords = setOf("run", "running", "10k", "5k", "marathon", "jog", "race"),
            milestones = listOf(
                "Buy proper shoes",
                "Run 2 km without stopping",
                "Run 5 km",
                "Run 7 km",
                "Run the full distance",
            ),
        ),
        MilestoneTemplate(
            label = "Learning a language",
            glyph = "🗣",
            keywords = setOf(
                "language", "spanish", "french", "german", "italian", "japanese",
                "speak", "fluent", "portuguese", "arabic", "mandarin",
            ),
            milestones = listOf(
                "Learn 100 everyday words",
                "Get through the present tense",
                "Watch something with subtitles",
                "Have one five-minute conversation",
                "Hold a conversation without switching to English",
            ),
        ),
        MilestoneTemplate(
            label = "Learning an instrument",
            glyph = "🎸",
            keywords = setOf("guitar", "piano", "instrument", "drums", "violin", "play"),
            milestones = listOf(
                "Learn the basic chords",
                "Play one song start to finish",
                "Practise twenty hours",
                "Record myself playing",
                "Play for someone else",
            ),
        ),
        MilestoneTemplate(
            label = "Shipping a project",
            glyph = "🚀",
            keywords = setOf(
                "launch", "build", "ship", "project", "app", "startup", "website",
                "business", "product",
            ),
            milestones = listOf(
                "Decide exactly what it is",
                "Sketch the core of it",
                "Build the main flow",
                "Show it to five people",
                "Fix what they struggled with",
                "Ship it publicly",
            ),
        ),
        MilestoneTemplate(
            label = "Reading more",
            glyph = "📚",
            keywords = setOf("read", "reading", "book", "books"),
            milestones = listOf(
                "Choose the first book",
                "Read twenty minutes a day for a week",
                "Finish book one",
                "Finish book two",
                "Finish book three",
            ),
        ),
        MilestoneTemplate(
            label = "Getting finances in order",
            glyph = "💰",
            keywords = setOf(
                "save", "saving", "savings", "money", "budget", "debt", "finance",
                "invest",
            ),
            milestones = listOf(
                "List everything going out each month",
                "Cancel what I don't use",
                "Set up an automatic transfer",
                "Build one month of buffer",
                "Build three months of buffer",
            ),
        ),
        MilestoneTemplate(
            label = "Getting stronger",
            glyph = "💪",
            keywords = setOf(
                "gym", "strong", "strength", "lift", "weight", "fit", "fitness",
                "muscle", "train",
            ),
            milestones = listOf(
                "Pick a routine I'll actually follow",
                "Train twice a week for a month",
                "Train three times a week for a month",
                "Add 10 kg to my main lift",
                "Keep it up for three months",
            ),
        ),
        MilestoneTemplate(
            label = "Making something creative",
            glyph = "🎨",
            keywords = setOf(
                "write", "writing", "paint", "draw", "photo", "photography", "film",
                "music", "create", "novel", "blog",
            ),
            milestones = listOf(
                "Set aside a regular time for it",
                "Finish one small piece",
                "Finish five pieces",
                "Show the work to someone",
                "Put it somewhere public",
            ),
        ),
    )

    /**
     * A generic skeleton, offered when nothing matches.
     *
     * Structural rather than topical: first step, visible progress, halfway, a
     * finish line. It works for anything, which is exactly why it is the fallback
     * and not a suggestion in its own right.
     */
    val generic = MilestoneTemplate(
        label = "A simple shape",
        glyph = "◆",
        keywords = emptySet(),
        milestones = listOf(
            "Take the first small step",
            "Do it once properly",
            "Get halfway",
            "Push through the boring middle",
            "Finish it",
        ),
    )

    /**
     * Templates whose keywords appear in [goalTitle], best match first.
     *
     * Returns [generic] alone when nothing matches, so the caller always has
     * something to offer and never has to special-case an empty list.
     */
    fun suggestFor(goalTitle: String, limit: Int = 2): List<MilestoneTemplate> {
        val words = goalTitle.lowercase()
            .split(NON_WORD)
            .filter { it.isNotBlank() }
            .toSet()
        if (words.isEmpty()) return listOf(generic)

        val matches = catalog
            .map { template -> template to template.keywords.count { it in words } }
            .filter { (_, score) -> score > 0 }
            .sortedByDescending { (_, score) -> score }
            .map { (template, _) -> template }
            .take(limit)

        return matches.ifEmpty { listOf(generic) }
    }

    private val NON_WORD = Regex("[^\\p{L}\\p{N}]+")
}
