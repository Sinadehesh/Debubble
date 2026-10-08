package com.debubble.app

import com.debubble.app.engine.Habitat
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
import com.debubble.app.engine.Stage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The room, and the one property it must never lose.
 *
 * A stage that could be taken back would turn the surface into something to protect, and
 * protecting a number is the behaviour this whole app exists to interrupt. That is a claim
 * about monotonicity, which is the kind of claim a test can actually hold.
 */
class HabitatTest {

    @Test
    fun `everyone starts sealed in`() {
        assertEquals(Stage.SEALED, Habitat.stageFor(0))
        assertEquals(0, Stage.all.first().threshold)
    }

    @Test
    fun `the thresholds only ever go up`() {
        Stage.all.zipWithNext { a, b ->
            assertTrue("${a.label} -> ${b.label} is not an increase", b.threshold > a.threshold)
            assertTrue("${a.label} -> ${b.label} does not open further", b.openness > a.openness)
        }
    }

    /** The load-bearing one. More cleared rungs can never mean a smaller room. */
    @Test
    fun `the room never closes`() {
        var last = Stage.SEALED
        (0..400).forEach { n ->
            val stage = Habitat.stageFor(n)
            assertTrue(
                "at $n rungs the room went backwards: ${last.label} -> ${stage.label}",
                stage.ordinal >= last.ordinal
            )
            last = stage
        }
    }

    @Test
    fun `each stage begins exactly at its own threshold`() {
        Stage.all.forEach { s ->
            assertEquals(s, Habitat.stageFor(s.threshold))
            if (s.threshold > 0) {
                assertEquals(
                    "one rung short should still be the previous stage",
                    Stage.all[s.ordinal - 1],
                    Habitat.stageFor(s.threshold - 1)
                )
            }
        }
    }

    @Test
    fun `cleared rungs are counted across all three ladders`() {
        assertEquals(0, Habitat.cleared(emptyMap()))
        assertEquals(
            30,
            Habitat.cleared(Pillar.order.associateWith { PillarState(cleared = 10) })
        )
        // A pillar missing from the map counts as zero rather than blowing up — a state
        // written by an older build will not have all three keys.
        assertEquals(
            7,
            Habitat.cleared(mapOf(Pillar.SOCIAL to PillarState(cleared = 7)))
        )
    }

    // ---------------------------------------------------------------- the readout

    @Test
    fun `progress runs cleanly from zero to one inside every stage`() {
        // Every stage but the last begins empty. The last has nothing after it, so it reads
        // as full rather than as a bar that can never move.
        Stage.all.dropLast(1).forEach { s ->
            assertEquals("${s.label} should start empty", 0f, Habitat.progress(s.threshold), 1e-5f)
        }
        val last = Stage.all.last()
        assertEquals(1f, Habitat.progress(last.threshold), 1e-5f)
        assertEquals("past the end is still full", 1f, Habitat.progress(10_000), 1e-5f)
        (0..400).forEach { n ->
            val p = Habitat.progress(n)
            assertTrue("progress out of range at $n: $p", p in 0f..1f)
        }
    }

    @Test
    fun `the remaining count reaches zero exactly as the next stage opens`() {
        Stage.all.dropLast(1).forEach { s ->
            val next = Habitat.next(s)
            assertNotNull(next)
            assertEquals(1, Habitat.toNext(next!!.threshold - 1))
            assertEquals(next, Habitat.stageFor(next.threshold))
        }
        assertNull("the last stage has nothing after it", Habitat.next(Stage.all.last()))
        assertNull(Habitat.toNext(Stage.all.last().threshold))
    }

    @Test
    fun `there is always something to read, at every count`() {
        (0..400).forEach { n ->
            assertTrue("blank caption at $n rungs", Habitat.caption(n).isNotBlank())
        }
        // And it counts rungs rather than percentages, because one of those is a thing a
        // person can picture doing.
        assertTrue(Habitat.caption(1).contains(Regex("""\d|One""")))
    }

    @Test
    fun `every stage says what it is and what it means`() {
        Stage.all.forEach { s ->
            assertTrue(s.label.isNotBlank())
            assertTrue("${s.label} has no note", s.note.isNotBlank())
            assertTrue("${s.label} openness out of range", s.openness in 0f..1f)
        }
        assertEquals(0f, Stage.all.first().openness, 1e-5f)
        assertEquals(1f, Stage.all.last().openness, 1e-5f)
        assertEquals(Stage.all.size, Stage.all.map { it.label }.distinct().size)
    }

    /**
     * The colours are solved against the WCAG floors by android/tools/check_habitat.py, which
     * is where the real check lives. This only holds the shape, so a hand-edit that drops the
     * alpha byte cannot ship a transparent ground that renders as whatever is behind it.
     */
    @Test
    fun `every stage colour is fully opaque`() {
        Stage.all.forEach { s ->
            listOf("ground" to s.ground, "horizon" to s.horizon, "structure" to s.structure)
                .forEach { (name, value) ->
                    assertEquals(
                        "${s.label} $name is not opaque",
                        0xFFL,
                        (value ushr 24) and 0xFFL
                    )
                    assertTrue("${s.label} $name is out of range", value in 0xFF000000..0xFFFFFFFF)
                }
        }
    }
}
