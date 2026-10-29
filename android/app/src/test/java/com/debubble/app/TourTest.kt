package com.debubble.app

import com.debubble.app.data.AppState
import com.debubble.app.data.LogEntry
import com.debubble.app.data.quests
import com.debubble.app.engine.Baseline
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.CareState
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Domain
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.ItemCatalogue
import com.debubble.app.engine.Ladder
import com.debubble.app.engine.Mobility
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
import com.debubble.app.engine.RoomTap
import com.debubble.app.engine.Scene
import com.debubble.app.engine.Spot
import com.debubble.app.engine.Stage
import com.debubble.app.engine.Today
import com.debubble.app.engine.Tour
import com.debubble.app.engine.TourAction
import com.debubble.app.engine.TourSpot
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The first-day tour. What it says has to match what is on the screen: a step that says
 * "tap the mess" has to light up something that, tapped, opens the Room task. Reads the
 * real shipped assets.
 */
class TourTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun asset(path: String): File =
        listOf(File("src/main/assets/$path"), File("app/src/main/assets/$path")).first { it.exists() }

    private val curriculum: Curriculum by lazy {
        Curriculum(Pillar.order.associateWith { json.decodeFromString<Ladder>(asset(it.asset).readText()) })
    }
    private val library: CareLibrary by lazy {
        json.decodeFromString<CareLibrary>(asset("care/routines.json").readText())
    }
    private val items: ItemCatalogue by lazy {
        json.decodeFromString<ItemCatalogue>(asset("pixel/items.json").readText())
    }

    private val today = 20_000L

    private fun state(
        care: Map<Domain, CareState> = emptyMap(),
        done: Set<Pillar> = emptySet(),
        log: List<LogEntry> = emptyList()
    ) = AppState(
        onboarded = true,
        movedIn = true,
        startedDay = today,
        baseline = Baseline(moves = setOf(Mobility.WALK)),
        pillars = Pillar.order.associate { it.name to PillarState(tier = 1) },
        servedDay = today,
        doneToday = done.map { it.name }.toSet(),
        care = care.mapKeys { it.key.name },
        log = log
    )

    private fun quests(s: AppState = state()) = s.quests(today, library, curriculum)

    private val everywhere = TourSpot.entries.toSet()

    // ------------------------------------------------------------------ the steps

    @Test
    fun `it starts at the room and ends by opening the next thing to do`() {
        val q = quests()
        val steps = Tour.steps(q, widget = true, canPin = true)
        assertEquals(TourSpot.ROOM, steps.first().spot)
        // You, the mess, your heart and the letter, in the order the list has them.
        assertEquals(
            listOf(TourSpot.ME, TourSpot.MESS, TourSpot.BUDDY, TourSpot.LETTER),
            steps.drop(1).take(4).map { it.spot }
        )
        val last = steps.last()
        assertEquals(TourAction.START_NEXT, last.action)
        assertEquals(Tour.spotOf(Today.next(q)!!), last.spot)
        assertTrue("the last step names the act", last.body.contains(Today.next(q)!!.name))
    }

    @Test
    fun `every step is short enough to read in one go`() {
        Tour.steps(quests(), widget = true, canPin = true).forEach { s ->
            assertTrue("title too long: ${s.title}", s.title.length <= 40)
            assertTrue("too much at once (${s.body.length}): ${s.title}", s.body.length <= 280)
            s.absent?.let { assertTrue("absent too long: ${s.title}", it.length <= 280) }
            assertTrue(s.button.isNotBlank())
        }
    }

    @Test
    fun `every sprite a step shows is in the atlas`() {
        Tour.steps(quests(), widget = true, canPin = true).mapNotNull { it.icon }.forEach { id ->
            assertTrue("missing sprite $id", items.fx(id) != null)
        }
    }

    @Test
    fun `the how-to step points at the first care still to do`() {
        val washed = mapOf(Domain.BODY to CareState(setDay = today, lastDoneDay = today, reps = 1))
        val steps = Tour.steps(quests(state(care = washed)), widget = false, canPin = false)
        assertEquals(TourSpot.ROW_ROOM, steps.first { it.title == "How to do one" }.spot)
    }

    @Test
    fun `the widget step is only there when there is no widget, and only offers to pin when it can`() {
        val q = quests()
        assertTrue(Tour.steps(q, widget = false, canPin = true).none { it.action == TourAction.ADD_WIDGET })
        val pin = Tour.steps(q, widget = true, canPin = true).single { it.action == TourAction.ADD_WIDGET }
        assertEquals("Add the widget", pin.button)
        val manual = Tour.steps(q, widget = true, canPin = false).single { it.title == "Keep it on your home screen" }
        assertNull(manual.action)
        assertTrue(manual.body.contains("Widgets"))
    }

    @Test
    fun `with everything done it ends on the day, not on a task`() {
        val p = Today.outsidePillar(1, emptySet())
        val all = Domain.order.associateWith { CareState(setDay = today, lastDoneDay = today, reps = 1) }
        val did = LogEntry(id = 1, pillar = p.name, tier = 1, title = "Stand outside", epochDay = today)
        val last = Tour.steps(quests(state(care = all, done = setOf(p), log = listOf(did))), false, false).last()
        assertNull(last.action)
        assertEquals("Finish", last.button)
    }

    // ------------------------------------------------------------------ what is on screen

    @Test
    fun `a spot that is not drawn falls back to the room, or the step goes`() {
        val steps = Tour.steps(quests(), widget = true, canPin = true)
        val shown = Tour.visible(steps, everywhere - TourSpot.MESS - TourSpot.LETTER - TourSpot.ROW_OUTSIDE)
        val mess = shown.first { it.title == "The mess is your Room" }
        assertEquals(TourSpot.ROOM, mess.spot)
        assertTrue(mess.body.startsWith("When your real room"))
        assertEquals(TourSpot.ROOM, shown.first { it.title == "The letter is today's mission" }.spot)
        assertFalse(shown.any { it.title == "Going out works a bit differently" })
        // The widget step has no spot and is kept regardless.
        assertTrue(shown.any { it.action == TourAction.ADD_WIDGET })
        assertEquals(steps, Tour.visible(steps, everywhere))
    }

    /** "Tap the mess", "tap yourself": the middle of what the tour lights up must do that. */
    @Test
    fun `the middle of every lit-up thing in the room opens the task it is about`() {
        val homes = listOf(
            HomeState(),
            HomeState(
                owned = listOf("rug", "cat", "bed", "sneakers"),
                placed = listOf("rug", "cat", "bed", "sneakers").associateWith { id -> items.item(id)!!.spot }
            ),
            HomeState(me = Spot(0, 96)),
            HomeState(me = Spot(items.room.w - items.me.w, 96))
        )
        val levels = (0..3).flatMap { b -> (0..3).flatMap { r -> (0..3).map { h -> Triple(b, r, h) } } }
        listOf(Stage.SEALED, Stage.WINDOW, Stage.UNWALLED).forEach { stage ->
            homes.forEach { home ->
                levels.forEach { (b, r, h) ->
                    val dirt = mapOf(Domain.BODY to b, Domain.ROOM to r, Domain.HEART to h)
                    listOf(false, true).forEach { letter ->
                        val spots = Scene.spots(items, stage, home, dirt, letter)
                        val where = "$stage $b$r$h letter=$letter me=${home.me}"
                        assertTrue(where, RoomTap.BODY in spots && RoomTap.HEART in spots)
                        assertEquals(where, r > 0, RoomTap.ROOM in spots)
                        assertEquals(where, letter, RoomTap.OUTSIDE in spots)
                        spots.forEach { (kind, a) ->
                            assertTrue("$kind outside the room, $where",
                                a.x >= 0 && a.y >= 0 && a.x + a.w <= items.room.w && a.y + a.h <= items.room.h)
                            assertEquals("$kind, $where", kind,
                                Scene.tap(items, stage, home, dirt, letter, a.cx, a.cy))
                        }
                    }
                }
            }
        }
    }
}
