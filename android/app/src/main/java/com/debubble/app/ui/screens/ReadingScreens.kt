package com.debubble.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Book
import com.debubble.app.engine.Reading
import com.debubble.app.engine.ReadingPack
import com.debubble.app.ui.components.FieldBox
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.PremiumPill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.SheetHandle
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.theme.Cut
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.fill
import com.debubble.app.ui.theme.wash
import kotlin.math.floor

/** What the books section needs, gathered by whoever knows the state. */
class BookShelf(
    val reading: List<Book>,
    val finished: List<Book>,
    val pagesToday: Int,
    val pace: Float,
    val onAdd: (title: String, author: String, pages: Int) -> Unit,
    val onRead: (bookId: Long, pages: Int) -> Unit,
    val onSetPage: (bookId: Long, page: Int) -> Unit,
    val onFinish: (bookId: Long) -> Unit,
    val onRemove: (bookId: Long) -> Unit
)

/**
 * Your books: the one you are reading, a page at a time, and every finished one standing on
 * a pixel shelf. One page counts, the same rule as tiny everywhere else.
 */
@Composable
fun BooksSection(art: RoomArt, shelf: BookShelf) {
    var adding by remember { mutableStateOf(false) }
    var pageFor by remember { mutableStateOf<Book?>(null) }

    Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
    SectionHeader("Your books", trailing = if (shelf.finished.isEmpty()) null else "${shelf.finished.size} finished")
    if (shelf.finished.isNotEmpty()) Shelf(shelf.finished)

    if (shelf.reading.isEmpty()) {
        Column(
            modifier = Modifier.fillMaxWidth().panel(fill = Ink.Heart.wash).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                SpriteIcon(art, "books", pixelScale = pixelScale(2.2f))
                Text("Reading is a habit too", color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
            }
            Text(
                text = "Add the book you are reading, or one you mean to. A page a day counts, and " +
                    "every finished one goes on your shelf.",
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
            SecondaryButton("Add a book") { adding = true }
        }
    } else {
        shelf.reading.forEach { book ->
            BookCard(
                art = art,
                book = book,
                pagesToday = if (book == shelf.reading.first()) shelf.pagesToday else 0,
                pace = shelf.pace,
                onRead = { shelf.onRead(book.id, it) },
                onPage = { pageFor = book },
                onFinish = { shelf.onFinish(book.id) }
            )
        }
        TextAction("Add another book", colour = Ink.Secondary) { adding = true }
    }
    }

    if (adding) {
        AddBookSheet(
            onAdd = { t, a, p ->
                adding = false
                shelf.onAdd(t, a, p)
            },
            onDismiss = { adding = false }
        )
    }
    pageFor?.let { b ->
        PageSheet(
            book = b,
            onSet = { page ->
                pageFor = null
                shelf.onSetPage(b.id, page)
            },
            onRemove = {
                pageFor = null
                shelf.onRemove(b.id)
            },
            onDismiss = { pageFor = null }
        )
    }
}

@Composable
private fun BookCard(
    art: RoomArt,
    book: Book,
    pagesToday: Int,
    pace: Float,
    onRead: (Int) -> Unit,
    onPage: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth().panel(fill = Ink.Heart.wash, borderWidth = 3.dp, shadow = Ink.Border).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Spine(book, modifier = Modifier.size(width = 22.dp, height = 52.dp))
            Column(modifier = Modifier.weight(1f)) {
                Label("Reading now", color = Ink.Heart, strong = true)
                Text(book.title, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                if (book.author.isNotBlank()) Label(book.author)
            }
        }
        if (book.pages > 0) {
            ProgressTrack(book.fraction, Ink.Heart)
            Label("Page ${book.page} of ${book.pages}")
        } else if (book.page > 0) {
            Label("On page ${book.page}")
        }
        Text(Reading.nudge(book, pagesToday, pace), color = Ink.Primary, style = MaterialTheme.typography.bodyMedium)
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PageChip("+1 page") { onRead(1) }
            PageChip("+10 pages") { onRead(10) }
            PageChip("+25") { onRead(25) }
            PageChip("I am on page...", onPage)
        }
        TextAction("I finished it", colour = Ink.Heart, onClick = onFinish)
    }
}

