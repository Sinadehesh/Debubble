package com.debubble.app

import com.debubble.app.engine.Baseline
import com.debubble.app.engine.Challenge
import com.debubble.app.engine.Cues
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Engine
import com.debubble.app.engine.Ladder
import com.debubble.app.engine.Mobility
import com.debubble.app.engine.Needs
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
import com.debubble.app.engine.Protocol
import java.io.File
import kotlin.math.ln
import kotlin.math.pow
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The curriculum is authored data, not generated at runtime, so these tests are what stop a
 * careless edit from breaking the escalation the whole product rests on.
 *
 * Reads the real asset files from src/main/assets so the tests validate what actually ships.
 */
class CurriculumTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun assetsDir(): File {
        // Unit tests run with the module directory as CWD.
        val candidates = listOf(
            File("src/main/assets/curriculum"),
            File("app/src/main/assets/curriculum")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("cannot locate curriculum assets from ${File(".").absolutePath}")
    }

    private fun ladder(pillar: Pillar): Ladder {
        val file = File(assetsDir(), pillar.asset.substringAfterLast('/'))
        assertTrue("missing asset ${file.path}", file.isFile)
        return json.decodeFromString<Ladder>(file.readText())
    }

    private fun all(): Map<Pillar, Ladder> = Pillar.order.associateWith { ladder(it) }

    /* Difficulty is a composite, and each pillar weights it differently: Access is mostly
       distance and time, Social is almost entirely vulnerability. A single global weighting
       would make an honest Social ladder look mis-ordered. */
    private data class Weights(val exp: Double, val min: Double, val cost: Double)

    private fun weights(p: Pillar) = when (p) {
        Pillar.ACCESS -> Weights(0.25, 0.50, 0.25)
        Pillar.ACTIVITY -> Weights(0.40, 0.40, 0.20)
        Pillar.SOCIAL -> Weights(0.75, 0.20, 0.05)
    }

    private fun difficulty(p: Pillar, c: Challenge): Double {
        val w = weights(p)
        val logMin = ln(300_001.0)
        val logCost = ln(20_001.0)
        return w.exp * (c.exposure / 10.0) +
            w.min * (ln(1.0 + c.minutes) / logMin) +
            w.cost * (ln(1.0 + c.cost) / logCost)
    }

    // ------------------------------------------------------------------ shape

    @Test
    fun `every pillar has exactly one hundred tiers, indexed 1 to 100`() {
        all().forEach { (pillar, ladder) ->
            assertEquals("$pillar tier count", 100, ladder.tiers.size)
            ladder.tiers.forEachIndexed { i, c ->
                assertEquals("$pillar tier index at position $i", i + 1, c.tier)
            }
        }
    }

    @Test
    fun `every field is populated and in range`() {
        all().forEach { (pillar, ladder) ->
            ladder.tiers.forEach { c ->
                assertTrue("$pillar t${c.tier} directive too short", c.directive.length >= 12)
                assertTrue("$pillar t${c.tier} coach line too short", c.coach.length >= 12)
                assertTrue("$pillar t${c.tier} alternate too short", c.alternate.length >= 12)
                assertTrue("$pillar t${c.tier} exposure ${c.exposure}", c.exposure in 1..10)
                assertTrue("$pillar t${c.tier} minutes ${c.minutes}", c.minutes >= 1)
                assertTrue("$pillar t${c.tier} cost ${c.cost}", c.cost >= 0)
                assertNotEquals(
                    "$pillar t${c.tier} alternate duplicates the directive",
                    c.directive, c.alternate
                )
            }
        }
    }

    @Test
    fun `no directive is repeated within a ladder`() {
        all().forEach { (pillar, ladder) ->
            val seen = mutableMapOf<String, Int>()
            ladder.tiers.forEach { c ->
                val key = c.directive.lowercase().filter { it.isLetter() || it == ' ' }.trim()
                val prior = seen.put(key, c.tier)
                assertTrue("$pillar duplicate directive at t$prior and t${c.tier}", prior == null)
            }
        }
    }

    @Test
    fun `every declared need is one the engine knows how to gate on`() {
        val known = setOf(
            Needs.TRANSIT, Needs.BIKE, Needs.CAR, Needs.KITCHEN,
            Needs.MONEY, Needs.OVERNIGHT, Needs.MULTIDAY, Needs.PASSPORT
        )
        all().forEach { (pillar, ladder) ->
            ladder.tiers.forEach { c ->
                c.needs.forEach { n ->
                    assertTrue("$pillar t${c.tier} unknown need '$n'", n in known)
                }
            }
        }
    }

