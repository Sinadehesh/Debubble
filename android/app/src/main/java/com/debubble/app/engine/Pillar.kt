package com.debubble.app.engine

/**
 * The three dimensions of the bubble. Each has its own 100-tier ladder, its own accent,
 * and its own unit of progress — progress is always reported as a physical measurement,
 * never as points.
 */
enum class Pillar(
    val display: String,
    val dimension: String,
    val code: String,
    val asset: String,
    val evidenceNoun: String
) {
    ACCESS(
        display = "Access",
        dimension = "Geographic",
        code = "A",
        asset = "curriculum/access.json",
        evidenceNoun = "places you had never been"
    ),
    ACTIVITY(
        display = "Activity",
        dimension = "Experiential",
        code = "B",
        asset = "curriculum/activity.json",
        evidenceNoun = "things you had never done"
    ),
    SOCIAL(
        display = "Social",
        dimension = "Interpersonal",
        code = "C",
        asset = "curriculum/social.json",
        evidenceNoun = "people you reached out to"
    );

    companion object {
        /** Fixed serve order, so muscle memory forms. */
        val order = listOf(ACCESS, ACTIVITY, SOCIAL)
    }
}

/** Capabilities and constraints a challenge can require. */
object Needs {
    const val TRANSIT = "transit"
    const val BIKE = "bike"
    const val CAR = "car"
    const val KITCHEN = "kitchen"
    const val MONEY = "money"
    const val OVERNIGHT = "overnight"
    const val MULTIDAY = "multiday"
    const val PASSPORT = "passport"

    /** Needs the user answers for directly during calibration. */
    val transportModes = listOf(TRANSIT, BIKE, CAR)
}
