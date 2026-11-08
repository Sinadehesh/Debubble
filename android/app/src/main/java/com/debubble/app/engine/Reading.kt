package com.debubble.app.engine

import kotlinx.serialization.Serializable
import kotlin.math.ceil

/**
 * Reading: the books you are reading, a page at a time, and the ones you finished on a shelf.
 *
 * Reading is the Heart's kind of habit: a few quiet minutes that take you somewhere else. It
 * is built the same way as everything else here. The daily ask is tiny ("one page counts"),
 * because an amount small enough to do on a bad day is what turns into a habit (Lally et
 * al., 2010); it is tied to a moment you already have; and nothing is ever lost. A book left
 * for a month is still open at your page, and a finished one stays on the shelf for good.
 *
 * Pure: same inputs, same outputs, no clock and no Android.
 */
@Serializable
data class Book(
    /** When it was added, in milliseconds. Also its identity. */
    val id: Long,
    val title: String,
    val author: String = "",
    /** Pages in it, if you said. Zero means unknown, which is fine. */
    val pages: Int = 0,
    /** The page you are on. */
    val page: Int = 0,
    val startedDay: Long = 0L,
    /** The day it was finished, or zero while you are still reading it. */
    val finishedDay: Long = 0L
) {
    val finished: Boolean get() = finishedDay != 0L

    /** How far through, when the length is known. */
    val fraction: Float get() = if (pages <= 0) 0f else (page.toFloat() / pages).coerceIn(0f, 1f)
}

/** One sitting: so many pages of one book, on one day. */
@Serializable
data class ReadingEntry(
    val id: Long,
    val book: Long,
    val pages: Int,
    val epochDay: Long,
    /** Minute of the local day, for "when do I usually read". */
    val minute: Int = 0
)

object Reading {

    /** The pace is the last fortnight's pages, spread over the days in it. */
    const val PACE_DAYS = 14

    /** The suggested daily amount, and the smallest one that still counts. */
    const val DAILY_PAGES = 10
    const val TINY_PAGES = 1

    /** Books being read, the one touched most recently first. */
    fun reading(books: List<Book>, log: List<ReadingEntry>): List<Book> =
        books.filter { !it.finished }.sortedByDescending { b ->
            log.filter { it.book == b.id }.maxOfOrNull { it.id } ?: b.id
        }

    /** Finished books, in the order they were finished: the shelf, left to right. */
    fun shelf(books: List<Book>): List<Book> = books.filter { it.finished }.sortedBy { it.finishedDay }

    fun pagesOn(log: List<ReadingEntry>, day: Long): Int = log.filter { it.epochDay == day }.sumOf { it.pages }

    /** Days anything was read. */
    fun days(log: List<ReadingEntry>): Set<Long> = log.filter { it.pages > 0 }.map { it.epochDay }.toSet()

    /** Average pages a day over the last [PACE_DAYS] days, today included. */
    fun pace(log: List<ReadingEntry>, today: Long): Float {
        val from = today - PACE_DAYS + 1
        val pages = log.filter { it.epochDay in from..today }.sumOf { it.pages }
        return pages / PACE_DAYS.toFloat()
    }

    /** Days left at your pace, when both the length and a pace are known. */
    fun daysToFinish(book: Book, pace: Float): Int? {
        if (book.pages <= 0 || book.finished || pace <= 0f) return null
        val left = (book.pages - book.page).coerceAtLeast(0)
        return ceil(left / pace).toInt().coerceAtLeast(1)
    }

    /**
     * Move to [page]. Going back is allowed (a re-read, or a typo); only forward pages count
     * as read. Reaching the last page finishes the book.
     */
    fun turnTo(book: Book, page: Int, today: Long, now: Long, minute: Int): Pair<Book, ReadingEntry?> {
        val capped = if (book.pages > 0) page.coerceIn(0, book.pages) else page.coerceAtLeast(0)
        val read = (capped - book.page).coerceAtLeast(0)
        val done = book.pages > 0 && capped >= book.pages
        val next = book.copy(page = capped, finishedDay = if (done) today else book.finishedDay)
        val entry = if (read > 0) ReadingEntry(now, book.id, read, today, minute) else null
        return next to entry
    }

    /** Read [pages] more. */
    fun readMore(book: Book, pages: Int, today: Long, now: Long, minute: Int): Pair<Book, ReadingEntry?> =
        turnTo(book, book.page + pages, today, now, minute)

    /** Finished, whatever page it says. Books with no length are finished this way. */
    fun finish(book: Book, today: Long): Book = book.copy(
        finishedDay = today,
        page = if (book.pages > 0) book.pages else book.page
    )

    /** The line under the current book. */
    fun nudge(book: Book, pagesToday: Int, pace: Float): String = when {
        pagesToday >= DAILY_PAGES -> "$pagesToday pages today. That is a good day of reading."
        pagesToday > 0 -> "$pagesToday ${if (pagesToday == 1) "page" else "pages"} today. One page counts."
        book.page == 0 -> "Read the first page. That is the whole ask."
        else -> daysToFinish(book, pace)?.let { "At your pace, about $it ${if (it == 1) "day" else "days"} to the end." }
            ?: "Ten pages, or one if that is what you have. One page counts."
    }
}
