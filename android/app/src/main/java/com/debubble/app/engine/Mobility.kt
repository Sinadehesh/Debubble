package com.debubble.app.engine

/**
 * How this person actually moves through the world.
 *
 * The old design asked only which *vehicles* someone had and then printed "on foot is
 * assumed — it is always available" underneath. That is false for a large number of people,
 * and the curriculum leans on it hard: forty-three of the hundred Access challenges use the
 * word "walk". A wheelchair user opening this app was told to walk to the end of their
 * street on day two.
 *
 * So walking is now a choice like any other, and what the user picks rewrites the wording of
 * every challenge they are served. Nothing is filtered out and no ladder is made shorter —
 * the same challenge is simply described in words that fit the body doing it.
 */
object Mobility {
    const val WALK = "walk"
    const val WHEELS = "wheels"
    const val BIKE = Needs.BIKE
    const val TRANSIT = Needs.TRANSIT
    const val CAR = Needs.CAR

    /** The five options, in the order they are offered. */
    val all = listOf(WALK, WHEELS, BIKE, TRANSIT, CAR)

    /** Ways of moving under your own power. These decide how directives are worded. */
    val selfPowered = setOf(WALK, WHEELS)

    /** Modes that are also capability gates in the curriculum's `needs` field. */
    val vehicles = setOf(BIKE, TRANSIT, CAR)

    fun label(key: String): String = when (key) {
        WALK -> "Walking"
        WHEELS -> "Wheelchair / mobility aid"
        BIKE -> "Bicycle / scooter"
        TRANSIT -> "Public transit"
        CAR -> "Car / motorbike"
        else -> key
    }

    fun detail(key: String): String = when (key) {
        WALK -> "You can cover short distances on foot"
        WHEELS -> "You use a chair, crutches, a frame or a scooter"
        BIKE -> "You have one, or can hire one nearby"
        TRANSIT -> "Buses, trams or trains you can afford"
        CAR -> "Yours, or one you can borrow"
        else -> ""
    }
}

/**
 * Rewrites a challenge so it describes something the reader can actually do.
 *
 * This runs on the served directive, the alternate and the coach line — every piece of
 * authored text the user reads. It is a wording change only: the tier, the exposure rating
 * and the minutes are untouched, because rolling two kilometres is not an easier challenge
 * than walking two kilometres and the engine must never imply that it is.
 *
 * When walking is selected nothing happens at all, so the overwhelmingly common case costs
 * one set lookup and returns the original string.
 */
object Copy {

    /**
     * Ordered because the specific cases have to fire before the general verb rule —
     * "an hour's walk" is a noun and must not become "an hour's roll".
     */
    private val nounRules = listOf(
        Regex("""\bon foot\b""") to "under your own power",
        Regex("""\bOn foot\b""") to "Under your own power",
        Regex("""\bhour's walk\b""") to "hour's travel",
        // Escaped deliberately: this is a regex group reference in the replacement, not a
        // Kotlin string template. Writing it bare works but reads like a mistake.
        Regex("""\b(\w+)-minute walk\b""") to "\$1-minute trip",
        Regex("""\ba walk\b""") to "a trip",
        Regex("""\bA walk\b""") to "A trip",
        Regex("""\bthe walk\b""") to "the trip",
        Regex("""\bThe walk\b""") to "The trip"
    )

    /**
     * Imperatives that assume standing. "Stand there for sixty seconds" is the very first
     * challenge in the app, so this one matters more than its size suggests.
     */
    private val postureRules = listOf(
        Regex("""\bStand\b""") to "Stay",
        Regex("""\bstand\b""") to "stay"
    )

    fun verb(mobility: Set<String>): String = when {
        Mobility.WALK in mobility -> "Walk"
        Mobility.WHEELS in mobility -> "Roll"
        // Someone who picked only transit or a car still has to close the last hundred
        // metres somehow; "head" makes no claim about how.
        else -> "Head"
    }

    /**
     * True when the served text needs rewriting. Walking selected means the curriculum's own
     * words are already right.
     */
    fun rewrites(mobility: Set<String>): Boolean = Mobility.WALK !in mobility

    fun adapt(text: String, mobility: Set<String>): String {
        if (!rewrites(mobility)) return text
        var out = text
        nounRules.forEach { (re, to) -> out = re.replace(out, to) }
        postureRules.forEach { (re, to) -> out = re.replace(out, to) }

        val v = verb(mobility)
        out = Regex("""\bWalk\b""").replace(out, v)
        out = Regex("""\bwalk\b""").replace(out, v.lowercase())
        out = Regex("""\bwalked\b""").replace(out, past(v))
        out = Regex("""\bwalking\b""").replace(out, progressive(v))
        return out
    }

    // "Head" has no usable past or progressive in these sentences — "a green space you have
    // never headed in" is not English. The neutral forms are, so those are what it gets.
    private fun past(v: String): String = when (v) {
        "Roll" -> "rolled"
        else -> "been"
    }

    private fun progressive(v: String): String = when (v) {
        "Roll" -> "rolling"
        else -> "going"
    }
}
