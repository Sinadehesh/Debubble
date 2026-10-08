package com.debubble.app

import com.debubble.app.data.AppState
import com.debubble.app.engine.Baseline
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Forecast
import com.debubble.app.engine.Intention
import com.debubble.app.engine.Ladder
import com.debubble.app.engine.Mobility
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
import com.debubble.app.widget.WidgetCue
import com.debubble.app.widget.WidgetCues
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * What the home-screen widget says.
 *
 * Worth testing more carefully than most screens, for a reason that has nothing to do with
 * how much code it is: it is the only surface that renders in someone else's process, on
 * someone else's launcher, where a wrong decision shows up as stale or empty text rather than
 * as anything a user would recognise as a bug and report. The decision is pure so that it can
 * be checked here instead of on a device.
 */
class WidgetTest {

    private val json = Json { ignoreUnknownKeys = true }

    private val curriculum: Curriculum by lazy {
        val dir = listOf(
            File("src/main/assets/curriculum"),
            File("app/src/main/assets/curriculum")
        ).first { it.isDirectory }
        Curriculum(
            Pillar.order.associateWith {
                json.decodeFromString<Ladder>(
                    File(dir, it.asset.substringAfterLast('/')).readText()
                )
            }
        )
    }

    private val today = 20_000L

    private fun state(
        done: Set<Pillar> = emptySet(),
        intentions: List<Intention> = emptyList(),
        forecasts: List<Forecast> = emptyList(),
        streak: Int = 0,
        tier: Int = 1
    ) = AppState(
        baseline = Baseline(moves = setOf(Mobility.WALK)),
        pillars = Pillar.order.associate { it.name to PillarState(tier = tier) },
        servedDay = today,
        doneToday = done.map { it.name }.toSet(),
        streak = streak,
        intentions = intentions,
        forecasts = forecasts
    )

    private fun plan(day: Long = today) = Intention(
        id = 1,
        challengeId = "tier:SOCIAL:4",
        challengeTitle = "Hold a door and say something",
        cue = "I next join the back of a queue",
        response = "say something about the wait",
        createdDay = day,
        dueDay = day + 2
    )

    private fun forecast(day: Long, resolved: Boolean = false) = Forecast(
        id = 1,
        challengeId = "tier:SOCIAL:4",
        challengeTitle = "Hold a door and say something",
        predicted = "It will be awful",
        predictedDistress = 8,
        createdDay = day,
        actualDistress = if (resolved) 2 else -1
    )

    // ------------------------------------------------------------------ what it leads with

    /**
     * The branch order is the design. A live plan outranks a fresh challenge because the plan
     * is the only thing on the widget whose value depends on being seen at a particular
     * moment — a challenge can be read whenever the app is next opened, a cue cannot.
     */
    @Test
    fun `a live plan is what the widget leads with`() {
        val cue = WidgetCues.of(state(intentions = listOf(plan())), today, curriculum)
        assertEquals(WidgetCue.Tone.PLAN, cue.tone)
        assertEquals("When I next join the back of a queue, I will say something about the wait.", cue.cue)
        assertEquals("tier:SOCIAL:4", cue.link)
    }

    @Test
    fun `an expired plan does not keep showing`() {
        // Due two days ago: a cue that has passed is a reproach, not a plan.
        val stale = plan(day = today - 5)
        val cue = WidgetCues.of(state(intentions = listOf(stale)), today, curriculum)
        assertFalse("an overdue plan must not be presented as live", cue.tone == WidgetCue.Tone.PLAN)
    }

    @Test
    fun `a resolved plan does not keep showing`() {
        val done = plan().copy(resolved = true)
        val cue = WidgetCues.of(state(intentions = listOf(done)), today, curriculum)
        assertFalse(cue.tone == WidgetCue.Tone.PLAN)
    }

    // ------------------------------------------------------------------ the fallback

    @Test
    fun `with no plan it shows the first pillar still open, as a moment`() {
        val cue = WidgetCues.of(state(), today, curriculum)
        assertEquals(WidgetCue.Tone.ACCESS, cue.tone)
        assertTrue("the cue must be a moment: ${cue.cue}", cue.cue.startsWith("When "))
        assertEquals("tier:ACCESS:1", cue.link)
        assertTrue(cue.body.isNotBlank())
    }