@Composable
private fun PageChip(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .panel(shape = Cut.small, shadow = Ink.Border)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Ink.Primary, style = MaterialTheme.typography.labelLarge)
    }
}

/** A book's spine, its colour picked from its title so the same book is always the same. */
@Composable
private fun Spine(book: Book, modifier: Modifier = Modifier) {
    val fill = SPINES[Math.floorMod(book.title.hashCode(), SPINES.size)]
    Box(
        modifier = modifier
            .clip(Cut.small)
            .background(fill)
            .border(BorderStroke(2.dp, Ink.Border), Cut.small)
    )
}

private val SPINES = listOf(Ink.Heart.fill, Ink.Body.fill, Ink.Room.fill, Ink.Gold.fill, Ink.Access.fill, Ink.Activity.fill, Ink.Social.fill)

/** Every finished book, standing on a shelf, in the order you finished them. */
@Composable
private fun Shelf(books: List<Book>) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .panel(fill = Ink.Surface)
            .clearAndSetSemantics {
                contentDescription = "Your shelf: ${books.size} finished. " + books.joinToString(", ") { it.title }
            }
            .drawBehind {
                val px = floor(2.dp.toPx()).coerceAtLeast(1f)
                val plank = px * 3
                val floorY = size.height - plank - px * 3
                // The plank.
                drawRect(Ink.Room.fill, topLeft = Offset(px * 3, floorY), size = Size(size.width - px * 6, plank))
                drawRect(Ink.Border, topLeft = Offset(px * 3, floorY + plank), size = Size(size.width - px * 6, px))
                var x = px * 6
                books.forEach { b ->
                    val h = Math.floorMod(b.title.hashCode(), 9973)
                    val w = px * (5 + h % 3)
                    val tall = size.height * (0.55f + (h % 4) * 0.07f)
                    if (x + w > size.width - px * 6) return@forEach
                    val top = floor(floorY - tall)
                    drawRect(Ink.Border, topLeft = Offset(x, top), size = Size(w, floorY - top))
                    drawRect(SPINES[h % SPINES.size], topLeft = Offset(x + px, top + px), size = Size(w - px * 2, floorY - top - px))
                    drawRect(Ink.Border, topLeft = Offset(x + px, top + (floorY - top) * 0.3f), size = Size(w - px * 2, px))
                    x += w + px
                }
            }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddBookSheet(onAdd: (String, String, Int) -> Unit, onDismiss: () -> Unit) {
    var title by remember { mutableStateOf("") }
    var author by remember { mutableStateOf("") }
    var pages by remember { mutableStateOf("") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = Cut.sheet,
        containerColor = Ink.Surface,
        contentColor = Ink.Primary,
        scrimColor = Ink.Primary.copy(alpha = 0.45f),
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Add a book", color = Ink.Primary, style = MaterialTheme.typography.headlineMedium)
            FieldBox(value = title, onChange = { title = it }, placeholder = "Title", max = 80)
            FieldBox(value = author, onChange = { author = it }, placeholder = "Author (optional)", max = 60)
            FieldBox(
                value = pages,
                onChange = { pages = it.filter { c -> c.isDigit() }.take(4) },
                placeholder = "How many pages (optional)",
                max = 4
            )
            Label("Pages let it work out how long you have left at your pace. Leave it empty if you do not know.")
            PrimaryButton("Add it", enabled = title.isNotBlank()) {
                onAdd(title, author, pages.toIntOrNull() ?: 0)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PageSheet(book: Book, onSet: (Int) -> Unit, onRemove: () -> Unit, onDismiss: () -> Unit) {
    var page by remember { mutableStateOf(if (book.page > 0) book.page.toString() else "") }
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        shape = Cut.sheet,
        containerColor = Ink.Surface,
        contentColor = Ink.Primary,
        scrimColor = Ink.Primary.copy(alpha = 0.45f),
        dragHandle = { SheetHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter)
                .padding(bottom = 20.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Which page are you on?", color = Ink.Primary, style = MaterialTheme.typography.headlineMedium)
            Label(book.title)
            FieldBox(
                value = page,
                onChange = { page = it.filter { c -> c.isDigit() }.take(4) },
                placeholder = if (book.pages > 0) "1 to ${book.pages}" else "Page number",
                max = 4
            )
            PrimaryButton("Save", enabled = page.isNotBlank()) { onSet(page.toIntOrNull() ?: book.page) }
            TextAction("Take it off my list", colour = Ink.Social, onClick = onRemove)
        }
    }
}

/* ================================================================== the reading room */

/** The reading room's packs, in the Library. The first page of each is free to read. */
@Composable
fun ReadingRoomSection(
    art: RoomArt,
    packs: List<ReadingPack>,
    read: Set<String>,
    premium: Boolean,
    onOpenPack: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
    SectionHeader("Reading room", trailing = if (premium) "${packs.sumOf { it.lessons.size }} reads" else "Premium")
    Text(
        text = "Short reads on what sits underneath everything else: sleep, moving for your mood, " +
            "getting unstuck and a quieter mind." + if (premium) "" else " The first one in each pack is free.",
        color = Ink.Secondary,
        style = MaterialTheme.typography.bodyMedium
    )
    packs.forEach { p ->
        val done = p.lessons.count { it.id in read }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Space.tap)
                .panel()
                .clickable(role = Role.Button) { onOpenPack(p.id) }
                .padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(Cut.small)
                    .background(Ink.Heart.wash)
                    .border(BorderStroke(2.dp, Ink.Border), Cut.small),
                contentAlignment = Alignment.Center
            ) {
                SpriteIcon(art, p.icon, pixelScale = pixelScale(1.6f))
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(p.title, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                Text(p.blurb, color = Ink.Secondary, style = MaterialTheme.typography.bodySmall)
                Label(if (done > 0) "$done of ${p.lessons.size} read" else "${p.lessons.size} short reads")
            }
            if (!premium) PremiumPill() else Glyph(Glyphs.ArrowRight, colour = Ink.Primary, size = 16)
        }
    }
    }
}

