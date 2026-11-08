package com.debubble.app

import com.debubble.app.engine.Measure
import com.debubble.app.engine.MoveGroup
import com.debubble.app.engine.Moves
import com.debubble.app.engine.Place
import com.debubble.app.engine.Plans
import com.debubble.app.engine.Training
import com.debubble.app.engine.Week
import com.debubble.app.engine.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrainingTest {

    private val today = 20_000L
    private var id = 1L

    private fun set(ex: String, amount: Int, day: Long, kg: Float = 0f) =
        WorkoutSet(id++, ex, amount, kg, day, minute = 8 * 60)

    @Test
    fun `every ladder link points at a real exercise in the same group`() {
        Moves.all.forEach { ex ->
            listOfNotNull(ex.easier, ex.harder).forEach { other ->
                val o = Moves.byId(other)
                assertNotNull("${ex.id} points at $other", o)
                assertEquals("${ex.id} -> $other changes group", ex.group, o!!.group)
            }
            assertTrue("${ex.id} starts above its top", ex.start <= ex.top)
            assertTrue("${ex.id} has no cue", ex.cue.isNotBlank())
        }
        assertEquals("ids are unique", Moves.all.size, Moves.all.map { it.id }.distinct().size)
    }

    @Test
    fun `home suggestions never need kit`() {
        MoveGroup.entries.forEach { g ->
            assertEquals(com.debubble.app.engine.Gear.NONE, Moves.starter(g, Place.HOME).gear)
        }
        assertTrue(Moves.at(Place.HOME).all { it.gear == com.debubble.app.engine.Gear.NONE })
    }

    @Test
    fun `a first time starts at the start`() {
        val ex = Moves.byId("pushup")!!
        val t = Training.target(ex, emptyList(), today)
        assertEquals(ex.start, t.amount)
        assertEquals(ex, t.exercise)
    }

    @Test
    fun `the next target is one step past the best set last time`() {
        val sets = listOf(set("squat", 10, today - 2), set("squat", 12, today - 2), set("squat", 9, today - 2))
        val t = Training.target(Moves.byId("squat")!!, sets, today)
        assertEquals(13, t.amount)
    }

    @Test
    fun `today's own sets do not move the goal you are working on`() {
        val sets = listOf(set("squat", 10, today - 2), set("squat", 15, today))
        assertEquals(11, Training.target(Moves.byId("squat")!!, sets, today).amount)
    }

    @Test
    fun `at the top of a ladder rung you move up to the harder version`() {
        val wall = Moves.byId("wall_pushup")!!
        val sets = listOf(set("wall_pushup", wall.top, today - 1))
        val t = Training.target(wall, sets, today)
        assertEquals("incline_pushup", t.exercise.id)
        assertEquals(t.exercise.start, t.amount)
    }

    @Test
    fun `loaded exercises add weight at the top of the range and go back to the bottom`() {
        val press = Moves.byId("db_press")!!
        val atTop = listOf(set("db_press", press.top, today - 3, kg = 10f))
        val up = Training.target(press, atTop, today)
        assertEquals(10f + press.kgStep, up.kg, 0.001f)
        assertEquals(press.start, up.amount)

        val mid = listOf(set("db_press", 9, today - 3, kg = 10f))
        val same = Training.target(press, mid, today)
        assertEquals(10f, same.kg, 0.001f)
        assertEquals(10, same.amount)
    }

    @Test
    fun `after a long gap the target steps back instead of forward`() {
        val sets = listOf(set("squat", 18, today - Training.COMING_BACK_DAYS - 5))
        val t = Training.target(Moves.byId("squat")!!, sets, today)
        assertTrue("came back to ${t.amount}", t.amount < 18)
    }

    @Test
    fun `the snack goes to the group trained least recently`() {
        val sets = listOf(
            set("wall_pushup", 10, today - 1),
            set("squat", 10, today - 1),
            set("dead_bug", 8, today - 1)
        )
        // Pull has never been trained, so it is most due.
        assertEquals(MoveGroup.PULL, Training.snack(sets, today, Place.HOME).exercise.group)
    }

    @Test
    fun `a different one steps through the groups and every snack is one set`() {
        val seen = (0 until 4).map { Training.snack(emptyList(), today, Place.HOME, skip = it) }
        assertEquals(MoveGroup.strengthGroups.toSet(), seen.map { it.exercise.group }.toSet())
        assertTrue(seen.all { it.sets == 1 })
    }

    @Test
    fun `a new user does not get the same group every day`() {
        val firsts = (0 until 4).map { Training.snack(emptyList(), today + it, Place.HOME).exercise.group }.toSet()
        assertTrue("only $firsts", firsts.size > 1)
    }

    @Test
    fun `the session is three different strength groups`() {
        val session = Training.session(emptyList(), today, Place.GYM)
        assertEquals(3, session.size)
        assertEquals(3, session.map { it.exercise.group }.distinct().size)
        assertTrue(session.all { it.exercise.group.strength })
    }

    @Test
    fun `the week counts strength days and active minutes against the WHO numbers`() {
        val monday = Training.weekStart(today)
        val sets = listOf(
            set("pushup", 5, monday),
            set("squat", 10, monday),
            set("plank", 30, monday + 2),
            set("walk", 25, monday + 3),
            set("walk", 30, monday + 4),
            set("pushup", 5, monday - 1) // last week
        )
        val w = Training.week(sets, monday + 4)
        assertEquals(2, w.strengthDays)
        assertEquals(55, w.activeMinutes)
        assertEquals(listOf(true, false, true, true, true, false, false), w.days)
        assertEquals(2, Week.STRENGTH_DAYS)
    }

    @Test
    fun `weeks start on a Monday`() {
        // 1970-01-05 was a Monday: epoch day 4.
        assertEquals(4L, Training.weekStart(4L))
        assertEquals(4L, Training.weekStart(10L))
        assertEquals(11L, Training.weekStart(11L))
    }

    @Test
    fun `bests take the heaviest set for loaded exercises and the most reps otherwise`() {
        val sets = listOf(
            set("db_row", 12, today - 5, kg = 8f),
            set("db_row", 8, today - 1, kg = 12f),
            set("pushup", 6, today - 5),
            set("pushup", 9, today - 1)
        )
        val bests = Training.bests(sets).associateBy { it.exercise.id }
        assertEquals(12f, bests.getValue("db_row").kg, 0.001f)
        assertEquals(9, bests.getValue("pushup").amount)
    }

    @Test
    fun `labels read like a sentence`() {
        val t = Training.target(Moves.byId("knee_pushup")!!, emptyList(), today)
        assertEquals("5 knee push-ups", t.label)
        val plank = Training.target(Moves.byId("plank")!!, emptyList(), today)
        assertEquals(Measure.SECONDS, plank.exercise.measure)
        assertEquals("15 s plank", plank.label)
    }

    @Test
    fun `every plan names real exercises and finishes a session only when every set is in`() {
        Plans.all.forEach { p ->
            assertTrue(p.length > 0)
            p.sessions.flatMap { it.items }.forEach { item ->
                assertNotNull("${p.id}: ${item.exercise}", Moves.byId(item.exercise))
            }
        }
        val first = Plans.firstPushup.sessions.first()
        val item = first.items.first()
        val partial = (1 until item.sets).map { set(item.exercise, item.amount, today) }
        assertFalse(Plans.complete(first, partial, today))
        val full = partial + set(item.exercise, item.amount, today)
        assertTrue(Plans.complete(first, full, today))
        assertFalse("yesterday's sets do not finish today's session", Plans.complete(first, full, today + 1))
    }

    @Test
    fun `the push-up plan climbs the ladder in order`() {
        val order = Plans.firstPushup.sessions.map { it.items.first().exercise }.distinct()
        assertEquals(listOf("wall_pushup", "incline_pushup", "knee_pushup", "pushup"), order)
    }
}
