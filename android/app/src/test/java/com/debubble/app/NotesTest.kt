package com.debubble.app

import com.debubble.app.data.Note
import com.debubble.app.data.NoteFilter
import com.debubble.app.data.Notes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Notes are the only place in the app where someone writes more than a sentence, which makes
 * them the only thing here that would genuinely hurt to lose or fail to find again. Search and
 * filtering are load-bearing, not conveniences.
 */
class NotesTest {

    private fun note(
        id: Long,
        body: String,
        tags: List<String> = emptyList(),
        logId: Long? = null,
        kind: String = "",
        lesson: String? = null
    ) = Note(
        id = id,
        body = body,
        epochDay = 100 + id,
        tags = tags,
        linkedLogId = logId,
        linkedTitle = if (logId != null) "Walk to the end of your street" else "",
        linkedKind = kind,
        linkedLessonId = lesson
    )

    private val corpus = listOf(
        note(1, "# Asked her out\n\nShe said no. I am still here."),
        note(2, "Rough day.\nCould not make myself leave.", tags = listOf("stuck"), logId = 9, kind = "FRICTION"),
        note(3, "Talked to someone in the queue.", tags = listOf("wins"), logId = 8, kind = "PILLAR"),
        note(4, "Notes on the fit lesson.", lesson = "fit_deep_dive", kind = "LESSON"),
        note(5, "")
    )

    // ------------------------------------------------------------------------ titles

    @Test
    fun `the title is the first real line with markdown stripped`() {
        assertEquals("Asked her out", corpus[0].title)
        assertEquals("Rough day.", corpus[1].title)
        assertEquals("Empty note", corpus[4].title)
    }

    @Test
    fun `the preview is the second real line, not a repeat of the first`() {
        assertEquals("She said no. I am still here.", corpus[0].preview)
        assertEquals("Could not make myself leave.", corpus[1].preview)
        assertEquals("", corpus[2].preview)
    }

    @Test
    fun `a very long first line is truncated rather than breaking the card`() {
        val long = note(9, "x".repeat(500))
        assertEquals(80, long.title.length)
    }

    // ------------------------------------------------------------------------ search

    @Test
    fun `search covers the body, the tags and the linked title`() {
        assertEquals(1, Notes.filter(corpus, "said no", NoteFilter.ALL).size)
        assertEquals(1, Notes.filter(corpus, "stuck", NoteFilter.ALL).size)
        // The linked challenge's own words are searchable, which is how someone finds
        // "everything I wrote about leaving the house".
        assertEquals(2, Notes.filter(corpus, "street", NoteFilter.ALL).size)
    }

    @Test
    fun `search ignores case and surrounding whitespace`() {
        assertEquals(
            Notes.filter(corpus, "ASKED", NoteFilter.ALL).size,
            Notes.filter(corpus, "  asked  ", NoteFilter.ALL).size
        )
        assertTrue(Notes.filter(corpus, "AsKeD", NoteFilter.ALL).isNotEmpty())
    }

    @Test
    fun `an empty query returns everything`() {
        assertEquals(corpus.size, Notes.filter(corpus, "", NoteFilter.ALL).size)
        assertEquals(corpus.size, Notes.filter(corpus, "   ", NoteFilter.ALL).size)
    }

    // ------------------------------------------------------------------------ filters

    @Test
    fun `filters partition the list without overlap or loss`() {
        val linked = Notes.filter(corpus, "", NoteFilter.LINKED)
        val free = Notes.filter(corpus, "", NoteFilter.FREE)
        assertEquals("linked and free must cover everything", corpus.size, linked.size + free.size)
        assertTrue(
            "a note cannot be both linked and free",
            linked.map { it.id }.intersect(free.map { it.id }.toSet()).isEmpty()
        )
    }

    @Test
    fun `the friction filter finds only friction notes`() {
        val friction = Notes.filter(corpus, "", NoteFilter.FRICTION)
        assertEquals(1, friction.size)
        assertEquals(2L, friction.first().id)
        assertTrue(friction.all { it.isFriction })
    }

    @Test
    fun `a lesson note counts as linked`() {
        val lesson = corpus.first { it.linkedLessonId != null }
        assertTrue(lesson.isLinked)
        assertFalse(lesson.isFriction)
        assertTrue(Notes.filter(corpus, "", NoteFilter.LINKED).any { it.id == lesson.id })
    }

    @Test
    fun `search and filter compose`() {
        val r = Notes.filter(corpus, "day", NoteFilter.FRICTION)
        assertEquals(1, r.size)
        assertEquals(0, Notes.filter(corpus, "day", NoteFilter.FREE).size)
    }

    // -------------------------------------------------------------------------- tags

    @Test
    fun `tags are normalised, de-duplicated and capped`() {
        val tags = Notes.cleanTags("Rejection, rejection,  #WINS , , Style!!")
        assertEquals(listOf("rejection", "wins", "style"), tags)
    }

    @Test
    fun `a runaway tag list is truncated rather than filling the card`() {
        val many = (1..20).joinToString(",") { "tag$it" }
        assertEquals(Notes.MAX_TAGS, Notes.cleanTags(many).size)
    }

    @Test
    fun `punctuation-only tags are dropped entirely`() {
        assertTrue(Notes.cleanTags("!!!, ###, ,,,").isEmpty())
        assertTrue(Notes.cleanTags("").isEmpty())
    }

    @Test
    fun `all tags are ranked by how often they are used`() {
        val notes = listOf(
            note(1, "a", tags = listOf("style", "fear")),
            note(2, "b", tags = listOf("style")),
            note(3, "c", tags = listOf("style", "fear")),
            note(4, "d", tags = listOf("wins"))
        )
        assertEquals(listOf("style", "fear", "wins"), Notes.allTags(notes))
    }

    // ------------------------------------------------------------------------ prompts

    /**
     * Friction gets a different question from a completion on purpose: the gap between what
     * someone expected and what happened is the most useful thing they can write after being
     * refused, and it is the thing they will not write unaided.
     */
    @Test
    fun `friction is prompted differently from everything else`() {
        val friction = Notes.promptFor("FRICTION")
        assertTrue(friction.contains("expect"))
        listOf("REP", "MISSION", "LESSON", "PILLAR", "").forEach {
            assertTrue("$it should have a prompt", Notes.promptFor(it).isNotBlank())
            assertTrue("$it must not reuse the friction prompt", Notes.promptFor(it) != friction)
        }
    }

    @Test
    fun `word count ignores whitespace runs`() {
        assertEquals(4, note(1, "one  two\n\nthree\tfour").wordCount)
        assertEquals(0, note(2, "   ").wordCount)
    }
}
