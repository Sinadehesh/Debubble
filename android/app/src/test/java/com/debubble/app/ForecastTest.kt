package com.debubble.app

import com.debubble.app.engine.Cues
import com.debubble.app.engine.Forecast
import com.debubble.app.engine.Forecasts
import com.debubble.app.engine.Intention
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Plans and predictions.
 *
 * The forecast record is the only thing in this app that measures its actual claim — that the
 * limitation was a bad forecast rather than the world — so the arithmetic behind it has to be
 * right in both directions. A bug that flattered the user here would be worse than useless: it
 * would manufacture evidence of progress that did not happen.
 */
class ForecastTest {

    private fun forecast(
        id: Long = 1,
        predicted: Int,
        actual: Int = -1,
        day: Long = 1
    ) = Forecast(
        id = id,
        challengeId = "c$id",
        challengeTitle = "Say something to the person in the queue",
        predicted = "It will be awful",
        predictedDistress = predicted,
        createdDay = day,
        actual = if (actual >= 0) "It was fine" else "",
        actualDistress = actual,
        resolvedDay = if (actual >= 0) day + 1 else -1
    )

    // ---------------------------------------------------------------- the core number

    @Test
    fun `violation is the gap, positive when it went better than feared`() {
        assertEquals(6, forecast(predicted = 8, actual = 2).violation)
        assertEquals(0, forecast(predicted = 5, actual = 5).violation)
        assertEquals(-3, forecast(predicted = 2, actual = 5).violation)
    }

    @Test
    fun `an unresolved forecast has no violation and does not count`() {
        val open = forecast(predicted = 9)
        assertFalse(open.isResolved)
        assertEquals(0, open.violation)
        assertEquals(0f, Forecasts.meanViolation(listOf(open)), 1e-6f)
        assertTrue(Forecasts.resolved(listOf(open)).isEmpty())
    }

    @Test
    fun `means and rates ignore the unresolved ones entirely`() {
        val list = listOf(
            forecast(1, predicted = 8, actual = 2),   // +6
            forecast(2, predicted = 6, actual = 4),   // +2
            forecast(3, predicted = 9)                // open
        )
        assertEquals(2, Forecasts.resolved(list).size)
        assertEquals(4f, Forecasts.meanViolation(list), 1e-6f)
        assertEquals(1f, Forecasts.overestimateRate(list), 1e-6f)
    }

    @Test
    fun `an empty record reports zero rather than dividing by nothing`() {
        assertEquals(0f, Forecasts.meanViolation(emptyList()), 1e-6f)
        assertEquals(0f, Forecasts.overestimateRate(emptyList()), 1e-6f)
        assertNull(Forecasts.calibrationShift(emptyList()))
        assertTrue(Forecasts.summary(emptyList()).isNotBlank())
    }

    @Test
    fun `overestimate rate only counts meaningful gaps, not noise`() {
        val list = listOf(
            forecast(1, predicted = 8, actual = 2),  // +6, counts
            forecast(2, predicted = 5, actual = 4),  // +1, below the threshold
            forecast(3, predicted = 7, actual = 5),  // +2, exactly at it
            forecast(4, predicted = 3, actual = 6)   // -3, wrong direction
        )
        assertEquals(0.5f, Forecasts.overestimateRate(list), 1e-6f)
    }

    // ------------------------------------------------------------- the Craske rule

    /**
     * The clinically load-bearing one. A frightening thing that went exactly as badly as
     * predicted disconfirmed nothing, so repeating it is the correct next step rather than
     * advancing. A *mild* thing predicted accurately carries no such signal.
     */
    @Test
    fun `a feared thing that went as badly as predicted wants repeating`() {
        assertTrue(Forecasts.wantsRepeat(forecast(predicted = 8, actual = 7)))
        assertTrue(Forecasts.wantsRepeat(forecast(predicted = 6, actual = 6)))
    }

    @Test
    fun `a mild thing predicted accurately does not want repeating`() {
        // Accurate, but nothing was at stake — there was no fear to disconfirm.
        assertFalse(Forecasts.wantsRepeat(forecast(predicted = 2, actual = 2)))
        assertFalse(Forecasts.wantsRepeat(forecast(predicted = 4, actual = 4)))
    }

    @Test
    fun `a big disconfirmation does not want repeating, however frightening it was`() {
        assertFalse(Forecasts.wantsRepeat(forecast(predicted = 9, actual = 2)))
        assertFalse(Forecasts.wantsRepeat(forecast(predicted = 10, actual = 5)))
    }

    @Test
    fun `an unresolved forecast never wants repeating`() {
        assertFalse(Forecasts.wantsRepeat(forecast(predicted = 9)))
    }

    // -------------------------------------------------------------- calibration drift

    @Test
    fun `calibration stays silent until there is enough to say`() {
        val few = (1..5).map { forecast(it.toLong(), predicted = 8, actual = 2, day = it.toLong()) }
        assertNull("five points is a mood, not a trend", Forecasts.calibrationShift(few))
        assertNotNull(
            Forecasts.calibrationShift(
                few + forecast(6, predicted = 8, actual = 2, day = 6)
            )
        )
    }

    @Test
    fun `shrinking error over time reads as positive`() {
        // Early: wildly catastrophic. Late: close to accurate.
        val early = (1..3).map { forecast(it.toLong(), predicted = 9, actual = 1, day = it.toLong()) }
        val late = (4..6).map { forecast(it.toLong(), predicted = 5, actual = 4, day = it.toLong()) }
        val shift = Forecasts.calibrationShift(early + late)
        assertNotNull(shift)
        assertTrue("expected improvement, got $shift", shift!! > 0f)
    }

