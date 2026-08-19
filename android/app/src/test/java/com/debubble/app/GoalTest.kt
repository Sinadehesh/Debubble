package com.debubble.app

import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalState
import com.debubble.app.engine.GoalTrack
import com.debubble.app.engine.Goals
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * The goal campaigns are authored data like the curriculum, and carry the same risk: a
 * careless edit could serve a step that references a principle that does not exist, or leave
 * a campaign with no way to log volume.
 */
class GoalCampaignTest {

    private val json = Json { ignoreUnknownKeys = true }

    private fun dir(): File = listOf(File("src/main/assets/goals"), File("app/src/main/assets/goals"))
        .firstOrNull { it.isDirectory }
        ?: error("cannot locate goal assets from ${File(".").absolutePath}")

    private fun track(goal: Goal): GoalTrack {
        val f = File(dir(), goal.asset.substringAfterLast('/'))
        assertTrue("missing ${f.path}", f.isFile)
        return json.decodeFromString<GoalTrack>(f.readText())
    }

    private fun all() = Goal.all.associateWith { track(it) }

    @Test
    fun `every goal has a full campaign`() {
        all().forEach { (goal, t) ->
            assertEquals("$goal mission count", Goals.CAMPAIGN_LENGTH, t.missions.size)
            t.missions.forEachIndexed { i, m -> assertEquals("$goal step index", i + 1, m.step) }
            assertEquals("$goal declares its own name", goal.name, t.goal)
            assertEquals("$goal phase count", 3, t.phases.size)
            assertTrue("$goal premise too thin", t.premise.length >= 80)
        }
    }

    @Test
    fun `every mission is complete and in range`() {
        all().forEach { (goal, t) ->
            t.missions.forEach { m ->
                assertTrue("$goal s${m.step} directive", m.directive.length >= 15)
                assertTrue("$goal s${m.step} coach", m.coach.length >= 15)
                assertTrue("$goal s${m.step} exposure ${m.exposure}", m.exposure in 1..10)
                assertTrue("$goal s${m.step} minutes ${m.minutes}", m.minutes >= 1)
                assertTrue("$goal s${m.step} repTarget ${m.repTarget}", m.repTarget in 0..10)
            }
        }
    }

    @Test
    fun `no directive repeats inside a campaign`() {
        all().forEach { (goal, t) ->
            val seen = mutableMapOf<String, Int>()
            t.missions.forEach { m ->
                val key = m.directive.lowercase().filter { it.isLetter() || it == ' ' }.trim()
                val prior = seen.put(key, m.step)
                assertTrue("$goal duplicate at s$prior / s${m.step}", prior == null)
            }
        }
    }

    /** A mission pointing at a principle that does not exist would crash the reader. */
    @Test
    fun `every principle reference resolves`() {
        all().forEach { (goal, t) ->
            t.missions.forEach { m ->
                if (m.principle >= 0) {
                    assertTrue(
                        "$goal s${m.step} references principle ${m.principle} of ${t.principles.size}",
                        m.principle < t.principles.size
                    )
                }
            }
        }
    }

    @Test
    fun `every principle is reachable and is surfaced by a mission`() {
        all().forEach { (goal, t) ->
            val referenced = t.missions.map { it.principle }.filter { it >= 0 }.toSet()
            t.principles.forEachIndexed { i, p ->
                assertTrue("$goal principle $i unlocks at ${p.unlocksAt}", p.unlocksAt in 1..Goals.CAMPAIGN_LENGTH)
                assertTrue("$goal principle $i body too thin", p.body.length >= 150)
                assertTrue("$goal principle '${p.title}' never surfaced", i in referenced)
            }
            assertEquals("$goal must have something readable on day one", 1, t.principles.first().unlocksAt)
            t.principles.zipWithNext().forEach { (a, b) ->
                assertTrue("$goal principle unlocks must ascend", b.unlocksAt > a.unlocksAt)
            }
        }
    }

