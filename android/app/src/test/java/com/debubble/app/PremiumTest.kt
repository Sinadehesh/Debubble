package com.debubble.app

import com.debubble.app.data.AppState
import com.debubble.app.engine.Entitlement
import com.debubble.app.engine.Offers
import com.debubble.app.engine.PlayAnswer
import com.debubble.app.engine.Premium
import com.debubble.app.engine.Plans
import com.debubble.app.engine.PremiumState
import com.debubble.app.engine.PricePhase
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PremiumTest {

    @Test
    fun `owned turns it on and keeps the original start date for the same purchase`() {
        val on = Entitlement.resolve(PremiumState(), PlayAnswer.Owned("tok", 1_000L), now = 5_000L)
        assertTrue(on.active)
        assertEquals(1_000L, on.since)
        val again = Entitlement.resolve(on, PlayAnswer.Owned("tok", 9_999L), now = 6_000L)
        assertEquals(1_000L, again.since)
        assertEquals(6_000L, again.checkedAt)
    }

    @Test
    fun `not owned turns it off, and unavailable changes nothing`() {
        val on = PremiumState(active = true, since = 1, checkedAt = 2, token = "tok")
        assertEquals(on, Entitlement.resolve(on, PlayAnswer.Unavailable, now = 3))
        val off = Entitlement.resolve(on, PlayAnswer.NotOwned, now = 4)
        assertFalse(off.active)
        assertEquals("", off.token)
    }

    @Test
    fun `the price line says the trial, the price and the period`() {
        val monthly = PricePhase("$4.99", 4_990_000, "P1M", 0, recurring = true)
        assertEquals("$4.99 a month", Offers.describe(listOf(monthly)))
        val trial = PricePhase("Free", 0, "P1W", 1, recurring = false)
        assertEquals("1 week free, then $4.99 a month", Offers.describe(listOf(trial, monthly)))
        assertTrue(Offers.hasFreeTrial(listOf(trial, monthly)))
        val intro = PricePhase("$0.99", 990_000, "P1M", 3, recurring = false)
        assertEquals("$0.99 a month for 3 months, then $4.99 a month", Offers.describe(listOf(intro, monthly)))
        assertFalse(Offers.hasFreeTrial(listOf(intro, monthly)))
    }

    @Test
    fun `yearly reads as a year and costs about half of paying monthly`() {
        val monthly = PricePhase("$4.99", 4_990_000, "P1M", 0, recurring = true)
        val yearly = PricePhase("$29.99", 29_990_000, "P1Y", 0, recurring = true)
        assertEquals("$29.99 a year", Offers.describe(listOf(yearly)))
        assertEquals("$29.99 a year", Offers.price(listOf(yearly)))
        assertEquals(50, Offers.yearlySaving(listOf(yearly), listOf(monthly)))
        assertEquals("About half the price of paying monthly", Offers.yearlyNote(listOf(yearly), listOf(monthly)))
        val dearer = PricePhase("$49.99", 49_990_000, "P1Y", 0, recurring = true)
        assertEquals("17% less than paying monthly", Offers.yearlyNote(listOf(dearer), listOf(monthly)))
        val notCheaper = PricePhase("$99", 99_000_000, "P1Y", 0, recurring = true)
        assertNull(Offers.yearlyNote(listOf(notCheaper), listOf(monthly)))
        assertNull(Offers.yearlyNote(listOf(yearly), emptyList()))
    }

    @Test
    fun `the price leaves out the free start, the line keeps it`() {
        val trial = PricePhase("Free", 0, "P1W", 1, recurring = false)
        val yearly = PricePhase("$29.99", 29_990_000, "P1Y", 0, recurring = true)
        assertEquals("1 week free, then $29.99 a year", Offers.describe(listOf(trial, yearly)))
        assertEquals("$29.99 a year", Offers.price(listOf(trial, yearly)))
    }

    @Test
    fun `before Play answers both plans show at the US price and neither can be bought`() {
        val c = Offers.choices(emptyMap())
        assertEquals(listOf(Premium.Plan.MONTHLY, Premium.Plan.YEARLY), c.map { it.plan })
        assertEquals(listOf("$4.99 a month", "$29.99 a year"), c.map { it.line })
        assertTrue(c.none { it.ready })
        assertEquals(Offers.MONTHLY_NOTE, c[0].note)
        assertEquals("About half the price of paying monthly.", c[1].note)
        assertEquals("$4.99 a month or $29.99 a year", Offers.summary(c))
    }

    @Test
    fun `once Play answers only the plans it offers are shown, at its prices`() {
        val trial = PricePhase("Free", 0, "P1W", 1, recurring = false)
        val monthly = PricePhase("4,99 €", 4_990_000, "P1M", 0, recurring = true)
        val only = Offers.choices(mapOf(Premium.Plan.MONTHLY to listOf(trial, monthly)))
        assertEquals(listOf(Premium.Plan.MONTHLY), only.map { it.plan })
        assertTrue(only.single().ready)
        assertTrue(only.single().trial)
        assertEquals("1 week free, then 4,99 € a month", only.single().line)
        assertEquals("4,99 € a month", Offers.summary(only))

        val yearly = PricePhase("29,99 €", 29_990_000, "P1Y", 0, recurring = true)
        val both = Offers.choices(mapOf(Premium.Plan.MONTHLY to listOf(monthly), Premium.Plan.YEARLY to listOf(yearly)))
        assertEquals("About half the price of paying monthly.", both[1].note)
        assertFalse(both[1].trial)
        assertEquals("4,99 € a month or 29,99 € a year", Offers.summary(both))
    }

    @Test
    fun `plans are found by their Play Console base plan ids`() {
        assertEquals(Premium.Plan.MONTHLY, Premium.Plan.of("monthly"))
        assertEquals(Premium.Plan.YEARLY, Premium.Plan.of("yearly"))
        assertNull(Premium.Plan.of("weekly"))
        assertEquals("year", Premium.Plan.YEARLY.every)
    }

    @Test
    fun `periods read in words`() {
        assertEquals("a month", Offers.period("P1M"))
        assertEquals("a year", Offers.period("P1Y"))
        assertEquals("7 days", Offers.period("P7D"))
        assertEquals("3 months", Offers.period("P3M", forLength = true))
    }

    @Test
    fun `a plan only counts while Premium is on`() {
        val s = AppState(plan = Plans.firstPushup.id)
        assertNull(s.activePlan)
        assertEquals(Plans.firstPushup, s.copy(premium = PremiumState(active = true)).activePlan)
    }

    @Test
    fun `state written before any of this still loads`() {
        val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
        val old = """{"onboarded":true,"xp":40}"""
        val s = json.decodeFromString<AppState>(old)
        assertFalse(s.isPremium)
        assertTrue(s.workouts.isEmpty())
        assertTrue(s.books.isEmpty())
        val round = json.decodeFromString<AppState>(json.encodeToString(s.copy(plan = "walk_it_off")))
        assertEquals("walk_it_off", round.plan)
    }
}
