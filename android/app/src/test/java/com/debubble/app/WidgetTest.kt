package com.debubble.app

import com.debubble.app.data.AppState
import com.debubble.app.data.LogEntry
import com.debubble.app.data.quests
import com.debubble.app.engine.Baseline
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.CareState
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Forecast
import com.debubble.app.engine.Intention
import com.debubble.app.engine.Ladder
import com.debubble.app.engine.Mobility
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
import com.debubble.app.engine.QuestKind
import com.debubble.app.engine.Today
import com.debubble.app.widget.WidgetCues
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Today's four, and what the home-screen widget says about them.
 *
 * Worth testing more carefully than most screens, for a reason that has nothing to do with
 * how much code it is: the widget is the only surface that renders in someone else's process,
 * on someone else's launcher, where a wrong decision shows up as stale or empty text rather
 * than as anything a user would recognise as a bug and report. The decisions are pure so that
 * they can be checked here instead of on a device. Reads the real shipped assets.
 */
class WidgetTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun asset(path: String): File =
        listOf(File("src/main/assets/$path"), File("app/src/main/assets/$path")).first { it.exists() }

    private val curriculum: Curriculum by lazy {
        Curriculum(
            Pillar.order.associateWith {
                json.decodeFromString<Ladder>(asset(it.asset).readText())
            }
        )
    }

    private val library: CareLibrary by lazy {
        json.decodeFromString<CareLibrary>(asset("care/routines.json").readText())
    }

    private val today = 20_000L

    /** Day one of the app is [today], so the rotation starts at its first pillar. */
    private fun state(
        done: Set<Pillar> = emptySet(),
        intentions: List<Intention> = emptyList(),
        forecasts: List<Forecast> = emptyList(),
        tier: Int = 1,
        startedDay: Long = today,
        care: Map<Domain, CareState> = emptyMap(),
        log: List<LogEntry> = emptyList(),
        movedIn: Boolean = true
    ) = AppState(
        onboarded = true,
        movedIn = movedIn,
        startedDay = startedDay,
        baseline = Baseline(moves = setOf(Mobility.WALK)),
        pillars = Pillar.order.associate { it.name to PillarState(tier = tier) },
        servedDay = today,
        doneToday = done.map { it.name }.toSet(),
        intentions = intentions,
        forecasts = forecasts,
        care = care.mapKeys { it.key.name },
        log = log
    )

    private fun quests(s: AppState) = s.quests(today, library, curriculum)

    private fun plan(id: String = "tier:ACCESS:1", day: Long = today) = Intention(
        id = 1,
        challengeId = id,
        challengeTitle = "Step outside",
        cue = "I next hear the kettle click off",
        response = "step outside for a minute",
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

    // ------------------------------------------------------------------ today's four

    @Test
    fun `today is body, room, heart, then one mission outside`() {
        val q = quests(state())
        assertEquals(listOf(QuestKind.CARE, QuestKind.CARE, QuestKind.CARE, QuestKind.OUTSIDE), q.map { it.kind })
        assertEquals(Domain.order, q.take(3).map { it.domain })
        assertTrue(q.all { !it.done })
        assertTrue("you hold the letter while the mission is open", Today.letter(q))
        q.filter { !it.done }.forEach {
            assertTrue("a task must lead with its moment: ${it.line}", it.line.startsWith("When "))
            assertTrue(it.first.isNotBlank())
        }
    }

    @Test
    fun `the mission rotates by day so every ladder gets its share`() {
        val seen = (0 until 3).map { offset ->
            quests(state(startedDay = today - offset)).last().pillar
        }
        assertEquals(Pillar.order.toSet(), seen.toSet())
    }

    @Test
    fun `a mission done today stays on the list as done, with what was done`() {
        val p = Today.outsidePillar(1, emptySet())
        val did = LogEntry(id = 1, pillar = p.name, tier = 3, title = "Walk the long way round", epochDay = today)
        val q = quests(state(done = setOf(p), log = listOf(did), tier = 4)).last()
        assertTrue(q.done)
        assertEquals("Walk the long way round", q.title)
        assertTrue(q.status.endsWith("level 3"))
        assertFalse("no letter once it is done", Today.letter(quests(state(done = setOf(p), log = listOf(did)))))
        // Any ladder done today counts as today's one, so finishing an extra first is not lost.
        val other = Pillar.order.last { it != p }
        assertEquals(other, quests(state(done = setOf(other))).last().pillar)
    }

    @Test
    fun `care done today is ticked, and a plan for the mission supplies its cue`() {
        val washed = mapOf(Domain.BODY to CareState(setDay = today, lastDoneDay = today, reps = 1))
        val q = quests(state(care = washed, intentions = listOf(plan())))
        assertTrue(q.first { it.domain == Domain.BODY }.done)
        assertEquals(1, Today.doneCount(q))
        assertEquals(Domain.ROOM, Today.next(q)?.domain)
        assertTrue(q.last().line.startsWith("When I next hear the kettle click off"))
    }

    // ------------------------------------------------------------------ the widget

    @Test
    fun `before moving in it says so instead of drawing an empty room`() {
        val cue = WidgetCues.of(state(movedIn = false), today, quests(state(movedIn = false)))
        assertFalse(cue.ready)
        assertTrue(cue.title.isNotBlank())
        assertTrue(cue.rows.isEmpty())
    }

    @Test
    fun `the widget shows the same four, counted`() {
        val s = state()
        val cue = WidgetCues.of(s, today, quests(s))
        assertTrue(cue.ready)
        assertEquals(4, cue.rows.size)
        assertEquals("0 of 4", cue.count)
        assertEquals(quests(s).map { it.icon }, cue.rows.map { it.icon })
        cue.rows.forEach { assertTrue("a row must lead with its moment", it.text.startsWith("When ")) }
    }

    @Test
    fun `all four done says so, and a done mission row opens nothing`() {
        val p = Today.outsidePillar(1, emptySet())
        val all = Domain.order.associateWith { CareState(setDay = today, lastDoneDay = today, reps = 1) }
        val did = LogEntry(id = 1, pillar = p.name, tier = 1, title = "Stand outside", epochDay = today)
        val s = state(done = setOf(p), care = all, log = listOf(did))
        val cue = WidgetCues.of(s, today, quests(s))
        assertEquals("4 of 4", cue.count)
        assertTrue(cue.title.contains("done", ignoreCase = true))
        assertTrue(cue.rows.all { it.done && it.text == "Done today" })
        assertEquals("", cue.rows.last().link)
    }

    /** A new day clears yesterday's completions, so the widget must not still say "done". */
    @Test
    fun `yesterday's completions do not carry over`() {
        val yesterday = state(done = Pillar.order.toSet()).copy(servedDay = today - 1).rolledTo(today)
        assertFalse(quests(yesterday).last().done)
    }

    @Test
    fun `a plan for something else gets its own line, a plan for the mission does not`() {
        val mine = state(intentions = listOf(plan()))
        assertEquals("", WidgetCues.of(mine, today, quests(mine)).plan)
        val other = state(intentions = listOf(plan(id = "mission:3")))
        assertTrue(WidgetCues.of(other, today, quests(other)).plan.startsWith("When I next hear"))
        val stale = state(intentions = listOf(plan(id = "mission:3", day = today - 5)))
        assertEquals("an overdue plan is a reproach, not a plan", "", WidgetCues.of(stale, today, quests(stale)).plan)
    }

    @Test
    fun `only a prediction from a previous day raises the review`() {
        fun review(f: Forecast) = state(forecasts = listOf(f)).let { WidgetCues.of(it, today, quests(it)).review }
        assertTrue(review(forecast(today - 1)))
        assertFalse("the challenge has not happened yet", review(forecast(today)))
        assertFalse(review(forecast(today - 3, resolved = true)))
    }

    /** Whatever tapping it says, the app has to be able to act on it. */
    @Test
    fun `every link the widget emits is one the app understands, at any rung`() {
        val shapes = Regex("""^$|^care:(BODY|ROOM|HEART)$|^tier:(ACCESS|ACTIVITY|SOCIAL):\d+$""")
        listOf(1, 2, 7, 40, 99, Engine.MAX_TIER).forEach { tier ->
            (0 until 3).forEach { offset ->
                val s = state(tier = tier, startedDay = today - offset)
                WidgetCues.of(s, today, quests(s)).rows.forEach { row ->
                    assertTrue("unroutable link '${row.link}'", shapes.matches(row.link))
                    assertTrue("blank row at tier $tier", row.name.isNotBlank() && row.text.isNotBlank())
                }
            }
        }
        assertTrue(WidgetCues.unavailable.title.isNotBlank())
    }
}
