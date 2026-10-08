package com.debubble.app.engine

import kotlinx.serialization.Serializable

/**
 * An if-then plan bound to a specific challenge.
 *
 * Gollwitzer's finding is one of the most replicated in behavioural science: a goal intention
 * ("I intend to talk to more people") predicts action badly, while an implementation intention
 * ("when I am waiting for coffee tomorrow, I will say something to the person in front of me")
 * roughly doubles execution. The mechanism is not motivation — it is that binding a concrete
 * cue to a concrete response creates a single memory representation, so the behaviour fires on
 * the cue without needing a fresh decision at the worst possible moment.
 *
 * That last part is why this matters here specifically. The users this app is for are not short
 * of intent. They are short of the capacity to decide, in the doorway, while frightened. The
 * plan is what removes the decision from the doorway.
 */
@Serializable
data class Intention(
    val id: Long,
    /** Which challenge this plans. Matches the id the router or ladder served. */
    val challengeId: String,
    val challengeTitle: String,
    /** "When I am waiting for coffee tomorrow morning" */
    val cue: String,
    /** "I will say something about the queue to whoever is in front of me" */
    val response: String,
    val createdDay: Long,
    /** The plan expires: a cue that has passed is no longer a plan, it is a reproach. */
    val dueDay: Long,
    val resolved: Boolean = false
) {
    val sentence: String get() = "When $cue, I will $response."

    fun isLive(today: Long): Boolean = !resolved && today <= dueDay

    fun isStale(today: Long): Boolean = !resolved && today > dueDay
}

/**
 * What the user expected, and what actually happened.
 *
 * This is the measuring instrument for the only claim the app makes. Craske's inhibitory
 * learning model holds that exposure does not erase a fear memory — it builds a competing
 * non-threat memory, and the strength of that new learning tracks **expectancy violation**:
 * the gap between the predicted catastrophe and the actual outcome. Habituation (distress
 * falling because you are tired) teaches far less than being specifically, concretely wrong
 * about what would happen.
 *
 * So the app records the prediction *before*, in the user's own words and on a number, and
 * makes them read it back afterwards. The gap is the product working. A user who cannot see
 * that gap has only their memory of the fear, which is the thing that was never accurate.
 */
@Serializable
data class Forecast(
    val id: Long,
    val challengeId: String,
    val challengeTitle: String,
    /** "They will look at me like I am strange and I will have to leave." */
    val predicted: String,
    /** 0 = nothing, 10 = the worst you have felt. */
    val predictedDistress: Int,
    val createdDay: Long,
    /* ---- filled in afterwards ---- */
    val actual: String = "",
    val actualDistress: Int = -1,
    val resolvedDay: Long = -1L
) {
    val isResolved: Boolean get() = actualDistress >= 0

    /**
     * How wrong the prediction was, in the useful direction. Positive means it went better
     * than feared, which is the number this whole app exists to produce.
     */
    val violation: Int get() = if (isResolved) predictedDistress - actualDistress else 0
}

/**
 * Scaffolding for the "when" half of a plan.
 *
 * Specificity is the entire active ingredient, and it is also the thing people skip. "When I
 * get a chance" is a goal intention wearing an if-then costume and performs like one. So the
 * cue is assembled from a fixed anchor plus the user's own detail rather than typed free-form
 * into an empty box, which reliably produces vagueness.
 */
object Cues {

    /** How long a plan stays live before it stops being a plan. */
    const val WINDOW_DAYS = 2L

    /**
     * Anchors are existing, reliable events in an ordinary day. Attaching a new behaviour to
     * one that already happens is the cheapest possible cue — the alternative is remembering
     * to remember.
     */
    val anchors = listOf(
        "I leave the house tomorrow morning",
        "I am next waiting in a queue",
        "I get to work or college tomorrow",
        "I am next in a shop",
        "I finish eating this evening",
        "I next see someone I recognise",
        "I get home today",
        "my alarm goes off tomorrow"
    )

    /** A cue has to name a moment. These are the ways people avoid naming one. */
    private val VAGUE = listOf(
        "when i can", "when i get a chance", "sometime", "soon", "later",
        "if i feel", "when i feel", "eventually", "at some point", "when ready"
    )