    @Test
    fun `it moves on as pillars are finished`() {
        assertEquals(
            WidgetCue.Tone.ACTIVITY,
            WidgetCues.of(state(done = setOf(Pillar.ACCESS)), today, curriculum).tone
        )
        assertEquals(
            WidgetCue.Tone.SOCIAL,
            WidgetCues.of(
                state(done = setOf(Pillar.ACCESS, Pillar.ACTIVITY)), today, curriculum
            ).tone
        )
    }

    @Test
    fun `all three done says so rather than offering a fourth thing`() {
        val cue = WidgetCues.of(state(done = Pillar.order.toSet()), today, curriculum)
        assertEquals(WidgetCue.Tone.DONE, cue.tone)
        assertTrue(cue.link.isEmpty())
        assertTrue(cue.body.contains("tomorrow"))
    }

    /** A new day clears yesterday's completions, so the widget must not still say "done". */
    @Test
    fun `yesterday's completions do not carry over`() {
        val yesterday = state(done = Pillar.order.toSet()).copy(servedDay = today - 1)
        val cue = WidgetCues.of(yesterday, today, curriculum)
        assertEquals(WidgetCue.Tone.ACCESS, cue.tone)
    }

    // ------------------------------------------------------------------ the loose end

    @Test
    fun `an unchecked prediction from a previous day raises the second action`() {
        val cue = WidgetCues.of(state(forecasts = listOf(forecast(today - 1))), today, curriculum)
        assertTrue(cue.review)
    }

    @Test
    fun `a prediction made today is not nagged about yet`() {
        val cue = WidgetCues.of(state(forecasts = listOf(forecast(today))), today, curriculum)
        assertFalse("the challenge has not happened yet", cue.review)
    }

    @Test
    fun `a checked prediction is not nagged about at all`() {
        val cue = WidgetCues.of(
            state(forecasts = listOf(forecast(today - 3, resolved = true))), today, curriculum
        )
        assertFalse(cue.review)
    }

    // ------------------------------------------------------------------ the small print

    @Test
    fun `a one-day streak says nothing rather than claiming a streak`() {
        assertEquals("", WidgetCues.of(state(streak = 1), today, curriculum).badge)
        assertEquals("", WidgetCues.of(state(streak = 0), today, curriculum).badge)
        assertEquals("4 days", WidgetCues.of(state(streak = 4), today, curriculum).badge)
    }

    /**
     * The widget renders against someone's wallpaper, so empty text is a visible defect
     * rather than a silent one. Every branch has to produce something readable.
     */
    @Test
    fun `no state produces an empty widget, at any rung`() {
        listOf(1, 2, 7, 40, 99, Engine.MAX_TIER).forEach { tier ->
            listOf(
                state(tier = tier),
                state(tier = tier, done = setOf(Pillar.ACCESS)),
                state(tier = tier, done = Pillar.order.toSet()),
                state(tier = tier, intentions = listOf(plan()))
            ).forEach { s ->
                val cue = WidgetCues.of(s, today, curriculum)
                assertTrue("blank kicker at tier $tier", cue.kicker.isNotBlank())
                assertTrue("blank cue at tier $tier", cue.cue.isNotBlank())
            }
        }
        assertTrue(WidgetCues.unavailable.cue.isNotBlank())
        assertTrue(WidgetCues.unavailable.kicker.isNotBlank())
    }

    /** Whatever tapping it says, the app has to be able to act on it. */
    @Test
    fun `every link the widget emits is one the app understands`() {
        val shapes = Regex("""^$|^tier:(ACCESS|ACTIVITY|SOCIAL):\d+$|^mission:\d+$""")
        (1..Engine.MAX_TIER).forEach { tier ->
            Pillar.order.forEach { p ->
                val cue = WidgetCues.of(
                    state(tier = tier, done = Pillar.order.takeWhile { it != p }.toSet()),
                    today,
                    curriculum
                )
                assertTrue("unroutable link '${cue.link}'", shapes.matches(cue.link))
            }
        }
    }
}
