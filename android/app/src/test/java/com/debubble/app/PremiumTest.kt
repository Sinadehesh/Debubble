package com.debubble.app

import com.debubble.app.data.AppState
import com.debubble.app.engine.Entitlement
import com.debubble.app.engine.Offers
import com.debubble.app.engine.PlayAnswer
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
