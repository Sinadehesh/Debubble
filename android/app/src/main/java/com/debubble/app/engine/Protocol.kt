package com.debubble.app.engine

/**
 * The scaffolding that turns a directive into something a frightened person can actually start.
 *
 * The first version of this curriculum was 300 well-written instructions and nothing else, and
 * that is a known failure mode rather than a stylistic one. Four separate literatures say the
 * same thing about why a clear instruction does not become a behaviour:
 *
 *  1. **Implementation intentions.** Gollwitzer's "if situation Y, then I will do Z" roughly
 *     doubles execution over an equally sincere goal intention, and the active ingredient is
 *     that the cue names a *moment*. "Walk to the end of your street" names no moment, so it
 *     competes with everything else in the day and loses. Every challenge now carries an
 *     [Challenge.anchor]: a precise instant that is going to happen anyway.
 *
 *  2. **Tiny Habits / Atomic Habits.** Fogg's recipe is anchor → tiny behaviour → celebration,
 *     and he is explicit that anchor-plus-behaviour on its own is only a routine: the
 *     celebration is what encodes it. Clear's two-minute rule is the same instinct from the
 *     other end — make the entry so small that starting cannot be refused. So each challenge
 *     carries an [Challenge.opener] that takes under two minutes and is not the whole task,
 *     and the app celebrates at the moment of completion rather than silently incrementing a
 *     counter. See [celebration].
 *
 *  3. **Inhibitory learning (Craske).** Exposure does not wear a fear down; it builds a
 *     competing memory, and the strength of that learning tracks *expectancy violation* —
 *     being concretely wrong about what would happen. Two things follow. The user has to state
 *     a prediction first ([Challenge.test], which pre-fills the plan screen), and the safety
 *     behaviour has to come off ([Challenge.drop]), because a feared thing survived while
 *     clutching a safety signal teaches that the safety signal was load-bearing. A 2025
 *     randomised trial found the inhibitory-learning protocol beat straight habituation on
 *     every measure with roughly *half* the exposure time, which is the single strongest
 *     reason this app stopped measuring challenges in minutes endured.
 *
 *  4. **Behavioural activation.** The graded task assignment is the part that keeps people in:
 *     when a rung does not fit, the answer is a smaller rung, offered in the same breath, not
 *     a motivational sentence. [Served.ease] is literally the ladder two rungs down — already
 *     authored, already calibrated, and therefore impossible to get accidentally wrong.
 *
 * There is also a defect the literature does not fix, which the old curriculum had badly:
 * "Ask one stranger a question" tells someone who cannot ask strangers questions nothing they
 * did not know. Where the barrier is a missing sentence rather than a missing nerve, the
 * challenge now ships the sentence ([Challenge.script]).
 *
 * One honest caveat, because the app makes claims at the user: the task-bracketing result
 * often cited for "start and finish cues matter" (Martiros, Burgess & Graybiel, *Current
 * Biology* 2018 — striatal neurons fire at the onset and completion of a learned routine and
 * fall quiet in the middle) is rodent work. It is a reason to mark the end of a task
 * deliberately rather than let it trail off. It is not evidence about human habit formation,
 * and nothing in the app tells the user it is.
 */
object Protocol {

    /** Longest an [Challenge.opener] is allowed to be, in minutes. Clear's two-minute rule. */
    const val OPENER_MINUTES = 2

    /**
     * A celebration, shown the instant a challenge is committed.
     *
     * Fogg's claim is narrow and worth stating precisely: the celebration has to produce real
     * positive feeling, and it has to land immediately — seconds, not at the end of the week.
     * That is why these are physical or spoken acts rather than badges. A badge is a record of
     * the behaviour; "say it out loud" is the behaviour's reward.
     *
     * Deterministic on tier so the same rung always gets the same line, which stops it reading
     * as a slot machine. Variety across tiers, not within one.
     */
    fun celebration(pillar: Pillar, tier: Int): String {
        val pool = when (pillar) {
            Pillar.ACCESS -> accessCelebrations
            Pillar.ACTIVITY -> activityCelebrations
            Pillar.SOCIAL -> socialCelebrations
        }
        return pool[(tier.coerceAtLeast(1) - 1) % pool.size]
    }

    private val accessCelebrations = listOf(
        "Out loud, before you do anything else: \"I went.\"",
        "Stop for three seconds and notice you are somewhere you had not been.",
        "Say \"that is further than yesterday\" out loud. It is.",
        "Look back the way you came. Say \"I did that one.\"",
        "One breath out, and say it: \"the door opens.\""
    )

    private val activityCelebrations = listOf(
        "Say it out loud: \"I did the thing I said I would do.\"",
        "Square your shoulders for three seconds. That was the hard part.",
        "Out loud: \"badly done is done.\"",
        "One fist, once, and mean it.",
        "Say \"I am the kind of person who finishes this\" before the feeling fades."
    )

    private val socialCelebrations = listOf(
        "Say it under your breath: \"I spoke first.\"",
        "Breathe out and say \"good\". That is the whole ritual.",
        "Out loud: \"nothing happened to me.\"",
        "Three seconds of stillness, and \"I said it.\"",
        "Say \"that was survivable\" before you start explaining it away."
    )

    /**
     * When nothing was authored — campaign missions and audit remediations run through the
     * same screen — this is the fallback cue. Still a moment rather than an intention, and
     * still passes the app's own specificity test, which [Cues.isSpecific] enforces.
     */
    fun genericAnchor(pillar: Pillar): String = when (pillar) {
        Pillar.ACCESS -> "I next put my shoes on to leave the house"
        Pillar.ACTIVITY -> "I next sit down with nothing scheduled"
        Pillar.SOCIAL -> "I am next in the same room as someone I could speak to"
    }

    /**
     * How a prediction is phrased back to the user on the plan screen.
     *
     * Deliberately the user's *own* catastrophe rather than a neutral forecast. A prediction
     * with no teeth cannot be violated, and an unviolated prediction teaches nothing.
     */
    fun predictionPrompt(test: String): String =
        if (test.isBlank()) "What is the worst you think will happen?" else test
}