/** One pack: its pages, the first one free, the rest behind Premium. */
@Composable
fun PackScreen(
    art: RoomArt,
    pack: ReadingPack,
    read: Set<String>,
    premium: Boolean,
    onOpen: (String) -> Unit,
    onPremium: () -> Unit,
    onBack: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        TopBar(title = pack.title, subtitle = "Reading room", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().panel(fill = Ink.Heart.wash).padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpriteIcon(art, pack.icon, pixelScale = pixelScale(2.4f))
                Text(pack.blurb, color = Ink.Primary, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
            }
            pack.lessons.forEachIndexed { i, l ->
                val open = premium || i == 0
                val isRead = l.id in read
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .panel(fill = if (open) Ink.Surface else Ink.Void, border = if (open) Ink.Border else Ink.Faint)
                        .clickable(role = Role.Button) { onOpen(l.id) }
                        .semantics { stateDescription = if (!open) "Premium" else if (isRead) "Read" else "Unread" }
                        .padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("${i + 1}", color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
                        Text(l.title, color = if (open) Ink.Primary else Ink.Secondary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                        when {
                            !open -> Glyph(Glyphs.Lock, colour = Ink.Secondary, size = 16)
                            isRead -> com.debubble.app.ui.components.Pill("Read", Ink.Gold, filled = true)
                            i == 0 && !premium -> com.debubble.app.ui.components.Pill("Free", Ink.Heart, filled = true)
                        }
                    }
                    Text(l.summary, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
                    Label("${l.minutes} min")
                }
            }
            if (!premium) {
                VSpace(4)
                Column(
                    modifier = Modifier.fillMaxWidth().panel(fill = Ink.Gold.wash, borderWidth = 3.dp, shadow = Ink.Border).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("The whole reading room", color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
                    Text(
                        text = "Every pack, every page, plus training plans and insights. It is part of Premium.",
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    PrimaryButton("See Premium", onClick = onPremium)
                }
            }
        }
    }
}
