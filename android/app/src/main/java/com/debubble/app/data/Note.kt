package com.debubble.app.data

import kotlinx.serialization.Serializable

/**
 * One journal entry.
 *
 * Notes are the only place in the app where the user writes more than a sentence, and they
 * are the thing most likely to still matter in a year. Everything else here is a count.
 *
 * A note can stand alone or be attached to something that happened — a completed challenge,
 * a logged friction event, a lesson. The link is what turns a pile of entries into a record
 * you can actually search: "everything I wrote after being turned down" is a question worth
 * being able to ask yourself on day ninety.
 */
@Serializable
data class Note(
    val id: Long,
    /** Markdown. Rendered on the list as plain text and in the reader properly. */
    val body: String,
    val epochDay: Long,
    /** Free-text tags the user typed. Lowercased on the way in. */
    val tags: List<String> = emptyList(),
    /** Id of the log entry this was written about, if any. */
    val linkedLogId: Long? = null,
    /** Denormalised so the list can show the link without walking the log. */
    val linkedTitle: String = "",
    /** "PILLAR" | "MISSION" | "REP" | "FRICTION" | "LESSON" | "" */
    val linkedKind: String = "",
    /** Id of the lesson this was written about, if any. */
    val linkedLessonId: String? = null
) {
    val isLinked: Boolean get() = linkedLogId != null || linkedLessonId != null

    val isFriction: Boolean get() = linkedKind == "FRICTION"

    /** First line, for the list. Markdown heading marks are stripped. */
    val title: String
        get() = body.lineSequence()
            .firstOrNull { it.isNotBlank() }
            ?.trimStart('#', ' ', '>', '-', '*')
            ?.trim()
            ?.take(80)
            ?: "Empty note"

    val preview: String
        get() = body.lineSequence()
            .filter { it.isNotBlank() }
            .drop(1)
            .firstOrNull()
            ?.trimStart('#', ' ', '>', '-', '*')
            ?.trim()
            ?.take(120)
            ?: ""

    val wordCount: Int get() = body.split(Regex("\\s+")).count { it.isNotBlank() }

    fun matches(query: String): Boolean {
        if (query.isBlank()) return true
        val q = query.trim().lowercase()
        return body.lowercase().contains(q) ||
            linkedTitle.lowercase().contains(q) ||
            tags.any { it.contains(q) }
    }
}

/** How the Notes list is being narrowed. */
enum class NoteFilter(val display: String) {
    ALL("All"),
    LINKED("On challenges"),
    FRICTION("On friction"),
    FREE("Free writing")
}

object Notes {

    const val MAX_TAGS = 6

    /** Tags are lowercased, trimmed, de-duplicated and capped. */
    fun cleanTags(raw: String): List<String> =
        raw.split(',', '#')
            .map { it.trim().lowercase().replace(Regex("[^a-z0-9 -]"), "") }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_TAGS)

    fun filter(notes: List<Note>, query: String, filter: NoteFilter): List<Note> =
        notes.asSequence()
            .filter { it.matches(query) }
            .filter {
                when (filter) {
                    NoteFilter.ALL -> true
                    NoteFilter.LINKED -> it.isLinked
                    NoteFilter.FRICTION -> it.isFriction
                    NoteFilter.FREE -> !it.isLinked
                }
            }
            .toList()

    /** Every tag in use, most-used first. Drives the filter row. */
    fun allTags(notes: List<Note>): List<String> =
        notes.flatMap { it.tags }
            .groupingBy { it }
            .eachCount()
            .entries
            .sortedByDescending { it.value }
            .map { it.key }

    /**
     * A prompt for a note attached to something that just happened.
     *
     * Friction gets a different question from a completion on purpose. "What did you expect
     * to happen, and what did?" is the single most useful thing someone can write after being
     * refused, because the gap between those two answers is where the fear lives.
     */
    fun promptFor(kind: String): String = when (kind) {
        "FRICTION" -> "What did you expect to happen? What actually happened?"
        "REP" -> "How did that one go?"
        "MISSION" -> "What was harder than you expected about that step?"
        "LESSON" -> "What in that is actually true for you?"
        else -> "What happened, and what surprised you?"
    }
}