    /** Reps are the unlimited half of the app; a campaign without them has no depth. */
    @Test
    fun `every campaign can log volume and can log a refusal`() {
        all().forEach { (goal, t) ->
            assertTrue("$goal has no rep types", t.reps.isNotEmpty())
            assertEquals("$goal duplicate rep keys", t.reps.size, t.reps.map { it.key }.distinct().size)
            assertTrue(
                "$goal has no friction rep — the anti-score would never grow from volume",
                t.reps.any { it.friction }
            )
            t.reps.forEach {
                assertTrue("$goal rep ${it.key} label", it.label.isNotBlank())
                assertTrue("$goal rep ${it.key} hint", it.hint.isNotBlank())
            }
            val volume = t.missions.sumOf { it.repTarget }
            assertTrue("$goal only asks for $volume reps across 30 steps", volume >= 25)
        }
    }

    @Test
    fun `campaigns escalate across their phases`() {
        all().forEach { (goal, t) ->
            val phases = (0 until 3).map { p ->
                t.missions.subList(p * 10, p * 10 + 10).map { it.exposure }.average()
            }
            phases.zipWithNext().forEachIndexed { i, (a, b) ->
                assertTrue("$goal exposure reverses at phase ${i + 2}", b >= a)
            }
            assertTrue("$goal does not escalate", phases.last() > phases.first())
        }
    }

    @Test
    fun `phase labels resolve for every step`() {
        all().forEach { (goal, t) ->
            (1..Goals.CAMPAIGN_LENGTH).forEach { step ->
                assertTrue("$goal step $step has no phase", t.phaseOf(step).isNotBlank())
            }
            assertNotEquals(t.phaseOf(1), t.phaseOf(30))
        }
    }

    @Test
    fun `goal metadata is distinct and points at a real asset`() {
        assertEquals(5, Goal.all.size)
        assertEquals(Goal.all.size, Goal.all.map { it.asset }.distinct().size)
        assertEquals(Goal.all.size, Goal.all.map { it.display }.distinct().size)
        Goal.all.forEach {
            assertTrue("${it.name} promise too thin", it.promise.length >= 40)
            assertEquals(it, Goal.from(it.name))
        }
        assertEquals(null, Goal.from("NOT_A_GOAL"))
        assertEquals(null, Goal.from(null))
    }
}

class GoalProgressionTest {

    @Test
    fun `a mission advances exactly one step`() {
        val s = Goals.onMissionCompleted(GoalState(step = 4))
        assertEquals("campaigns are a sequence, not a ladder — no double steps", 5, s.step)
        assertEquals(1, s.completed)
    }

    @Test
    fun `a campaign ends at thirty and stays there`() {
        var s = GoalState(step = 28)
        repeat(10) { s = Goals.onMissionCompleted(s) }
        assertEquals(Goals.CAMPAIGN_LENGTH, s.step)
        assertTrue(Goals.isComplete(s))
    }

    @Test
    fun `thirty completions is exactly one campaign`() {
        var s = GoalState()
        repeat(30) { s = Goals.onMissionCompleted(s) }
        assertTrue(Goals.isComplete(s))
        assertEquals(1f, Goals.progress(s), 0.001f)
    }

    @Test
    fun `reps are unlimited and never touch the campaign step`() {
        var s = GoalState(step = 7)
        repeat(250) { s = Goals.onRep(s) }
        assertEquals("reps must not advance the campaign", 7, s.step)
        assertEquals(250, s.repsLogged)
    }

    @Test
    fun `progress runs from zero to one without overshooting`() {
        var s = GoalState()
        var previous = -1f
        assertEquals("an untouched campaign reads as zero", 0f, Goals.progress(s), 0.001f)
        repeat(Goals.CAMPAIGN_LENGTH) {
            val p = Goals.progress(s)
            assertTrue("progress $p out of range", p in 0f..1f)
            assertTrue("progress must not regress", p > previous)
            previous = p
            s = Goals.onMissionCompleted(s)
        }
        // The step caps at 30, so progress must come from completions or a finished
        // campaign would show a bar stuck at 29/30 forever.
        assertEquals("a finished campaign must read as full", 1f, Goals.progress(s), 0.001f)
    }
}