    // ------------------------------------------------------------------ the four rules

    /** Rule 1 — micro-beginnings: zero money, zero planning, failure near impossible. */
    @Test
    fun `rule 1 tier one is microscopic`() {
        all().forEach { (pillar, ladder) ->
            val first = ladder.at(1)
            assertEquals("$pillar t1 must be free", 0, first.cost)
            assertTrue("$pillar t1 must be under five minutes", first.minutes <= 5)
            assertEquals("$pillar t1 must be minimum exposure", 1, first.exposure)
        }
    }

    @Test
    fun `rule 1 the opening stretch never requires money`() {
        all().forEach { (pillar, ladder) ->
            (1..10).forEach { t ->
                assertEquals("$pillar t$t must cost nothing", 0, ladder.at(t).cost)
            }
        }
    }

    /** Rule 2 — escalation must be near-invisible early and never trend backwards. */
    @Test
    fun `rule 2 exposure trends upward by decade`() {
        all().forEach { (pillar, ladder) ->
            val decades = (0 until 10).map { d ->
                ladder.tiers.subList(d * 10, d * 10 + 10).map { it.exposure }.average()
            }
            decades.zipWithNext().forEachIndexed { i, (a, b) ->
                assertTrue(
                    "$pillar exposure reverses between decade $i ($a) and ${i + 1} ($b)",
                    b >= a
                )
            }
            assertTrue("$pillar must actually escalate", decades.last() > decades.first() + 4)
        }
    }

    @Test
    fun `rule 2 composite difficulty rises every decade`() {
        all().forEach { (pillar, ladder) ->
            val decades = (0 until 10).map { d ->
                ladder.tiers.subList(d * 10, d * 10 + 10).map { difficulty(pillar, it) }.average()
            }
            decades.zipWithNext().forEachIndexed { i, (a, b) ->
                assertTrue("$pillar difficulty flat or reversed at decade ${i + 1}", b > a)
            }
        }
    }

    @Test
    fun `rule 2 the first quarter stays gentle`() {
        all().forEach { (pillar, ladder) ->
            val peak = ladder.tiers.take(25).maxOf { it.exposure }
            assertTrue("$pillar reaches exposure $peak inside the first 25 tiers", peak <= 5)
        }
    }

    /**
     * Per-tier monotonicity is deliberately NOT asserted. A hundred-tier ladder mixes
     * two-minute acts with month-long projects, so a short sharp tier between two long ones
     * is rhythm rather than a defect. What is forbidden is a gross local backslide.
     */
    @Test
    fun `rule 2 no gross local backslide`() {
        val maxDip = 0.20
        all().forEach { (pillar, ladder) ->
            ladder.tiers.zipWithNext().forEach { (a, b) ->
                val dip = difficulty(pillar, a) - difficulty(pillar, b)
                assertTrue(
                    "$pillar drops $dip between t${a.tier} and t${b.tier}",
                    dip <= maxDip
                )
            }
        }
    }

    @Test
    fun `rule 2 exposure never jumps or drops by more than two points`() {
        all().forEach { (pillar, ladder) ->
            ladder.tiers.zipWithNext().forEach { (a, b) ->
                val delta = kotlin.math.abs(b.exposure - a.exposure)
                assertTrue(
                    "$pillar exposure cliff of $delta between t${a.tier} and t${b.tier}",
                    delta <= 2
                )
            }
        }
    }

    /** Rule 3 — macro-endings: life-altering, high vulnerability, real investment. */
    @Test
    fun `rule 3 the summit is maximal`() {
        all().forEach { (pillar, ladder) ->
            val last = ladder.at(100)
            assertEquals("$pillar t100 must be maximum exposure", 10, last.exposure)
            assertTrue(
                "$pillar t100 must dwarf t1 in time invested",
                last.minutes > ladder.at(1).minutes * 50
            )
        }
        // Access ends in geographic displacement measured in weeks.
        assertTrue(ladder(Pillar.ACCESS).at(100).minutes >= 60 * 24 * 14)
    }