    fun isSpecific(cue: String): Boolean {
        val c = cue.trim().lowercase()
        if (c.length < 8) return false
        return VAGUE.none { c.contains(it) }
    }

    fun critique(cue: String): String? = when {
        cue.isBlank() -> "Name the moment this will happen."
        cue.trim().length < 8 -> "Too short to be a moment. When exactly?"
        !isSpecific(cue) -> "That is a feeling, not a moment. Pick something that will happen anyway."
        else -> null
    }

    /** The response half has the same failure mode: a direction rather than an action. */
    fun critiqueResponse(response: String): String? = when {
        response.isBlank() -> "What will you actually do?"
        response.trim().length < 6 -> "Say what the action is, in a few words."
        else -> null
    }
}

object Forecasts {

    /** Distress runs 0..10. Clinical SUDS is 0..100; nobody has that resolution about a feeling. */
    const val SCALE = 10

    /** Below this, the prediction was close enough that nothing was disconfirmed. */
    const val MEANINGFUL_VIOLATION = 2

    /** At or above this, the moment was genuinely frightening rather than merely mild. */
    const val REALLY_FEARED = 5

    fun label(distress: Int): String = when {
        distress <= 0 -> "Nothing at all"
        distress <= 2 -> "Barely noticeable"
        distress <= 4 -> "Uncomfortable"
        distress <= 6 -> "Difficult"
        distress <= 8 -> "Very hard"
        else -> "The worst I get"
    }

    fun resolved(all: List<Forecast>): List<Forecast> = all.filter { it.isResolved }

    /** The headline: on average, how much better than feared things go. */
    fun meanViolation(all: List<Forecast>): Float {
        val done = resolved(all)
        if (done.isEmpty()) return 0f
        return done.sumOf { it.violation }.toFloat() / done.size
    }

    /** How often a prediction was meaningfully wrong in the good direction. */
    fun overestimateRate(all: List<Forecast>): Float {
        val done = resolved(all)
        if (done.isEmpty()) return 0f
        return done.count { it.violation >= MEANINGFUL_VIOLATION }.toFloat() / done.size
    }

    /**
     * Craske's repeat rule.
     *
     * A frightening thing that went exactly as badly as predicted produced no expectancy
     * violation, so it built no new learning — the step wants repeating rather than
     * advancing past. A *mild* thing with an accurate prediction is simply a mild thing, and
     * carries no such signal, which is why the fear threshold is part of the test.
     */
    fun wantsRepeat(f: Forecast): Boolean =
        f.isResolved &&
            f.violation < MEANINGFUL_VIOLATION &&
            f.actualDistress >= REALLY_FEARED

    /**
     * Whether predictions are getting better calibrated, comparing the oldest and newest
     * halves of the record.
     *
     * Falling absolute error is the real end state: not that things stop being frightening,
     * but that the forecast stops being catastrophic. Needs a reasonable sample before it
     * says anything, because two data points are a mood rather than a trend.
     */
    fun calibrationShift(all: List<Forecast>): Float? {
        val done = resolved(all).sortedBy { it.createdDay }
        if (done.size < 6) return null
        val half = done.size / 2
        val early = done.take(half).map { kotlin.math.abs(it.violation) }.average()
        val late = done.takeLast(half).map { kotlin.math.abs(it.violation) }.average()
        return (early - late).toFloat()
    }

    /** One plain sentence about the record so far, for the profile. */
    fun summary(all: List<Forecast>): String {
        val done = resolved(all)
        if (done.isEmpty()) return "Predict how one will go, then check it afterwards."
        if (done.size < 3) {
            return "${done.size} checked so far. Keep going — the pattern needs a few more."
        }
        val mean = meanViolation(all)
        val rate = (overestimateRate(all) * 100).toInt()
        return when {
            mean >= 2f ->
                "You have overestimated $rate% of these. On average they go " +
                    "${"%.1f".format(mean)} points easier than you expect."
            mean >= 0.5f ->
                "Slightly worse in your head than in reality, most times. " +
                    "$rate% came in easier than predicted."
            mean > -0.5f ->
                "Your predictions are close to accurate now. That is the end state — " +
                    "not fearlessness, just a forecast you can trust."
            else ->
                "These are landing harder than you expect. That almost always means the " +
                    "steps are too big. Pick smaller ones for a week."
        }
    }
}
