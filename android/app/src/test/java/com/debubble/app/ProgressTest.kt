package com.debubble.app

import com.debubble.app.engine.Progress
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Levels and armour.
 *
 * The load-bearing property is the separation: XP unlocks clothes, Friction unlocks armour,
 * and no amount of one buys the other. If that ever stopped being true the avatar would stop
 * meaning anything, so it is asserted directly rather than left to the UI.
 */
class ProgressTest {

    @Test
    fun `levels start at one and advance every full bar of XP`() {
        assertEquals(1, Progress.level(0))
        assertEquals(1, Progress.level(Progress.XP_PER_LEVEL - 1))
        assertEquals(2, Progress.level(Progress.XP_PER_LEVEL))
        assertEquals(5, Progress.level(Progress.XP_PER_LEVEL * 4))
    }

    @Test
    fun `level progress runs zero to one and never reaches one`() {
        (0..Progress.XP_PER_LEVEL * 3).forEach { xp ->
            val p = Progress.levelProgress(xp)
            assertTrue("progress out of range at $xp: $p", p >= 0f && p < 1f)
        }
        assertEquals(0f, Progress.levelProgress(0), 1e-6f)
        assertEquals(0f, Progress.levelProgress(Progress.XP_PER_LEVEL), 1e-6f)
        assertEquals(0.5f, Progress.levelProgress(Progress.XP_PER_LEVEL / 2), 1e-6f)
    }

    @Test
    fun `xp into level always matches the bar`() {
        (0..500).forEach { xp ->
            assertEquals(
                Progress.xpIntoLevel(xp) / Progress.XP_PER_LEVEL.toFloat(),
                Progress.levelProgress(xp),
                1e-6f
            )
        }
    }

    @Test
    fun `a plate costs a fixed number of friction events and caps`() {
        assertEquals(0, Progress.plates(0))
        assertEquals(0, Progress.plates(Progress.FRICTION_PER_PLATE - 1))
        assertEquals(1, Progress.plates(Progress.FRICTION_PER_PLATE))
        assertEquals(
            Progress.MAX_PLATES,
            Progress.plates(Progress.FRICTION_PER_PLATE * Progress.MAX_PLATES)
        )
        // Past the cap it stays at the cap rather than overflowing the drawing code, which
        // only has room for MAX_PLATES rings inside the avatar's 100-unit grid.
        assertEquals(Progress.MAX_PLATES, Progress.plates(10_000))
    }

    @Test
    fun `the countdown to the next plate is always consistent with the count`() {
        (0..Progress.FRICTION_PER_PLATE * Progress.MAX_PLATES).forEach { f ->
            val need = Progress.toNextPlate(f)
            if (need == null) {
                assertEquals(
                    "null countdown only at the cap",
                    Progress.MAX_PLATES, Progress.plates(f)
                )
            } else {
                assertTrue("countdown must be positive at $f", need > 0)
                assertEquals(
                    "adding exactly the countdown must earn a plate at $f",
                    Progress.plates(f) + 1,
                    Progress.plates(f + need)
                )
            }
        }
    }

    @Test
    fun `plates never decrease as friction rises`() {
        var last = 0
        (0..200).forEach { f ->
            val p = Progress.plates(f)
            assertTrue("plates went backwards at $f", p >= last)
            last = p
        }
    }

    @Test
    fun `the aura keeps growing after every plate is earned, and stops at one`() {
        val capped = Progress.FRICTION_PER_PLATE * Progress.MAX_PLATES
        assertTrue(Progress.auraStrength(capped + 20) > Progress.auraStrength(capped))
        assertTrue(Progress.auraStrength(10_000) <= 1f)
        (0..500).forEach { f ->
            val a = Progress.auraStrength(f)
            assertTrue("aura out of range at $f: $a", a >= 0f && a <= 1f)
        }
    }

