package com.debubble.app

import com.debubble.app.engine.Care
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.CareState
import com.debubble.app.engine.Cues
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Family
import com.debubble.app.engine.Feel
import com.debubble.app.engine.Home
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.ItemCatalogue
import com.debubble.app.engine.Scene
import com.debubble.app.engine.Size
import com.debubble.app.engine.Spot
import com.debubble.app.engine.Stage
import java.io.File
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The care loop and the room it drives.
 *
 * These are the rules the user feels directly — how fast the room gets messy, what a tiny
 * task is worth, when something new arrives — so each one is pinned here rather than left
 * to be rediscovered from a bug report. Reads the real shipped assets, so a content edit that
 * breaks a rule fails here before it reaches anyone.
 */
class CareTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun asset(path: String): File {
        val candidates = listOf(File("src/main/assets/$path"), File("app/src/main/assets/$path"))
        return candidates.firstOrNull { it.isFile }
            ?: error("cannot locate $path from ${File(".").absolutePath}")
    }

    private val library: CareLibrary by lazy {
        json.decodeFromString<CareLibrary>(asset("care/routines.json").readText())
    }

    private val items: ItemCatalogue by lazy {
        json.decodeFromString<ItemCatalogue>(asset("pixel/items.json").readText())
    }

    private val day = 20_000L

    /* ------------------------------------------------------------ the asset agrees */

    @Test
    fun `the asset's rules are the engine's rules`() {
        // The web preview runs off the asset; the app runs off the constants. If they ever
        // disagree, the preview is showing someone an app that does not exist.
        val r = library.rules
        assertEquals(Care.MAX_DIRT, r.maxDirt)
        assertEquals(Care.REWARD_AT, r.rewardAt)
        assertEquals(Care.AUTOMATICITY_DAYS, r.automaticityDays)
        Domain.order.forEach { d -> assertEquals(d.name, d.daysPerLevel, r.daysPerLevel[d.name]) }
        assertEquals(Care.MAX_DIRT, items.maxDirt)
    }

    @Test
    fun `every domain has a library, one starter, and four named levels`() {
        Domain.order.forEach { d ->
            assertTrue("${d.name} has too few routines", library.routines(d).size >= 5)
            assertEquals(1, library.routines(d).count { it.starter })
            assertEquals(Care.MAX_DIRT + 1, library.info(d).levels.size)
            assertEquals(Care.MAX_DIRT + 1, library.info(d).checkin.size)
        }
    }

    @Test
    fun `every tiny is two minutes or less and every cue is a real moment`() {
        library.routines.forEach { r ->
            assertTrue("${r.id}: tiny is ${r.tinyMin} min", r.tinyMin <= 2)
            assertTrue("${r.id}: needs cues", r.anchors.size >= 2)
            r.anchors.forEach { a -> assertTrue("${r.id}: vague cue '$a'", Cues.isSpecific(a)) }
            assertTrue("${r.id}: no source", r.source.isNotBlank())
            assertTrue("${r.id}: no setup tip", r.ease.isNotBlank())
        }
    }

    @Test
    fun `a removed routine falls back to the starter instead of leaving a hole`() {
        val stale = CareState(routine = "deleted-in-a-later-build")
        assertEquals(library.starter(Domain.ROOM).id, library.chosen(Domain.ROOM, stale).id)
        val wrongDomain = CareState(routine = library.starter(Domain.HEART).id)
        assertEquals(library.starter(Domain.BODY).id, library.chosen(Domain.BODY, wrongDomain).id)
    }

    /* ------------------------------------------------------------ the mirror */

    @Test
    fun `before moving in the room holds still`() {
        val s = CareState()
        assertEquals(Care.START_DIRT, Care.dirt(Domain.BODY, s, day))
        assertEquals(Care.START_DIRT, Care.dirt(Domain.BODY, s, day + 400))
    }

    @Test
    fun `a body slips daily and a room every other day`() {
        val clean = Care.checkIn(CareState(), 0, day)
        assertEquals(1, Care.dirt(Domain.BODY, clean, day + 1))
        assertEquals(0, Care.dirt(Domain.ROOM, clean, day + 1))
        assertEquals(1, Care.dirt(Domain.ROOM, clean, day + 2))
        assertEquals(2, Care.dirt(Domain.HEART, clean, day + 4))
    }

    @Test
    fun `mess is capped, however long it has been`() {
        val clean = Care.checkIn(CareState(), 0, day)
        Domain.order.forEach { d ->
            assertEquals(Care.MAX_DIRT, Care.dirt(d, clean, day + 10_000))
        }
    }

    @Test
    fun `a tiny task lifts one level and a standard one cleans`() {
        val messy = Care.checkIn(CareState(), 3, day)
        assertEquals(2, Care.dirt(Domain.ROOM, Care.afterCare(Domain.ROOM, messy, Size.TINY, day), day))
        assertEquals(0, Care.dirt(Domain.ROOM, Care.afterCare(Domain.ROOM, messy, Size.STANDARD, day), day))
        assertEquals(0, Care.dirt(Domain.ROOM, Care.afterCare(Domain.ROOM, messy, Size.DEEP, day), day))
        // Tiny on a clean room cannot go below clean.
        val clean = Care.checkIn(CareState(), 0, day)
        assertEquals(0, Care.dirt(Domain.ROOM, Care.afterCare(Domain.ROOM, clean, Size.TINY, day), day))
    }

    @Test
    fun `tiny counts as a full rep, and only once a day`() {
        var s = Care.checkIn(CareState(), 2, day)
        s = Care.afterCare(Domain.HEART, s, Size.TINY, day)
        assertEquals(1, s.reps)
        s = Care.afterCare(Domain.HEART, s, Size.DEEP, day)
        assertEquals("a second session the same day still cleans", 0, Care.dirt(Domain.HEART, s, day))
        assertEquals("but it does not count twice", 1, s.reps)
        s = Care.afterCare(Domain.HEART, s, Size.TINY, day + 1)
        assertEquals(2, s.reps)
    }

    @Test
    fun `a check-in overrides the slip and restarts it`() {
        val clean = Care.checkIn(CareState(), 0, day)
        val honest = Care.checkIn(clean, 1, day + 5)
        assertEquals(1, Care.dirt(Domain.BODY, honest, day + 5))
        assertEquals(2, Care.dirt(Domain.BODY, honest, day + 6))
    }

    /* ------------------------------------------------------------ sizing */

    @Test
    fun `tiny is suggested for the first week and after a gap`() {
        assertEquals(Size.TINY, Care.suggestedSize(CareState(), day))
        val settled = CareState(reps = Care.TINY_FIRST_REPS, lastDoneDay = day - 1)
        assertEquals(Size.STANDARD, Care.suggestedSize(settled, day))
        val away = settled.copy(lastDoneDay = day - Care.COMING_BACK_DAYS)
        assertEquals(Size.TINY, Care.suggestedSize(away, day))
        assertNotNull(Care.sizeNote(away, day))
        assertNull(Care.sizeNote(settled, day))
    }

    /* ------------------------------------------------------------ rewards */

    @Test
    fun `the first care-day earns something, and the schedule spreads out`() {
        assertEquals(0, Care.owed(0))
        assertEquals(1, Care.owed(1))
        assertEquals(2, Care.owed(3))
        assertEquals(Care.REWARD_AT.size, Care.owed(1_000))
        assertEquals(2, Care.nextRewardIn(1))
        assertNull(Care.nextRewardIn(66))
        val gaps = Care.REWARD_AT.zipWithNext { a, b -> b - a }
        assertEquals("gaps never shrink", gaps, gaps.sorted())
    }

    @Test
    fun `every family has an item for every reward, and one first`() {
        Family.entries.forEach { f ->
            assertEquals(f.name, Care.REWARD_AT.size, items.family(f).size)
            assertEquals(f.name, 1, items.family(f).count { it.first })
        }
    }

    @Test
    fun `the first item is the chosen one, then nothing repeats`() {
        Family.entries.forEach { f ->
            val first = Home.pick(items, f, emptyList(), seed = 42L)
            assertNotNull(first)
            assertTrue(first!!.first)
            val (home, granted) = Home.settle(HomeState(), items, f, 66, seed = 42L)
            assertEquals(items.family(f).map { it.id }.toSet(), granted.map { it.id }.toSet())
            assertEquals(home.owned.size, home.owned.toSet().size)
            assertNull(Home.pick(items, f, home.owned, seed = 42L))
        }
    }

    @Test
    fun `settling is derived from the count, so it never grants twice`() {
        val (once, first) = Home.settle(HomeState(), items, Family.ROOM, 6, seed = 7L)
        assertEquals(3, first.size)
        val (twice, again) = Home.settle(once, items, Family.ROOM, 6, seed = 7L)
        assertTrue(again.isEmpty())
        assertEquals(once, twice)
        // A family's items never leak into another family's count.
        val (mixed, _) = Home.settle(once, items, Family.BODY, 1, seed = 7L)
        assertEquals(3, Home.ownedIn(mixed, items, Family.ROOM))
        assertEquals(1, Home.ownedIn(mixed, items, Family.BODY))
    }

    @Test
    fun `an arriving item is placed where it belongs, and an upgrader is paid in full`() {
        val (home, granted) = Home.settle(HomeState(), items, Family.WORLD, 40, seed = 3L)
        assertEquals(Care.owed(40), granted.size)
        granted.forEach { assertEquals(it.spot, home.placed[it.id]) }
        assertEquals(granted.map { it.id }.toSet(), home.unseen)
    }

    @Test
    fun `different people get different surprises`() {
        val a = Home.settle(HomeState(), items, Family.HEART, 66, seed = 1L).second.map { it.id }
        val b = Home.settle(HomeState(), items, Family.HEART, 66, seed = 999_999L).second.map { it.id }
        assertEquals(a.first(), b.first())
        assertFalse("the order after the first should depend on the seed", a == b)
    }

    @Test
    fun `default spots are inside the room`() {
        items.items.forEach { i ->
            val clamped = Home.clamp(items.room, i.w, i.h, i.onWall, i.spot.x, i.spot.b)
            assertEquals("${i.id} lands outside where it belongs", i.spot, clamped)
        }
    }

    @Test
    fun `dragging keeps floor things on the floor and wall things on the wall`() {
        val room = items.room
        val onCeiling = Home.clamp(room, 24, 10, onWall = false, x = 50, b = 3)
        assertEquals(room.floorY + 2, onCeiling.b)
        val inBasement = Home.clamp(room, 12, 12, onWall = true, x = 50, b = 99)
        assertEquals(room.floorY - room.skirt, inBasement.b)
        val offEdge = Home.clamp(room, 20, 10, onWall = false, x = 500, b = 80)
        assertEquals(room.w - 20, offEdge.x)
    }

    /* ------------------------------------------------------------ habit and evidence */

    @Test
    fun `automaticity follows the median curve`() {
        assertEquals(0f, Care.automaticity(0), 0f)
        assertEquals(0.95f, Care.automaticity(Care.AUTOMATICITY_DAYS), 0.001f)
        var last = 0f
        for (n in 1..200) {
            val a = Care.automaticity(n)
            assertTrue(a > last && a <= 1f)
            last = a
        }
    }

    @Test
    fun `evidence waits for enough answers, then reports them honestly`() {
        val info = library.info(Domain.HEART)
        var s = CareState()
        s = Care.afterFeeling(s, Feel.BETTER)
        s = Care.afterFeeling(s, Feel.BETTER)
        assertNull(Care.evidence(info, s))
        s = Care.afterFeeling(s, Feel.SAME)
        assertTrue(Care.evidence(info, s)!!.contains("2 of 3"))
        val worse = CareState(worse = 3, better = 0)
        assertTrue(Care.evidence(info, worse)!!.contains("different routine"))
    }

    @Test
    fun `welcome back is kind and only after a real gap`() {
        assertNull(Care.welcomeBack(1))
        val back = Care.welcomeBack(Care.COMING_BACK_DAYS)!!
        assertFalse(back.contains("missed", ignoreCase = true))
        assertFalse(back.contains("streak", ignoreCase = true))
        assertNotNull(Care.welcomeBack(60))
    }

    /* ------------------------------------------------------------ the scene */

    private fun homeWith(vararg ids: String): HomeState =
        HomeState(owned = ids.toList(), placed = ids.associateWith { items.item(it)!!.spot })

    private fun levels(body: Int, room: Int, heart: Int) =
        mapOf(Domain.BODY to body, Domain.ROOM to room, Domain.HEART to heart)

    @Test
    fun `flat things are drawn under what stands on them, and nearer over further`() {
        val draws = Scene.draws(items, Stage.WINDOW, homeWith("rug", "cat", "bed", "sneakers"), levels(1, 1, 1))
        val order = draws.map { it.id }
        assertTrue(order.indexOf("rug") < order.indexOf("cat"))
        assertTrue("the bed is further back than the trainers", order.indexOf("bed") < order.indexOf("sneakers"))
    }

    @Test
    fun `a messier room shows more mess, and a clean one sparkles`() {
        val mess = items.mess.map { it.fx }.toSet()
        val counts = (0..Care.MAX_DIRT).map { room ->
            Scene.draws(items, Stage.WINDOW, HomeState(), levels(1, room, 1)).count { it.id in mess }
        }
        assertEquals(0, counts[0])
        assertEquals(counts, counts.sorted())
        assertTrue(counts[3] > counts[1])
        val clean = Scene.draws(items, Stage.WINDOW, HomeState(), levels(1, 0, 1))
        assertTrue(clean.any { it.id == "sparkle" })
        val lived = Scene.draws(items, Stage.WINDOW, HomeState(), levels(1, 1, 1))
        assertFalse(lived.any { it.id == "sparkle" })
    }

    @Test
    fun `you show the body, and the weather over you shows the heart`() {
        (0..Care.MAX_DIRT).forEach { body ->
            val draws = Scene.draws(items, Stage.SEALED, HomeState(), levels(body, 1, 1))
            assertEquals(1, draws.count { it.id == "me$body" })
        }
        assertTrue(Scene.draws(items, Stage.SEALED, HomeState(), levels(1, 1, 1)).none { it.id.startsWith("heart_") })
        assertTrue(Scene.draws(items, Stage.SEALED, HomeState(), levels(1, 1, 3)).any { it.id == "heart_storm" })
        assertTrue(Scene.draws(items, Stage.SEALED, HomeState(), levels(1, 1, 0)).any { it.id == "heart_sun" })
    }

    @Test
    fun `wall things come down when there are no walls, and come back with them`() {
        val home = homeWith("mirror", "bed")
        assertTrue(Scene.draws(items, Stage.WINDOW, home, levels(1, 1, 1)).any { it.id == "mirror" })
        val outside = Scene.draws(items, Stage.UNWALLED, home, levels(1, 1, 1))
        assertFalse(outside.any { it.id == "mirror" })
        assertTrue(outside.any { it.id == "bed" })
    }

    @Test
    fun `a put-away item is owned but not drawn`() {
        val home = homeWith("lamp").let { it.copy(placed = emptyMap()) }
        assertFalse(Scene.draws(items, Stage.WINDOW, home, levels(1, 1, 1)).any { it.id == "lamp" })
    }

    @Test
    fun `dragging draws the item where the finger is, not where it was`() {
        val home = homeWith("lamp")
        val to = Spot(100, 90)
        val draw = Scene.draws(items, Stage.WINDOW, home, levels(1, 1, 1), moving = "lamp" to to)
            .first { it.id == "lamp" }
        assertEquals(to.x, draw.x)
        assertEquals(to.b, draw.b)
    }

    @Test
    fun `touching something picks the thing on top, and touching the floor picks nothing`() {
        val home = homeWith("bed")
        val bed = items.item("bed")!!
        val inside = Scene.hit(items, Stage.WINDOW, home, levels(1, 0, 1), bed.spot.x + 5, bed.spot.b - 3)
        assertEquals("bed", inside)
        val me = Home.meSpot(home, items)
        assertEquals(Scene.ME, Scene.hit(items, Stage.WINDOW, home, levels(1, 0, 1), me.x + 10, me.b - 8))
        assertNull(Scene.hit(items, Stage.WINDOW, home, levels(1, 0, 1), 158, 2))
    }
}
