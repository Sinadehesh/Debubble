package com.debubble.app

import com.debubble.app.engine.Baseline
import com.debubble.app.engine.Calibration
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Needs
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CalibrationTest {

    @Test
    fun `the most sheltered baseline starts every pillar at tier 1`() {
        val b = Baseline(
            transport = emptySet(),
            radiusKm = 0,
            routinePct = 100,
            noveltyRecency = 0,
            socialResistance = 10,
            longConversations = 0
        )
        Pillar.order.forEach { p ->
            assertEquals("$p should start at 1", 1, Calibration.entryTier(p, b))
        }
    }

    @Test
    fun `a mobile, varied, sociable baseline starts well up the ladder`() {
        val b = Baseline(
            transport = setOf(Needs.TRANSIT, Needs.BIKE, Needs.CAR),
            radiusKm = 40,
            routinePct = 20,
            noveltyRecency = 4,
            socialResistance = 1,
            longConversations = 16
        )
        Pillar.order.forEach { p ->
            assertTrue("$p should start above 1", Calibration.entryTier(p, b) > 1)
        }
        // A cyclist who ranged 40km must never be told to walk to the end of their street.
        assertTrue(Calibration.entryTier(Pillar.ACCESS, b) >= 8)
    }

    @Test
    fun `entry tier is always inside the calibration cap`() {
        // Sweep the whole answer space rather than spot-checking.
        for (km in 0..60 step 3) {
            for (routine in 0..100 step 10) {
                for (resistance in 0..10) {
                    for (novelty in 0..4) {
                        for (convos in 0..20 step 5) {
                            val b = Baseline(
                                transport = setOf(Needs.TRANSIT, Needs.BIKE, Needs.CAR),
                                radiusKm = km,
                                routinePct = routine,
                                noveltyRecency = novelty,
                                socialResistance = resistance,
                                longConversations = convos
                            )
                            Pillar.order.forEach { p ->
                                val t = Calibration.entryTier(p, b)
                                assertTrue(
                                    "$p tier $t out of range for $b",
                                    t in 1..Calibration.MAX_ENTRY
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Test
    fun `entry tier rises monotonically with capability on each axis`() {
        var previous = 0
        for (km in 0..60 step 6) {
            val t = Calibration.entryTier(Pillar.ACCESS, Baseline(radiusKm = km))
            assertTrue("access entry should not fall as range grows", t >= previous)
            previous = t
        }
        previous = 0
        for (resistance in 10 downTo 0) {
            val t = Calibration.entryTier(Pillar.SOCIAL, Baseline(socialResistance = resistance))
            assertTrue("social entry should not fall as resistance drops", t >= previous)
            previous = t
        }
    }
}

class ProgressionTest {

    @Test
    fun `completion advances one tier and records the investment`() {
        val after = Engine.onCompleted(PillarState(tier = 5), minutes = 20)
        assertEquals(6, after.tier)
        assertEquals(1, after.cleared)
        assertEquals(20, after.minutesInvested)
    }

    @Test
    fun `three clean completions earn a double step`() {
        var s = PillarState(tier = 1)
        s = Engine.onCompleted(s, 5)   // -> 2
        s = Engine.onCompleted(s, 5)   // -> 3
        assertEquals(3, s.tier)
        s = Engine.onCompleted(s, 5)   // third clean run: double step -> 5
        assertEquals(5, s.tier)
        // The run resets, so the next completion is a single step again.
        s = Engine.onCompleted(s, 5)
        assertEquals(6, s.tier)
    }

    @Test
    fun `one friction holds the tier instead of advancing it`() {
        val s = Engine.onFriction(PillarState(tier = 9))
        assertEquals("first friction must not move the tier", 9, s.tier)
        assertEquals(1, s.frictionAtTier)
    }

    @Test
    fun `two frictions at one tier step the user back down`() {
        var s = PillarState(tier = 9)
        s = Engine.onFriction(s)
        s = Engine.onFriction(s)
        assertEquals("the ladder must meet the user, not stall them", 8, s.tier)
        assertEquals("counter resets after stepping back", 0, s.frictionAtTier)
    }

    @Test
    fun `stepping back never goes below tier 1 and never erases cleared tiers`() {
        var s = PillarState(tier = 1, cleared = 7)
        repeat(10) { s = Engine.onFriction(s) }
        assertEquals(1, s.tier)
        assertEquals("history is never taken back", 7, s.cleared)
    }

    @Test
    fun `the ladder caps at tier 100`() {
        var s = PillarState(tier = 99)
        repeat(10) { s = Engine.onCompleted(s, 1) }
        assertEquals(Engine.MAX_TIER, s.tier)
    }

    @Test
    fun `friction resets a clean run so a double step must be re-earned`() {
        var s = PillarState(tier = 4)
        s = Engine.onCompleted(s, 5)
        s = Engine.onCompleted(s, 5)
        assertEquals(2, s.cleanRun)
        s = Engine.onFriction(s)
        assertEquals(0, s.cleanRun)
    }
}

class StreakTest {

    @Test
    fun `consecutive days build the streak`() {
        assertEquals(4, Engine.updateStreak(currentStreak = 3, lastActiveDay = 99, today = 100))
    }

    @Test
    fun `acting twice in one day does not double count`() {
        assertEquals(3, Engine.updateStreak(currentStreak = 3, lastActiveDay = 100, today = 100))
    }

    @Test
    fun `a missed day restarts the streak at one rather than zero`() {
        // Pausing must not read as a punishment: they showed up today, so today counts.
        assertEquals(1, Engine.updateStreak(currentStreak = 30, lastActiveDay = 90, today = 100))
    }
}

class ReadoutTest {

    @Test
    fun `radius grows with tier and stays inside the drawable range`() {
        var previous = 0f
        for (tier in 1..100) {
            val r = Engine.radius(tier)
            assertTrue("radius $r out of range at tier $tier", r in 0f..1f)
            assertTrue("radius must not shrink as tier rises", r >= previous)
            previous = r
        }
    }

    @Test
    fun `early tiers produce visible growth`() {
        // If tier 1 to 10 is imperceptible, nobody reaches tier 10.
        val growth = Engine.radius(10) - Engine.radius(1)
        assertTrue("early growth must be visible, was $growth", growth > 0.05f)
    }

    @Test
    fun `courage badge only ever climbs`() {
        val badges = (0..80).map { Engine.courageBadge(it) }
        assertEquals("Untested", badges[0])
        assertNotEquals(badges[0], badges[80])
        // Each distinct badge appears in one contiguous run, so it can never regress.
        val runs = badges.fold(mutableListOf<String>()) { acc, b ->
            if (acc.lastOrNull() != b) acc.add(b); acc
        }
        assertEquals("badges must not repeat out of order", runs.distinct().size, runs.size)
    }

    @Test
    fun `durations format across the whole ladder span`() {
        assertEquals("2 min", Engine.formatMinutes(2))
        assertEquals("1 hr", Engine.formatMinutes(60))
        assertEquals("1 day", Engine.formatMinutes(60 * 24))
        assertEquals("3 days", Engine.formatMinutes(60 * 24 * 3))
        assertTrue(Engine.formatMinutes(60 * 24 * 30).endsWith("wk"))
    }
}