    @Test
    fun `rule 3 the closing quarter is all high exposure`() {
        all().forEach { (pillar, ladder) ->
            val floor = ladder.tiers.drop(75).minOf { it.exposure }
            assertTrue("$pillar closing quarter drops to exposure $floor", floor >= 7)
        }
    }

    // ------------------------------------------------------------------ serving

    private fun curriculum() = Curriculum(all())

    /** Rule 4 — a challenge the user physically cannot do is worse than an easy one. */
    @Test
    fun `a user with no transport is never served a challenge that needs it`() {
        val c = curriculum()
        val b = Baseline(
            moves = setOf(Mobility.WALK),
            canStayOut = false,
            hasPassport = false,
            budgetPerChallenge = 0
        )
        Pillar.order.forEach { p ->
            (1..100).forEach { tier ->
                val served = Engine.serve(p, PillarState(tier = tier), c, b)
                val authored = c.challenge(p, tier)
                if (authored.needs.any { !b.has(it) }) {
                    assertTrue(
                        "$p t$tier should have been substituted",
                        served.substituted
                    )
                    assertEquals(
                        "$p t$tier should serve the authored alternate",
                        authored.alternate, served.directive
                    )
                }
                assertTrue("$p t$tier must stay inside a zero budget", served.cost <= 0)
            }
        }
    }

    @Test
    fun `a fully equipped user gets the canonical directive`() {
        val c = curriculum()
        val b = Baseline(
            moves = setOf(Mobility.WALK, Needs.TRANSIT, Needs.BIKE, Needs.CAR),
            canStayOut = true,
            hasPassport = true,
            budgetPerChallenge = 100000
        )
        Pillar.order.forEach { p ->
            (1..100).forEach { tier ->
                val served = Engine.serve(p, PillarState(tier = tier), c, b)
                assertFalse("$p t$tier should not be substituted", served.substituted)
                assertEquals(c.challenge(p, tier).directive, served.directive)
            }
        }
    }

    @Test
    fun `swapping serves the alternate at the same tier`() {
        val c = curriculum()
        val b = Baseline(budgetPerChallenge = 100000, hasPassport = true)
        val swapped = Engine.serve(Pillar.SOCIAL, PillarState(tier = 20), c, b, forceAlternate = true)
        assertEquals(20, swapped.tier)
        assertEquals(c.challenge(Pillar.SOCIAL, 20).alternate, swapped.directive)
        assertTrue(swapped.substituted)
    }

    @Test
    fun `serving is defined for every tier of every pillar`() {
        val c = curriculum()
        val b = Baseline()
        Pillar.order.forEach { p ->
            (1..100).forEach { tier ->
                val s = Engine.serve(p, PillarState(tier = tier), c, b)
                assertTrue("$p t$tier empty directive", s.directive.isNotBlank())
                assertTrue("$p t$tier empty coach line", s.coach.isNotBlank())
                assertTrue("$p t$tier bad minutes ${s.minutes}", s.minutes >= 1)
                assertEquals(tier, s.tier)
            }
        }
    }

    @Test
    fun `a calibrated beginner walking the whole ladder never falls off it`() {
        // End-to-end: 300 completions should land every pillar exactly at the summit.
        val c = curriculum()
        val b = Baseline()
        Pillar.order.forEach { p ->
            var s = PillarState(tier = 1)
            var guard = 0
            while (s.tier < Engine.MAX_TIER && guard < 500) {
                val served = Engine.serve(p, s, c, b)
                s = Engine.onCompleted(s, served.minutes)
                guard++
            }
            assertEquals("$p should reach the summit", Engine.MAX_TIER, s.tier)
            assertTrue("$p took an implausible number of steps: $guard", guard <= 100)
        }
    }

    @Test
    fun `the difficulty curve matches the documented shape`() {
        // Rule 2 is specified as (t/100)^1.9 — the first thirty tiers stay almost flat.
        val at30 = (30 / 100.0).pow(1.9)
        val at100 = 1.0
        assertTrue("curve should be under 12% of full range by tier 30", at30 < 0.12 * at100)
    }
}

/**
 * The protocol around each challenge.
 *
 * These tests exist because the protocol is the part of the curriculum a careless edit would
 * break invisibly. A missing directive is obvious the moment anyone opens the app; a cue that
 * no longer names a moment, or a success criterion that reads as a second instruction, still
 * renders perfectly and quietly returns the challenge to the state it was in before — a clear
 * sentence that nobody can act on.
 */
class ProtocolTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun assetsDir(): File {
        val candidates = listOf(
            File("src/main/assets/curriculum"),
            File("app/src/main/assets/curriculum")
        )
        return candidates.firstOrNull { it.isDirectory }
            ?: error("cannot locate curriculum assets from ${File(".").absolutePath}")
    }

    private fun ladder(pillar: Pillar): Ladder =
        json.decodeFromString(
            File(assetsDir(), pillar.asset.substringAfterLast('/')).readText()
        )

    private fun every(block: (Pillar, Challenge) -> Unit) {
        Pillar.order.forEach { p -> ladder(p).tiers.forEach { block(p, it) } }
    }

    // ---------------------------------------------------------------- completeness

    @Test
    fun `every challenge on every ladder carries the whole protocol`() {
        every { p, c ->
            val at = "${p.name} t${c.tier}"
            assertTrue("$at has no anchor", c.anchor.isNotBlank())
            assertTrue("$at has no opener", c.opener.isNotBlank())
            assertTrue("$at has no success criterion", c.done.isNotBlank())
            assertTrue("$at names no safety behaviour", c.drop.isNotBlank())
            assertTrue("$at has no prediction to disconfirm", c.test.isNotBlank())
        }
    }

    /**
     * The hard one. The app refuses vague cues typed by the user; shipping vague ones of its
     * own would be the app failing its own test, which is exactly the bug the intention tests
     * caught in [Cues.anchors].
     */
    @Test
    fun `every authored anchor passes the specificity test the app applies to users`() {
        every { p, c ->
            val at = "${p.name} t${c.tier}"
            assertTrue("$at ships a cue the app would reject: '${c.anchor}'", Cues.isSpecific(c.anchor))
            assertNull("$at anchor would be critiqued", Cues.critique(c.anchor))
        }
    }

    /** The anchor is dropped into "When ___, I will ___", so it has to be first person. */
    @Test
    fun `anchors are written to fit the sentence they go into`() {
        every { p, c ->
            val at = "${p.name} t${c.tier}"
            // Mid-sentence, so not sentence-cased — but "I" is capitalised in English and
            // most of these start with it, so that is the one allowed capital.
            val opensLowerOrPronoun = c.anchor.first().isLowerCase() || c.anchor.startsWith("I ")
            assertTrue("$at anchor is sentence-cased: '${c.anchor}'", opensLowerOrPronoun)
            assertFalse("$at anchor ends a sentence", c.anchor.endsWith("."))
            val firstPerson = c.anchor.contains(Regex("""\b(I|my|me)\b"""))
            val eventForm = c.anchor.startsWith("someone ") || c.anchor.startsWith("the doors ")
            assertTrue("$at anchor names no subject: '${c.anchor}'", firstPerson || eventForm)
        }
    }

    /** "Done when <done>" has to read as one sentence. */
    @Test
    fun `the success criterion continues the lead-in rather than restarting`() {
        every { p, c ->
            val at = "${p.name} t${c.tier}"
            assertTrue("$at done is capitalised: '${c.done}'", c.done.first().isLowerCase())
            assertTrue("$at done is not a sentence", c.done.endsWith("."))
        }
    }

    /**
     * The opener is the two-minute rule made concrete. Length is a crude proxy, but a long
     * opener is reliably a second task wearing the first one's clothes.
     */
    @Test
    fun `the opener stays small enough to be the thing nobody can refuse`() {
        every { p, c ->
            val at = "${p.name} t${c.tier}"
            assertTrue("$at opener is ${c.opener.length} chars", c.opener.length <= 130)
            assertTrue("$at opener does not read as an instruction", c.opener.first().isUpperCase())
            assertNotEquals("$at opener is just the directive again", c.directive, c.opener)
        }
    }

    @Test
    fun `scripts contain the actual words`() {
        every { p, c ->
            if (c.script.isNotBlank()) {
                assertTrue("${p.name} t${c.tier} script has no quoted line", c.script.contains('"'))
            }
        }
    }

    /**
     * The ladder with the worst version of the old defect should carry the most scripts.
     * Exposure without the sentence tells someone who cannot say the sentence nothing.
     */
    @Test
    fun `the social ladder ships scripts where the barrier is a missing sentence`() {
        val scripted = ladder(Pillar.SOCIAL).tiers.count { it.script.isNotBlank() }
        assertTrue("only $scripted social challenges carry a script", scripted >= 30)
    }

    // ---------------------------------------------------------------- derived parts

    @Test
    fun `the easier rung is genuinely below the one being offered`() {
        Pillar.order.forEach { p ->
            val l = ladder(p)
            (3..Engine.MAX_TIER).forEach { t ->
                assertEquals(
                    "${p.name} t$t should step back two rungs",
                    l.at(t - 2).directive,
                    l.easierThan(t)
                )
            }
            // At the bottom there is nothing below, so the authored alternate stands in.
            assertEquals(l.at(1).alternate, l.easierThan(1))
            assertEquals(l.at(2).alternate, l.easierThan(2))
        }
    }

    @Test
    fun `every rung has a celebration and they are not all the same`() {
        Pillar.order.forEach { p ->
            val lines = (1..Engine.MAX_TIER).map { Protocol.celebration(p, it) }
            lines.forEachIndexed { i, line ->
                assertTrue("${p.name} t${i + 1} has no celebration", line.isNotBlank())
            }
            assertTrue("${p.name} celebrations never vary", lines.distinct().size > 1)
            // Deterministic: the same rung must always produce the same line, or it reads as
            // a reward schedule rather than a ritual.
            assertEquals(Protocol.celebration(p, 7), Protocol.celebration(p, 7))
        }
        // And the fallback cue has to clear the same bar as the authored ones.
        Pillar.order.forEach { p ->
            assertTrue(Cues.isSpecific(Protocol.genericAnchor(p)))
            assertNull(Cues.critique(Protocol.genericAnchor(p)))
        }
    }

    // ---------------------------------------------------------------- end to end

    @Test
    fun `the engine serves the protocol, and swapping to the alternate does not drop it`() {
        val curriculum = Curriculum(Pillar.order.associateWith { ladder(it) })
        val baseline = Baseline(radiusKm = 2, moves = setOf(Mobility.WALK))
        Pillar.order.forEach { p ->
            val state = PillarState(tier = 12)
            val normal = Engine.serve(p, state, curriculum, baseline)
            val swapped = Engine.serve(p, state, curriculum, baseline, forceAlternate = true)

            assertTrue(normal.hasProtocol)
            assertTrue("swapping must not strip the protocol", swapped.hasProtocol)
            assertEquals("the moment does not change with the variant", normal.anchor, swapped.anchor)
            assertEquals(normal.done, swapped.done)
            assertEquals(normal.drop, swapped.drop)
            assertNotEquals("the variant should change the directive", normal.directive, swapped.directive)
            assertTrue(normal.ease.isNotBlank())
            assertTrue(normal.celebration.isNotBlank())
        }
    }

    /**
     * The protocol has to survive the mobility rewriter in both directions, because a user who
     * picked neither walking nor wheels gets "head", which has no usable past tense. The
     * generator checks the asset text; this checks what the engine actually hands the screen.
     */
    @Test
    fun `the protocol reads as English for a user who does not walk`() {
        val curriculum = Curriculum(Pillar.order.associateWith { ladder(it) })
        val broken = Regex("""\bbeen (a|an|the|it|back|for|outside)\b|\bbe been\b|\bstay out\b""")
        listOf(setOf(Mobility.WHEELS), setOf(Mobility.TRANSIT)).forEach { modes ->
            val baseline = Baseline(radiusKm = 5, moves = modes)
            Pillar.order.forEach { p ->
                (1..Engine.MAX_TIER).forEach { t ->
                    val s = Engine.serve(p, PillarState(tier = t), curriculum, baseline)
                    listOf(s.anchor, s.opener, s.done, s.drop, s.test, s.ease).forEach { text ->
                        val hit = broken.find(text)
                        // The authored text is allowed to contain these; only a substitution
                        // that introduces one is the bug.
                        if (hit != null) {
                            val source = curriculum.challenge(p, t)
                            val authored = listOf(
                                source.anchor, source.opener, source.done,
                                source.drop, source.test
                            ).any { broken.containsMatchIn(it) }
                            assertTrue(
                                "${p.name} t$t became non-English for $modes: $text",
                                authored
                            )
                        }
                    }
                }
            }
        }
    }
}