    @Test
    fun `growing error over time reads as negative`() {
        val early = (1..3).map { forecast(it.toLong(), predicted = 5, actual = 4, day = it.toLong()) }
        val late = (4..6).map { forecast(it.toLong(), predicted = 9, actual = 1, day = it.toLong()) }
        val shift = Forecasts.calibrationShift(early + late)
        assertNotNull(shift)
        assertTrue("expected deterioration, got $shift", shift!! < 0f)
    }

    /** Order of arrival must not matter; the record is sorted by when it was made. */
    @Test
    fun `calibration reads the record chronologically, not by list order`() {
        val chronological = listOf(
            forecast(1, predicted = 9, actual = 1, day = 1),
            forecast(2, predicted = 9, actual = 1, day = 2),
            forecast(3, predicted = 9, actual = 1, day = 3),
            forecast(4, predicted = 5, actual = 4, day = 4),
            forecast(5, predicted = 5, actual = 4, day = 5),
            forecast(6, predicted = 5, actual = 4, day = 6)
        )
        assertEquals(
            Forecasts.calibrationShift(chronological),
            Forecasts.calibrationShift(chronological.reversed())
        )
    }

    // -------------------------------------------------------------------- the summary

    @Test
    fun `the summary never blames the user for a bad run`() {
        // A record where everything landed harder than expected.
        val rough = (1..6).map { forecast(it.toLong(), predicted = 3, actual = 8, day = it.toLong()) }
        val text = Forecasts.summary(rough).lowercase()
        assertTrue("should name the step size, not the person", text.contains("too big"))
        listOf("fail", "weak", "should have", "not trying").forEach {
            assertFalse("summary must not contain '$it': $text", text.contains(it))
        }
    }

    @Test
    fun `the summary always says something, at every size of record`() {
        (0..10).forEach { n ->
            val list = (1..n).map { forecast(it.toLong(), predicted = 7, actual = 3, day = it.toLong()) }
            assertTrue("empty summary at n=$n", Forecasts.summary(list).isNotBlank())
        }
    }

    @Test
    fun `distress labels cover the whole scale with no gaps`() {
        (0..Forecasts.SCALE).forEach {
            assertTrue("no label for $it", Forecasts.label(it).isNotBlank())
        }
        // Out of range should not crash the profile.
        assertTrue(Forecasts.label(-5).isNotBlank())
        assertTrue(Forecasts.label(99).isNotBlank())
    }
}

/**
 * If-then plans.
 *
 * The specificity check is the entire active ingredient. An implementation intention that
 * names no moment is a goal intention in costume and performs like one, so the screen refuses
 * to accept the vague forms people reach for.
 */
class IntentionTest {

    private fun plan(created: Long = 10, due: Long = 12) = Intention(
        id = 1,
        challengeId = "tier:SOCIAL:4",
        challengeTitle = "Talk to someone in the queue",
        cue = "I am next waiting in a queue",
        response = "say something about the wait to whoever is in front of me",
        createdDay = created,
        dueDay = due
    )

    @Test
    fun `the plan reads as one sentence`() {
        assertEquals(
            "When I am next waiting in a queue, I will say something about the wait " +
                "to whoever is in front of me.",
            plan().sentence
        )
    }

    @Test
    fun `a plan is live up to and including its last day`() {
        val p = plan(created = 10, due = 12)
        assertTrue(p.isLive(10))
        assertTrue(p.isLive(12))
        assertFalse(p.isLive(13))
        assertTrue(p.isStale(13))
        assertFalse(p.isStale(12))
    }

    @Test
    fun `a resolved plan is neither live nor stale`() {
        val done = plan().copy(resolved = true)
        assertFalse(done.isLive(10))
        assertFalse("a finished plan must not resurface as overdue", done.isStale(99))
    }

    @Test
    fun `vague cues are rejected`() {
        listOf(
            "when I can",
            "when I get a chance",
            "sometime this week",
            "when I feel ready",
            "soon",
            "eventually",
            ""
        ).forEach {
            assertFalse("'$it' should not pass as a cue", Cues.isSpecific(it))
            assertNotNull("'$it' should get a critique", Cues.critique(it))
        }
    }

    @Test
    fun `concrete cues are accepted`() {
        listOf(
            "I leave the house tomorrow morning",
            "I am next in a shop",
            "my alarm goes off tomorrow",
            "I get home today, before I sit down"
        ).forEach {
            assertTrue("'$it' should pass as a cue", Cues.isSpecific(it))
            assertNull("'$it' should not be critiqued", Cues.critique(it))
        }
    }

    @Test
    fun `every offered anchor passes the app's own specificity test`() {
        Cues.anchors.forEach {
            assertTrue("the app offers '$it' but would reject it", Cues.isSpecific(it))
            assertNull(Cues.critique(it))
        }
        assertTrue(Cues.anchors.isNotEmpty())
        assertEquals(Cues.anchors.size, Cues.anchors.distinct().size)
    }

    @Test
    fun `the response half is checked too`() {
        assertNotNull(Cues.critiqueResponse(""))
        assertNotNull(Cues.critiqueResponse("ask"))
        assertNull(Cues.critiqueResponse("say hello to whoever is behind the counter"))
    }

    @Test
    fun `the plan window is short enough to stay a plan`() {
        // A cue that has passed is a reproach, not a plan. Days, not weeks.
        assertTrue(Cues.WINDOW_DAYS in 1..3)
    }
}
