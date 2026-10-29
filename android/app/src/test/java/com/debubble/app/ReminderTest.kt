package com.debubble.app

import com.debubble.app.data.AppState
import com.debubble.app.data.LogEntry
import com.debubble.app.data.quests
import com.debubble.app.data.stillToDo
import com.debubble.app.engine.Baseline
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.CareState
import com.debubble.app.engine.Curriculum
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Ladder
import com.debubble.app.engine.Mobility
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
import com.debubble.app.engine.QuestKind
import com.debubble.app.engine.Reminder
import com.debubble.app.engine.Reminders
import com.debubble.app.engine.Today
import com.debubble.app.widget.WidgetCues
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Reminders: the bell beside "I did it". The times have to be right, the labels have to say
 * the right day, and a reminder must never nag about something already done.
 */
class ReminderTest {

    private val day = LocalDate.of(2026, 10, 29)
    private val today = day.toEpochDay()
    private fun at(h: Int, m: Int = 0) = day.atTime(h, m)

    private fun once(link: String = "care:BODY", minute: Int = 19 * 60, on: Long = today) =
        Reminder(link = link, title = "Wash up", text = "Brush your teeth", minute = minute, day = on)

    private fun daily(link: String = "care:BODY", minute: Int = 8 * 60) =
        Reminder(link = link, title = "Wash up", text = "Brush your teeth", minute = minute, daily = true)

    // ------------------------------------------------------------------ when it goes off

    @Test
    fun `a one-off goes off once, at its time, and is gone after`() {
        assertEquals(at(19), Reminders.next(once(), at(9)))
        assertNull(Reminders.next(once(), at(19, 1)))
        assertEquals(at(19).plusDays(1), Reminders.next(once(on = today + 1), at(23)))
    }

    @Test
    fun `a daily one goes off today if it still can, otherwise tomorrow`() {
        assertEquals(at(8), Reminders.next(daily(), at(7, 59)))
        assertEquals(at(8).plusDays(1), Reminders.next(daily(), at(8)))
        assertEquals(at(8).plusDays(1), Reminders.next(daily(), at(22)))
    }

    @Test
    fun `labels say the day the way a person would`() {
        assertEquals("Today 19:00", Reminders.label(once(), today))
        assertEquals("Tomorrow 19:00", Reminders.label(once(on = today + 1), today))
        assertEquals("Every day 08:00", Reminders.label(daily(), today))
        assertEquals("19:00", Reminders.short(once(), today))
        assertEquals("08:00", Reminders.short(daily(), today))
        assertEquals("7:00 pm", Reminders.clock(19 * 60, h24 = false))
        assertEquals("12:30 pm", Reminders.clock(12 * 60 + 30, h24 = false))
        assertEquals("12:05 am", Reminders.clock(5, h24 = false))
        assertTrue(Reminders.short(once(on = today + 3), today).endsWith("19:00"))
    }

    // ------------------------------------------------------------------ the quick choices

    @Test
    fun `there are always four quick times, in order, all still ahead`() {
        listOf(at(6, 40), at(9), at(12, 20), at(14, 55), at(18, 50), at(21, 20), at(23, 30)).forEach { now ->
            val choices = Reminders.choices(now)
            assertTrue("$now: ${choices.map { it.label }}", choices.size in 2..4)
            val times = choices.map { LocalDate.ofEpochDay(it.day).atTime(it.minute / 60, it.minute % 60) }
            assertEquals("in order at $now", times.sorted(), times)
            assertTrue("all ahead at $now", times.all { it.isAfter(now) })
            assertEquals("ends on tomorrow morning", today + 1, choices.last().day)
            assertTrue(choices.first().label.startsWith("In an hour"))
        }
        // Late at night, "in an hour" crosses midnight onto tomorrow.
        val late = Reminders.choices(at(23, 30)).first()
        assertEquals(today + 1, late.day)
        assertEquals(30, late.minute)
    }

    @Test
    fun `named times are only offered while there is still time before them`() {
        val labels = Reminders.choices(at(18, 50)).map { it.label }
        assertFalse(labels.any { it.startsWith("This evening") })
        assertTrue(labels.any { it.startsWith("Before bed") })
        val morning = Reminders.choices(at(9)).map { it.label }
        assertTrue(morning.any { it.startsWith("Lunchtime") })
    }

    // ------------------------------------------------------------------ keeping the list right

    @Test
    fun `setting a reminder for the same thing replaces it`() {
        val list = Reminders.upsert(Reminders.upsert(emptyList(), once()), once(minute = 20 * 60))
        assertEquals(1, list.size)
        assertEquals(20 * 60, list.single().minute)
    }

    @Test
    fun `gone-off one-offs are dropped, daily ones are kept`() {
        val list = listOf(once(minute = 9 * 60), once(link = "care:ROOM"), daily(link = "care:HEART"))
        assertEquals(listOf("care:ROOM", "care:HEART"), Reminders.live(list, at(12)).map { it.link })
    }

