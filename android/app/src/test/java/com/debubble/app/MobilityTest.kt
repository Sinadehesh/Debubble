package com.debubble.app

import com.debubble.app.engine.Baseline
import com.debubble.app.engine.Calibration
import com.debubble.app.engine.Copy
import com.debubble.app.engine.Mobility
import com.debubble.app.engine.Needs
import com.debubble.app.engine.Pillar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The copy adapter is the accessibility feature with the widest blast radius — it rewrites
 * every directive a non-walking user ever sees — so it is tested against the real strings
 * rather than against invented ones.
 */
class CopyTest {

    private val walker = setOf(Mobility.WALK)
    private val roller = setOf(Mobility.WHEELS)
    private val driver = setOf(Mobility.CAR, Mobility.TRANSIT)
    private val both = setOf(Mobility.WALK, Mobility.WHEELS)

    @Test
    fun `a walker sees the authored text untouched`() {
        val source = "Walk to the end of your street and back."
        assertEquals(source, Copy.adapt(source, walker))
        assertFalse(Copy.rewrites(walker))
    }

    @Test
    fun `selecting both walking and wheels leaves the text alone`() {
        // Walking is available, so the curriculum's own wording is already correct and
        // rewriting it would be presumptuous.
        val source = "Walk a full lap around the outside of your own building."
        assertEquals(source, Copy.adapt(source, both))
    }

    @Test
    fun `a wheelchair user rolls`() {
        assertEquals(
            "Roll to the end of your street and back.",
            Copy.adapt("Walk to the end of your street and back.", roller)
        )
        assertEquals(
            "Roll for fifteen minutes, choosing every turn at random.",
            Copy.adapt("Walk for fifteen minutes, choosing every turn at random.", roller)
        )
    }

    @Test
    fun `someone with only a car or transit heads rather than rolls`() {
        assertEquals(
            "Head to the end of your street and back.",
            Copy.adapt("Walk to the end of your street and back.", driver)
        )
    }

    /** "an hour's walk" is a noun. Turning it into "an hour's roll" is the obvious bug. */
    @Test
    fun `walk as a noun becomes a journey, not a verb`() {
        assertEquals(
            "Spend an hour anywhere over an hour's travel from home.",
            Copy.adapt("Spend an hour anywhere over an hour's walk from home.", roller)
        )
        assertEquals(
            "Take a ten-minute trip with no audio in your ears.",
            Copy.adapt("Take a ten-minute walk with no audio in your ears.", roller)
        )
        assertEquals(
            "A trip, a table, nothing on.",
            Copy.adapt("A walk, a table, nothing on.", roller)
        )
    }

    /** "a green space you have never headed in" is not English. */
    @Test
    fun `the head variant uses neutral past and progressive forms`() {
        assertEquals(
            "Find the nearest green space you have never been in.",
            Copy.adapt("Find the nearest green space you have never walked in.", driver)
        )
        assertEquals(
            "Find the nearest green space you have never rolled in.",
            Copy.adapt("Find the nearest green space you have never walked in.", roller)
        )
    }

    @Test
    fun `on foot becomes a phrase that makes no claim about feet`() {
        assertEquals(
            "Roll to the furthest point you can reach in twenty minutes under your own power.",
            Copy.adapt(
                "Walk to the furthest point you can reach in twenty minutes on foot.",
                roller
            )
        )
    }

    /** The very first challenge in the app says "Stand there for sixty seconds". */
    @Test
    fun `standing becomes staying`() {
        assertEquals(
            "Step outside your front door. Stay there for sixty seconds. Go back in.",
            Copy.adapt(
                "Step outside your front door. Stand there for sixty seconds. Go back in.",
                roller
            )
        )
    }

    @Test
    fun `nothing survives a rewrite still telling the reader to walk`() {
        val samples = listOf(
            "Walk to the end of your street and back.",
            "Walk a lit route you know at an hour you never walk it.",
            "Find the nearest green space you have never walked in.",
            "Stand in a public place for three minutes without touching your phone.",
            "Walk to the furthest point you can reach in twenty minutes on foot.",
            "Leave the house before 7am and walk for twenty minutes."
        )
        listOf(roller, driver).forEach { modes ->
            samples.forEach { source ->
                val out = Copy.adapt(source, modes)
                assertFalse(
                    "\"$out\" still contains a gait word for $modes",
                    Regex("""(?i)\bwalk\w*\b|\bon foot\b|\bStand\b""").containsMatchIn(out)
                )
            }
        }
    }
}

class MobilityBaselineTest {

    /**
     * State written before mobility existed has `transport` and no `moves`. Those builds
     * printed "on foot is assumed" on the setup card, so walking really was part of that
     * answer and the migration is recovering it rather than inventing it.
     */
    @Test
    fun `pre-mobility state migrates to its old meaning`() {
        val legacy = Baseline(transport = setOf(Needs.TRANSIT), moves = emptySet())
        assertEquals(setOf(Needs.TRANSIT, Mobility.WALK), legacy.modes)
        assertTrue(legacy.has(Needs.TRANSIT))
        assertFalse(legacy.has(Needs.CAR))
        assertFalse("a migrated walker must not have their copy rewritten", legacy.rewritesCopy)
    }

    @Test
    fun `moves wins over the legacy field once it is set`() {
        val b = Baseline(transport = setOf(Needs.CAR), moves = setOf(Mobility.WHEELS))
        assertEquals(setOf(Mobility.WHEELS), b.modes)
        assertFalse("the legacy car must not leak through", b.has(Needs.CAR))
    }

    /**
     * The Access entry tier counts vehicles. `modes` also holds walking and wheels, so
     * counting the whole set would silently start every wheelchair user a tier higher.
     */
    @Test
    fun `self-powered modes do not inflate the starting level`() {
        val base = Baseline(radiusKm = 0, moves = setOf(Mobility.WALK))
        val withWheels = base.copy(moves = setOf(Mobility.WALK, Mobility.WHEELS))
        assertEquals(
            Calibration.entryTier(Pillar.ACCESS, base),
            Calibration.entryTier(Pillar.ACCESS, withWheels)
        )
    }

    @Test
    fun `each vehicle adds exactly one level of Access`() {
        val walkOnly = Baseline(radiusKm = 0, moves = setOf(Mobility.WALK))
        val plusBus = walkOnly.copy(moves = walkOnly.moves + Needs.TRANSIT)
        val plusAll = walkOnly.copy(
            moves = walkOnly.moves + setOf(Needs.TRANSIT, Needs.BIKE, Needs.CAR)
        )
        val a = Calibration.entryTier(Pillar.ACCESS, walkOnly)
        assertEquals(a + 1, Calibration.entryTier(Pillar.ACCESS, plusBus))
        assertEquals(a + 3, Calibration.entryTier(Pillar.ACCESS, plusAll))
    }

    @Test
    fun `every offered mode has a label and a description`() {
        Mobility.all.forEach { key ->
            assertTrue("$key needs a label", Mobility.label(key).isNotBlank())
            assertTrue("$key needs a detail line", Mobility.detail(key).isNotBlank())
            assertTrue("$key must not fall through to its raw key", Mobility.label(key) != key)
        }
        assertEquals(5, Mobility.all.size)
        assertEquals(Mobility.all.size, Mobility.all.distinct().size)
    }
}
