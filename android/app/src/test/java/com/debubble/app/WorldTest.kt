package com.debubble.app

import com.debubble.app.engine.Domain
import com.debubble.app.engine.Goal
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.ItemCatalogue
import com.debubble.app.engine.Look
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Progress
import com.debubble.app.engine.RoomTap
import com.debubble.app.engine.Scene
import com.debubble.app.engine.Stage
import com.debubble.app.engine.World
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The world the older parts now live in: the courage jar, the roads and landmarks, and the
 * hats a level earns. Reads the real shipped atlas.
 */
class WorldTest {

    private val json = Json { ignoreUnknownKeys = true }
    private fun asset(path: String): File =
        listOf(File("src/main/assets/$path"), File("app/src/main/assets/$path")).first { it.exists() }
    private val items: ItemCatalogue by lazy {
        json.decodeFromString<ItemCatalogue>(asset("pixel/items.json").readText())
    }

    @Test
    fun `every sprite the world draws is in the atlas`() {
        World.ICONS.forEach { assertTrue("missing $it", items.fx(it) != null) }
        Pillar.order.forEach { assertTrue(items.fx(World.road(it)) != null) }
        Goal.all.forEach { assertTrue(items.fx(World.landmark(it)) != null) }
        assertEquals("one sign per road", Pillar.order.size, Pillar.order.map { World.road(it) }.toSet().size)
        assertEquals("one place per journey", Goal.all.size, Goal.all.map { World.landmark(it) }.toSet().size)
    }

    @Test
    fun `the courage jar only ever fills, and says how far to the next spark`() {
        val levels = (0..40).map { World.jarLevel(it) }
        assertEquals(0, levels.first())
        assertEquals(4, levels.last())
        assertTrue("never empties", levels.zipWithNext().all { (a, b) -> b >= a })
        assertEquals("the first spark shows straight away", 1, World.jarLevel(1))
        (0..40).forEach { f ->
            val next = World.toNextSpark(f)
            if (World.jarLevel(f) == 4) assertNull(next) else {
                assertTrue("at $f", next != null && next > 0)
                assertEquals("at $f", World.jarLevel(f) + 1, World.jarLevel(f + next!!))
            }
        }
        (0..4).forEach { assertTrue(items.fx("jar$it") != null) }
    }

    @Test
    fun `hats arrive in level order, none at the start, and the next one is always ahead`() {
        val hats = items.hats
        assertTrue(hats.isNotEmpty())
        assertEquals(hats.sortedBy { it.level }, hats)
        assertTrue(hats.first().level > 1)
        assertTrue(World.hatsAt(items, 1).isEmpty())
        assertEquals(hats.size, World.hatsAt(items, 99).size)
        (1..20).forEach { level ->
            World.nextHat(items, level)?.let { assertTrue(it.level > level) }
        }
        hats.forEach { h ->
            val f = items.fx(h.id)!!
            assertEquals("${h.id} is drawn in your frame", items.me.w, f.w)
            assertTrue(f.h < items.me.h)
        }
    }

    @Test
    fun `your hat sits on your head, right after you, and tapping it is tapping you`() {
        items.hats.forEachIndexed { i, h ->
            val home = HomeState(look = Look(hat = i + 1))
            val dirt = Domain.order.associateWith { 1 }
            listOf(0, 1).forEach { frame ->
                val draws = Scene.draws(items, Stage.WINDOW, home, dirt, frame = frame)
                val me = draws.indexOfFirst { it.id == Scene.meFrame(1) }
                val hat = draws.indexOfFirst { it.id == h.id }
                assertEquals("${h.id} straight after you", me + 1, hat)
                val top = draws[me].b - items.me.h
                assertEquals("${h.id} top at your top", top, draws[hat].b - items.fx(h.id)!!.h)
            }
            val spot = Scene.spots(items, Stage.WINDOW, home, dirt, letter = false).getValue(RoomTap.BODY)
            assertEquals(RoomTap.BODY, Scene.tap(items, Stage.WINDOW, home, dirt, false, spot.cx, spot.y + 2))
        }
        // No hat, or one that does not exist, draws nothing extra.
        val bare = Scene.draws(items, Stage.WINDOW, HomeState(), Domain.order.associateWith { 1 })
        assertTrue(bare.none { it.id.startsWith("hat_") })
        val bogus = Scene.draws(items, Stage.WINDOW, HomeState(look = Look(hat = 99)), Domain.order.associateWith { 1 })
        assertTrue(bogus.none { it.id.startsWith("hat_") })
    }

    @Test
    fun `a level is reachable from what a week of the app pays`() {
        // Four care tasks and a mission a day for a week should earn at least the first hat.
        val week = 7 * (3 * Progress.XP_CARE + Progress.XP_CHALLENGE)
        assertTrue(Progress.level(week) >= items.hats.first().level)
    }
}