    @Test
    fun `doing a thing clears its one-offs, keeps its daily, and clears that whole ladder`() {
        val list = listOf(
            once(link = "care:BODY"),
            once(link = "care:BODY#later"),
            daily(link = "care:BODY"),
            once(link = "tier:SOCIAL:4"),
            once(link = "tier:SOCIAL:3"),
            once(link = "${Reminders.OUTSIDE}#later"),
            daily(link = Reminders.OUTSIDE),
            once(link = "tier:ACCESS:2")
        )
        val afterCare = Reminders.afterDone(list, "care:BODY").map { it.link to it.daily }
        assertFalse(("care:BODY" to false) in afterCare)
        assertFalse(("care:BODY#later" to false) in afterCare)
        assertTrue(("care:BODY" to true) in afterCare)

        val afterSocial = Reminders.afterDone(list, "tier:SOCIAL:4").map { it.link to it.daily }
        assertFalse(afterSocial.any { it.first.startsWith("tier:SOCIAL") })
        assertFalse(("${Reminders.OUTSIDE}#later" to false) in afterSocial)
        assertTrue((Reminders.OUTSIDE to true) in afterSocial)
        assertTrue(("tier:ACCESS:2" to false) in afterSocial)
    }

    @Test
    fun `a snoozed daily one opens the same thing`() {
        assertEquals("care:BODY", Reminders.base("care:BODY#later"))
        assertEquals("tier:SOCIAL:4", Reminders.base("tier:SOCIAL:4"))
    }

    // ------------------------------------------------------------------ against today's four

    private val json = Json { ignoreUnknownKeys = true }
    private fun asset(path: String): File =
        listOf(File("src/main/assets/$path"), File("app/src/main/assets/$path")).first { it.exists() }
    private val curriculum: Curriculum by lazy {
        Curriculum(Pillar.order.associateWith { json.decodeFromString<Ladder>(asset(it.asset).readText()) })
    }
    private val library: CareLibrary by lazy {
        json.decodeFromString<CareLibrary>(asset("care/routines.json").readText())
    }

    private fun state(
        reminders: List<Reminder> = emptyList(),
        care: Map<Domain, CareState> = emptyMap(),
        done: Set<Pillar> = emptySet(),
        log: List<LogEntry> = emptyList()
    ) = AppState(
        onboarded = true,
        movedIn = true,
        startedDay = today,
        baseline = Baseline(moves = setOf(Mobility.WALK)),
        pillars = Pillar.order.associate { it.name to PillarState(tier = 1) },
        servedDay = today,
        doneToday = done.map { it.name }.toSet(),
        care = care.mapKeys { it.key.name },
        reminders = reminders,
        log = log
    )

    @Test
    fun `today's mission finds its own reminder or the every-day one`() {
        val s = state()
        val out = s.quests(today, library, curriculum).first { it.kind == QuestKind.OUTSIDE }
        assertNull(Reminders.forQuest(emptyList(), out))
        assertNotNull(Reminders.forQuest(listOf(daily(link = Reminders.OUTSIDE)), out))
        val own = once(link = out.link)
        assertEquals(own, Reminders.forQuest(listOf(daily(link = Reminders.OUTSIDE), own), out))
    }

    @Test
    fun `nothing nags about something already done today`() {
        val washed = mapOf(Domain.BODY to CareState(setDay = today, lastDoneDay = today, reps = 1))
        assertFalse(state(care = washed).stillToDo("care:BODY", today))
        assertTrue(state().stillToDo("care:BODY", today))
        val s = state()
        val p = Today.outsidePillar(1, emptySet())
        assertTrue(s.stillToDo(Reminders.OUTSIDE, today))
        assertTrue(s.stillToDo("tier:${p.name}:1", today))
        val did = LogEntry(id = 1, pillar = p.name, tier = 1, title = "Stand outside", epochDay = today)
        val after = state(done = setOf(p), log = listOf(did))
        assertFalse(after.stillToDo(Reminders.OUTSIDE, today))
        assertFalse(after.stillToDo("tier:${p.name}:1", today))
        assertFalse("an unknown link never nags", s.stillToDo("nonsense", today))
    }

    @Test
    fun `the widget leads with the time you picked`() {
        val s = state(reminders = listOf(once(link = "care:ROOM", minute = 18 * 60 + 30)))
        val cue = WidgetCues.of(s, today, s.quests(today, library, curriculum))
        val room = cue.rows.first { it.link == "care:ROOM" }
        assertTrue(room.text, room.text.contains("18:30 · When "))
        cue.rows.filter { it.link != "care:ROOM" }.forEach { assertTrue(it.text.startsWith("When ")) }
        val twelve = WidgetCues.of(s, today, s.quests(today, library, curriculum), h24 = false)
        assertTrue(twelve.rows.first { it.link == "care:ROOM" }.text.contains("6:30 pm"))
    }

    @Test
    fun `every quick time builds a reminder that goes off when it says`() {
        val now: LocalDateTime = at(10, 7)
        Reminders.choices(now).forEach { c ->
            val r = once(minute = c.minute, on = c.day)
            val next = Reminders.next(r, now)
            assertNotNull(c.label, next)
            assertTrue(c.label, c.label.endsWith(Reminders.clock(c.minute)))
        }
    }
}