    /** The whole point of two currencies: neither one can be spent on the other. */
    @Test
    fun `no amount of XP earns a single plate`() {
        assertEquals(0, Progress.plates(0))
        // Level 50 with zero friction is a real and intended profile: someone who shows up
        // every day and never once goes past what is comfortable.
        assertEquals(50, Progress.level(Progress.XP_PER_LEVEL * 49))
        assertEquals(0, Progress.plates(0))
    }

    @Test
    fun `no amount of friction unlocks a cosmetic`() {
        val atLevelOne = 1
        val everything = Progress.hats + Progress.shirts + Progress.backdrops
        val lockedAtStart = everything.filter { !Progress.unlocked(it, atLevelOne) }
        assertTrue("there should be things to earn", lockedAtStart.isNotEmpty())
        // Friction is not an input to `unlocked` at all, which is the guarantee.
        lockedAtStart.forEach {
            assertFalse(Progress.unlocked(it, atLevelOne))
        }
    }

    @Test
    fun `every catalogue offers something from level one and something to work towards`() {
        listOf(
            "bodies" to Progress.bodies,
            "shapes" to Progress.shapes,
            "hats" to Progress.hats,
            "shirts" to Progress.shirts,
            "backdrops" to Progress.backdrops
        ).forEach { (name, items) ->
            assertTrue("$name must have a level 1 option", items.any { it.level <= 1 })
            assertTrue("$name needs unique ids", items.map { it.id } == items.map { it.id }.distinct())
            // Ids are used directly as indices by the drawing code.
            items.forEachIndexed { i, item ->
                assertEquals("$name id must equal its index", i, item.id)
            }
            assertTrue("$name names must be set", items.all { it.name.isNotBlank() })
        }
    }

    @Test
    fun `newly unlocked reports each item exactly once, on the level it arrives`() {
        val all = Progress.bodies + Progress.shapes + Progress.hats +
            Progress.shirts + Progress.backdrops
        val seen = mutableListOf<String>()
        (1..20).forEach { level ->
            Progress.newlyUnlocked(level - 1, level).forEach { item ->
                assertEquals("reported on the wrong level", level, item.level)
                seen += "${item.name}@${item.level}"
            }
        }
        assertEquals("every item should be reported once", seen.size, seen.distinct().size)
        val reachable = all.filter { it.level in 1..20 }
        assertEquals(reachable.size, seen.size)
    }

    @Test
    fun `no unlocks are reported for standing still or going backwards`() {
        assertTrue(Progress.newlyUnlocked(5, 5).isEmpty())
        assertTrue(Progress.newlyUnlocked(9, 3).isEmpty())
    }

    /** Friction pays XP too: the app's position is that it is output, not failure. */
    @Test
    fun `friction is never worth less XP than a plain rep`() {
        assertTrue(Progress.XP_FRICTION > Progress.XP_REP)
        assertTrue(Progress.XP_MISSION >= Progress.XP_CHALLENGE)
        listOf(
            Progress.XP_CHALLENGE, Progress.XP_MISSION,
            Progress.XP_REP, Progress.XP_FRICTION
        ).forEach { assertTrue("XP awards must be positive", it > 0) }
    }

    @Test
    fun `a level is reachable in a sane number of days`() {
        // Three challenges plus a mission is a full day: 90 XP, so a level lands every
        // day and a half of perfect play. Slow enough to mean something, fast enough
        // that the first unlock is not a week away.
        val perfectDay = Progress.XP_CHALLENGE * 3 + Progress.XP_MISSION
        assertTrue("a level should take more than one perfect day", perfectDay < Progress.XP_PER_LEVEL)
        assertTrue("a level should take less than three", perfectDay * 3 > Progress.XP_PER_LEVEL * 2)
    }

    @Test
    fun `no cosmetic is locked behind a level nobody would reach`() {
        val all = Progress.bodies + Progress.shapes + Progress.hats +
            Progress.shirts + Progress.backdrops
        assertNull(all.firstOrNull { it.level > 15 })
    }
}
