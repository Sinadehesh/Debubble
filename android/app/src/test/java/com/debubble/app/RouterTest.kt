package com.debubble.app

import com.debubble.app.engine.AuditCatalogue
import com.debubble.app.engine.BudgetTier
import com.debubble.app.engine.CampaignPool
import com.debubble.app.engine.Goal
import com.debubble.app.engine.LearnCurriculum
import com.debubble.app.engine.RemediationPool
import com.debubble.app.engine.Router
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The routing engine, tested against the real shipping pools rather than fixtures.
 *
 * The pools are authored data, and the whole feature is a set of filters over them, so a test
 * against invented data would prove almost nothing. These read what actually ships.
 */
class RouterTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun assets(): File = listOf(File("src/main/assets"), File("app/src/main/assets"))
        .firstOrNull { it.isDirectory }
        ?: error("cannot locate assets from ${File(".").absolutePath}")

    private inline fun <reified T> load(path: String): T =
        json.decodeFromString<T>(File(assets(), path).readText())

    private val catalogue: AuditCatalogue by lazy { load("audit/debuffs.json") }
    private val remediation: RemediationPool by lazy { load("audit/remediation.json") }
    private val pool: CampaignPool by lazy { load("campaigns/pool.json") }
    private val learn: LearnCurriculum by lazy { load("learn/curriculum.json") }

    private val allGoals = Goal.all.map { it.name }.toSet()
    private val allDebuffs get() = catalogue.debuffs.map { it.id }.toSet()

    // ------------------------------------------------------------------- the catalogue

    @Test
    fun `the audit has the categories and volume the UI assumes`() {
        assertEquals(4, catalogue.categories.size)
        assertTrue("need at least 40 micro-debuffs", catalogue.debuffs.size >= 40)
        assertEquals(
            "ids must be unique",
            catalogue.debuffs.size,
            catalogue.debuffs.map { it.id }.distinct().size
        )
        catalogue.categories.forEach { c ->
            assertTrue(
                "${c.id} has no debuffs, so its tab renders empty",
                catalogue.inCategory(c.id).isNotEmpty()
            )
        }
    }

    /**
     * The load-bearing property of the whole feature: ticking anything must produce
     * something to do. A debuff with no remediation is a checkbox that does nothing.
     */
    @Test
    fun `every debuff has three rungs and a free first one`() {
        catalogue.debuffs.forEach { d ->
            val rungs = remediation.forDebuff(d.id)
            assertEquals("${d.id} should have three rungs", 3, rungs.size)
            assertEquals("${d.id} rungs must be ordered", listOf(1, 2, 3), rungs.map { it.rung })

            val first = rungs.first()
            assertEquals("${d.id} rung 1 must cost nothing", 0, first.cost)
            assertTrue(
                "${d.id} rung 1 is too exposed at ${first.exposure} to be a starting point",
                first.exposure <= 5
            )
            assertNotNull(
                "${d.id} must be servable on the free tier",
                remediation.next(d.id, emptySet(), BudgetTier.FREE)
            )
        }
    }

    @Test
    fun `remediation advances one rung at a time and never goes backwards`() {
        val id = catalogue.debuffs.first().id
        val rungs = remediation.forDebuff(id)
        var cleared = emptySet<String>()
        rungs.forEach { expected ->
            val next = remediation.next(id, cleared, BudgetTier.PREMIUM)
            assertEquals("wrong rung served", expected.id, next?.id)
            cleared = cleared + expected.id
        }
        // Everything cleared: it repeats the last rung rather than going silent.
        assertNotNull(remediation.next(id, cleared, BudgetTier.PREMIUM))
    }

    // ------------------------------------------------------------------------ budget

    @Test
    fun `the free tier never shows anything that costs money`() {
        pool.forActive(allGoals, BudgetTier.FREE).forEach {
            assertEquals("${it.id} costs money on the free tier", 0, it.cost)
            assertEquals("${it.id} is above tier 0", 0, it.tier)
        }
        allDebuffs.forEach { d ->
            val served = remediation.next(d, emptySet(), BudgetTier.FREE)
            assertEquals("$d served a paid rung on free", 0, served?.cost)
        }
    }

    @Test
    fun `each tier is a strict superset of the one below`() {
        val free = pool.forActive(allGoals, BudgetTier.FREE).map { it.id }.toSet()
        val cheap = pool.forActive(allGoals, BudgetTier.CHEAP).map { it.id }.toSet()
        val premium = pool.forActive(allGoals, BudgetTier.PREMIUM).map { it.id }.toSet()
        assertTrue("cheap must contain everything free has", cheap.containsAll(free))
        assertTrue("premium must contain everything cheap has", premium.containsAll(cheap))
        assertTrue("premium should open something up", premium.size > free.size)
    }

    @Test
    fun `nothing is ever served above the cash ceiling`() {
        BudgetTier.all.forEach { tier ->
            pool.forActive(allGoals, tier).forEach {
                assertTrue("${it.id} costs ${it.cost}, over ${tier.display}", it.cost <= tier.cap)
            }
        }
    }

    /** Someone on Free must be able to run the whole app indefinitely. */
    @Test
    fun `every campaign has free material`() {
        Goal.all.forEach { g ->
            val free = pool.forActive(setOf(g.name), BudgetTier.FREE)
            assertTrue("${g.name} has nothing free", free.isNotEmpty())
            assertTrue("${g.name} has too little free material", free.size >= 5)
        }
    }

    // -------------------------------------------------------------------- campaigns

    @Test
    fun `a bridge is never served to someone running only one campaign`() {
        Goal.all.forEach { g ->
            pool.forActive(setOf(g.name), BudgetTier.PREMIUM).forEach {
                assertFalse("${it.id} bridged into a single-campaign run", it.isBridge)
            }
        }
    }

    @Test
    fun `every pair of campaigns has at least one bridge`() {
        val names = Goal.all.map { it.name }
        names.forEachIndexed { i, a ->
            names.drop(i + 1).forEach { b ->
                val bridges = pool.bridges(setOf(a, b), BudgetTier.PREMIUM)
                assertTrue("no bridge exists for $a + $b", bridges.isNotEmpty())
                bridges.forEach {
                    assertEquals("a bridge must name exactly two campaigns", 2, it.goals.size)
                }
            }
        }
    }

    @Test
    fun `bridges surface regularly when two campaigns are running`() {
        val two = setOf(Goal.PARTNER.name, Goal.FRIENDS.name)
        val seen = (1L..30L).mapNotNull {
            Router.pickCampaign(it, two, BudgetTier.PREMIUM, pool, emptySet())
        }
        assertEquals("every day should produce a campaign challenge", 30, seen.size)
        val bridges = seen.count { it.isBridge }
        assertTrue("bridges never appeared", bridges > 0)
        // Roughly one in three, and never so many that the campaigns lose their identity.
        assertTrue("too many bridges: $bridges of 30", bridges <= 14)
    }

    @Test
    fun `both campaigns get turns rather than one starving the other`() {
        val two = setOf(Goal.PARTNER.name, Goal.CRAFT.name)
        val singles = (1L..30L)
            .mapNotNull { Router.pickCampaign(it, two, BudgetTier.PREMIUM, pool, emptySet()) }
            .filterNot { it.isBridge }
        assertTrue(
            "PARTNER never came up",
            singles.any { it.goals.first() == Goal.PARTNER.name }
        )
        assertTrue(
            "CRAFT never came up",
            singles.any { it.goals.first() == Goal.CRAFT.name }
        )
    }

    // ------------------------------------------------------------------- the router

    @Test
    fun `nothing is routed when nothing is configured`() {
        val r = Router.route(
            day = 4, activeGoals = emptySet(), debuffs = emptySet(), budget = BudgetTier.FREE,
            pool = pool, remediation = remediation,
            clearedRemediation = emptySet(), clearedCampaign = emptySet()
        )
        assertTrue(r.isEmpty)
        assertNull(r.campaign)
        assertNull(r.remediation)
    }

    /** The cap is the most important number in the router. Without it this is unusable. */
    @Test
    fun `a maximal setup still gets exactly two extra things`() {
        val r = Router.route(
            day = 7, activeGoals = allGoals, debuffs = allDebuffs, budget = BudgetTier.PREMIUM,
            pool = pool, remediation = remediation,
            clearedRemediation = emptySet(), clearedCampaign = emptySet()
        )
        assertNotNull("five campaigns and 48 debuffs produced no campaign", r.campaign)
        assertNotNull("five campaigns and 48 debuffs produced no remediation", r.remediation)
        val count = listOfNotNull(r.campaign, r.remediation).size
        assertEquals("the router must never exceed two items", 2, count)
    }

    @Test
    fun `routing is deterministic for a given day`() {
        repeat(20) { i ->
            val day = i.toLong() + 1
            val a = Router.route(
                day, allGoals, allDebuffs, BudgetTier.CHEAP, pool, remediation,
                emptySet(), emptySet()
            )
            val b = Router.route(
                day, allGoals, allDebuffs, BudgetTier.CHEAP, pool, remediation,
                emptySet(), emptySet()
            )
            assertEquals("day $day changed between calls", a.campaign?.id, b.campaign?.id)
            assertEquals("day $day changed between calls", a.remediation?.id, b.remediation?.id)
        }
    }

    @Test
    fun `remediation rotates across every marked debuff`() {
        val marked = catalogue.debuffs.take(5).map { it.id }.toSet()
        val served = (1L..25L).mapNotNull {
            Router.pickRemediation(it, marked, BudgetTier.PREMIUM, remediation, emptySet())
        }.map { it.debuff }.toSet()
        assertEquals("every marked debuff should come up within 25 days", marked, served)
    }

    @Test
    fun `an exhausted debuff yields the slot rather than emptying it`() {
        val marked = catalogue.debuffs.take(3).map { it.id }.toSet()
        // Clear everything for the first one.
        val cleared = remediation.forDebuff(marked.first()).map { it.id }.toSet()
        val served = (1L..12L).mapNotNull {
            Router.pickRemediation(it, marked, BudgetTier.PREMIUM, remediation, cleared)
        }
        assertEquals("the slot went empty on some days", 12, served.size)
    }

    @Test
    fun `a campaign with no free material still yields a challenge on the free tier`() {
        // Every day of a month on the tightest possible budget must produce something.
        Goal.all.forEach { g ->
            (1L..30L).forEach { day ->
                val picked = Router.pickCampaign(day, setOf(g.name), BudgetTier.FREE, pool, emptySet())
                assertNotNull("${g.name} produced nothing on day $day at Free", picked)
                assertEquals("${g.name} day $day served a paid challenge", 0, picked!!.cost)
            }
        }
    }

    @Test
    fun `the rationale explains a bridge whenever one is served`() {
        val two = setOf(Goal.PARTNER.name, Goal.FRIENDS.name)
        val bridgeDay = (1L..30L).first { day ->
            Router.pickCampaign(day, two, BudgetTier.PREMIUM, pool, emptySet())?.isBridge == true
        }
        val r = Router.route(
            bridgeDay, two, emptySet(), BudgetTier.PREMIUM, pool, remediation,
            emptySet(), emptySet()
        )
        assertTrue("a bridge should say so", r.rationale.contains("both"))
    }

    // ------------------------------------------------------------------------ Learn

    @Test
    fun `the course covers thirty days with no gaps and no duplicates`() {
        val days = learn.course.map { it.day }
        assertEquals((1..30).toList(), days.sorted())
        assertEquals("duplicate days", days.size, days.distinct().size)
    }

    @Test
    fun `course lessons unlock strictly by day`() {
        val day5 = learn.unlocked(5, emptySet())
        assertTrue(day5.all { it.day <= 5 })
        assertEquals(5, day5.size)
        assertEquals(30, learn.unlocked(30, emptySet()).size)
        assertEquals(30, learn.unlocked(9999, emptySet()).size)
    }

    @Test
    fun `targeted articles appear only for a marked debuff, and immediately`() {
        assertTrue(
            "targeted articles leaked in with nothing marked",
            learn.targetedFor(emptySet()).isEmpty()
        )
        val trigger = learn.targeted.first().debuffs.first()
        val shown = learn.targetedFor(setOf(trigger))
        assertTrue("marking $trigger showed nothing", shown.isNotEmpty())
        // Day 1 with a mark must already include it — no waiting.
        assertTrue(learn.unlocked(1, setOf(trigger)).any { it.isTargeted })
    }

    @Test
    fun `every targeted article points at debuffs that exist`() {
        val known = allDebuffs
        learn.targeted.forEach { l ->
            assertTrue("${l.id} has no triggers", l.debuffs.isNotEmpty())
            l.debuffs.forEach {
                assertTrue("${l.id} references unknown debuff '$it'", it in known)
            }
        }
    }

    /** Marking anything in a category should produce reading, or the tab looks broken. */
    @Test
    fun `every audit category can trigger at least one article`() {
        catalogue.categories.forEach { c ->
            val ids = catalogue.inCategory(c.id).map { it.id }.toSet()
            assertTrue(
                "nothing in '${c.id}' triggers any article",
                learn.targetedFor(ids).isNotEmpty()
            )
        }
    }

    @Test
    fun `every lesson has a body worth opening and an action at the end`() {
        learn.lessons.forEach { l ->
            assertTrue("${l.id} is too short", l.body.length > 700)
            assertTrue("${l.id} has no 'Do this'", l.body.contains("## Do this"))
            assertTrue("${l.id} has no title", l.title.isNotBlank())
            assertTrue("${l.id} has no summary", l.summary.isNotBlank())
            assertTrue("${l.id} has an implausible read time", l.minutes in 2..12)
        }
    }
}
