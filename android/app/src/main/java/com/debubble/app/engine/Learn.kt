package com.debubble.app.engine

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * One lesson in the Learn tab.
 *
 * Two kinds, distinguished by [day]:
 *
 *  - **Course** lessons carry a day from 1 to 30 and unlock on that day of the user's run.
 *    They are the spine, and there are no gaps in the sequence.
 *  - **Targeted** lessons carry day 0 and a list of [debuffs]. They appear the moment a
 *    matching item is flagged in the Systems Audit, and they appear *immediately* rather
 *    than on a schedule — someone who has just admitted their clothes do not fit should not
 *    wait three weeks to read about fit.
 */
@Serializable
data class Lesson(
    @SerialName("id") val id: String,
    @SerialName("day") val day: Int,
    @SerialName("title") val title: String,
    @SerialName("summary") val summary: String,
    @SerialName("min") val minutes: Int,
    @SerialName("tags") val tags: List<String> = emptyList(),
    @SerialName("debuffs") val debuffs: List<String> = emptyList(),
    /** Markdown. Headings, bold, italic, lists and block quotes. */
    @SerialName("body") val body: String
) {
    val isTargeted: Boolean get() = day == 0

    fun unlockedOn(dayIndex: Long, selected: Set<String>): Boolean =
        if (isTargeted) debuffs.any { it in selected } else day <= dayIndex
}

@Serializable
data class LearnCurriculum(
    @SerialName("lessons") val lessons: List<Lesson> = emptyList()
) {
    val course: List<Lesson> by lazy { lessons.filter { !it.isTargeted }.sortedBy { it.day } }

    val targeted: List<Lesson> by lazy { lessons.filter { it.isTargeted } }

    fun byId(id: String): Lesson? = lessons.firstOrNull { it.id == id }

    /** Targeted lessons the user's audit has switched on, in catalogue order. */
    fun targetedFor(selected: Set<String>): List<Lesson> =
        if (selected.isEmpty()) emptyList()
        else targeted.filter { l -> l.debuffs.any { it in selected } }

    /** Everything readable right now. */
    fun unlocked(dayIndex: Long, selected: Set<String>): List<Lesson> =
        lessons.filter { it.unlockedOn(dayIndex, selected) }

    /** The next course lesson that is not yet available, for the "coming up" row. */
    fun nextLocked(dayIndex: Long): Lesson? = course.firstOrNull { it.day > dayIndex }

    fun readCount(read: Set<String>): Int = lessons.count { it.id in read }
}

/**
 * One pack in the reading room: five short reads on one thing that sits underneath
 * everything else, like sleep or a quieter mind. Premium. Authored in tools/gen_reading.py.
 */
@Serializable
data class ReadingPack(
    @SerialName("id") val id: String,
    @SerialName("title") val title: String,
    @SerialName("blurb") val blurb: String,
    /** The room sprite on its cover. */
    @SerialName("icon") val icon: String,
    @SerialName("lessons") val lessons: List<Lesson> = emptyList()
)

@Serializable
data class ReadingRoom(
    @SerialName("packs") val packs: List<ReadingPack> = emptyList()
) {
    fun pack(id: String): ReadingPack? = packs.firstOrNull { it.id == id }

    fun lesson(id: String): Lesson? = packs.firstNotNullOfOrNull { p -> p.lessons.firstOrNull { it.id == id } }

    fun packOf(lessonId: String): ReadingPack? = packs.firstOrNull { p -> p.lessons.any { it.id == lessonId } }

    val pageCount: Int get() = packs.sumOf { it.lessons.size }

    fun readIn(pack: ReadingPack, read: Set<String>): Int = pack.lessons.count { it.id in read }
}
