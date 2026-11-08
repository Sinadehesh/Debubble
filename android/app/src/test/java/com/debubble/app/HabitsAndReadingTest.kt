package com.debubble.app

import com.debubble.app.engine.Book
import com.debubble.app.engine.HabitKind
import com.debubble.app.engine.Habits
import com.debubble.app.engine.Mark
import com.debubble.app.engine.Reading
import com.debubble.app.engine.ReadingEntry
import com.debubble.app.engine.Training
import com.debubble.app.engine.WorkoutSet
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class HabitsAndReadingTest {

    private val today = 20_004L // a Monday? weekStart decides; the tests do not assume.

    /* ------------------------------------------------------------------ habits */

    @Test
    fun `total counts days, not marks, and never includes the future`() {
        val marks = listOf(Mark(today), Mark(today), Mark(today - 1), Mark(today + 1))
        val s = Habits.stats(HabitKind.BODY, marks, today)
        assertEquals(2, s.total)
        assertEquals(today - 1, s.first)
        assertEquals(today, s.last)
    }

    @Test
    fun `this week is Monday to Sunday of the current week`() {
        val monday = Training.weekStart(today)
        val marks = listOf(Mark(monday), Mark(monday + 2), Mark(monday - 1))
        val s = Habits.stats(HabitKind.ROOM, marks, monday + 3)
        assertEquals(listOf(true, false, true, false, false, false, false), s.week)
        assertEquals(2, s.weekCount)
    }

    @Test
    fun `the grid is twelve whole weeks ending with this one`() {
        val s = Habits.stats(HabitKind.HEART, listOf(Mark(today)), today)
        assertEquals(Habits.GRID_WEEKS * 7, s.grid.size)
        assertEquals(Training.weekStart(today) - (Habits.GRID_WEEKS - 1) * 7L, s.gridStart)
        assertTrue(s.grid[(today - s.gridStart).toInt()])
        assertEquals(1, s.grid.count { it })
    }

    @Test
    fun `best week is the most days in any one week`() {
        val monday = Training.weekStart(today) - 14
        val marks = (0..4).map { Mark(monday + it) } + listOf(Mark(today))
        assertEquals(5, Habits.stats(HabitKind.TRAINING, marks, today).bestWeek)
    }

    @Test
    fun `a usual time needs a few timed marks, and unknown times are ignored`() {
        val few = listOf(Mark(today, 8 * 60), Mark(today - 1, 9 * 60))
        assertNull(Habits.stats(HabitKind.BODY, few, today).usualMinute)
        val enough = few + Mark(today - 2, 8 * 60 + 30) + Mark(today - 3, -1)
        assertEquals(8 * 60 + 30, Habits.stats(HabitKind.BODY, enough, today).usualMinute)
    }

    @Test
    fun `clock and part of day read naturally`() {
        assertEquals("8:05 am", Habits.clock(8 * 60 + 5, h24 = false))
        assertEquals("12:00 pm", Habits.clock(12 * 60, h24 = false))
        assertEquals("20:15", Habits.clock(20 * 60 + 15, h24 = true))
        assertEquals("evening", Habits.partOfDay(19 * 60))
        assertEquals("morning", Habits.partOfDay(9 * 60))
    }

    @Test
    fun `no insights without enough days, and a pair shows up when two habits go together`() {
        assertTrue(Habits.insights(emptyMap(), emptyMap(), emptyList(), 0f, today, false).isEmpty())
        val days = (0 until 10).map { today - it }
        val marks = mapOf(
            HabitKind.BODY to days.map { Mark(it, 8 * 60) },
            HabitKind.HEART to days.take(8).map { Mark(it, 8 * 60 + 10) }
        )
        val out = Habits.insights(marks, emptyMap(), emptyList(), 0f, today, false)
        assertTrue(out.any { it.title.contains("go together") })
        assertTrue(out.any { it.title.contains("best day") })
    }

    @Test
    fun `the mood insight only appears when better is at least half`() {
        val worse = mapOf(HabitKind.HEART to Habits.Feelings(better = 1, same = 1, worse = 2))
        assertFalse(Habits.insights(emptyMap(), worse, emptyList(), 0f, today, false).any { it.title.endsWith("helps") })
        val better = mapOf(HabitKind.HEART to Habits.Feelings(better = 3, same = 1, worse = 0))
        assertTrue(Habits.insights(emptyMap(), better, emptyList(), 0f, today, false).any { it.title == "Heart helps" })
    }

    @Test
    fun `strength gains compare the first session with the best`() {
        val sets = listOf(
            WorkoutSet(1, "pushup", 3, 0f, today - 10),
            WorkoutSet(2, "pushup", 8, 0f, today - 1)
        )
        val out = Habits.insights(emptyMap(), emptyMap(), sets, 0f, today, false)
        assertTrue(out.any { it.title == "Push-ups: 3 to 8" })
    }

    /* ------------------------------------------------------------------ reading */

    private fun book(pages: Int = 300, page: Int = 0) = Book(id = 7, title = "A book", pages = pages, page = page, startedDay = today - 5)

    @Test
    fun `only forward pages count as read, and the last page finishes it`() {
        val (moved, entry) = Reading.readMore(book(page = 10), 15, today, 100, 600)
        assertEquals(25, moved.page)
        assertEquals(15, entry!!.pages)
        val (back, none) = Reading.turnTo(moved, 20, today, 101, 600)
        assertEquals(20, back.page)
        assertNull(none)
        val (done, _) = Reading.turnTo(back, 999, today, 102, 600)
        assertEquals(300, done.page)
        assertTrue(done.finished)
    }

    @Test
    fun `a book with no length never finishes by itself`() {
        val (moved, _) = Reading.readMore(book(pages = 0), 500, today, 1, 0)
        assertFalse(moved.finished)
        assertTrue(Reading.finish(moved, today).finished)
    }

    @Test
    fun `pace is the last fortnight spread over fourteen days, and gives days to finish`() {
        val log = listOf(
            ReadingEntry(1, 7, 70, today),
            ReadingEntry(2, 7, 70, today - 13),
            ReadingEntry(3, 7, 500, today - 30) // too long ago
        )
        assertEquals(10f, Reading.pace(log, today), 0.001f)
        assertEquals(15, Reading.daysToFinish(book(page = 150), 10f))
        assertNull(Reading.daysToFinish(book(pages = 0), 10f))
    }

    @Test
    fun `the shelf is finished books in the order they were finished`() {
        val books = listOf(
            Book(1, "Later", finishedDay = today),
            Book(2, "Reading", page = 3),
            Book(3, "First", finishedDay = today - 9)
        )
        assertEquals(listOf("First", "Later"), Reading.shelf(books).map { it.title })
        assertEquals(listOf("Reading"), Reading.reading(books, emptyList()).map { it.title })
    }
}
