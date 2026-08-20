package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.data.Note
import com.debubble.app.data.NoteFilter
import com.debubble.app.data.Notes
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Markdown
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space

/**
 * The Notes tab.
 *
 * Everything else in this app is a count. This is the only place someone writes more than a
 * sentence, and it is the part most likely to still matter in a year — the record of what
 * they thought at the time, which is the only way to see a mindset shift rather than be told
 * one happened.
 *
 * Search and the filter row exist for exactly one question, asked around day ninety:
 * "what was I writing when this was still frightening?"
 */
@Composable
fun NotesScreen(
    notes: List<Note>,
    dayIndexOf: (Note) -> Long,
    onOpen: (Long) -> Unit,
    onNew: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf(NoteFilter.ALL) }

    val shown = Notes.filter(notes, query, filter)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
    ) {
        TopBar(
            title = "Notes",
            subtitle = if (notes.isEmpty()) "Nothing written yet"
            else "${notes.size} entr${if (notes.size == 1) "y" else "ies"}"
        )

        Column(modifier = Modifier.padding(horizontal = Space.gutter)) {
            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = "Search everything you have written",
                        color = Ink.Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(Space.radius),
                textStyle = MaterialTheme.typography.bodyLarge,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Ink.Primary,
                    unfocusedTextColor = Ink.Primary,
                    focusedBorderColor = Ink.Access,
                    unfocusedBorderColor = Ink.Border,
                    cursorColor = Ink.Access,
                    focusedContainerColor = Ink.Surface,
                    unfocusedContainerColor = Ink.Surface
                )
            )

            VSpace(10)

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                NoteFilter.entries.forEach { f ->
                    val on = f == filter
                    val count = Notes.filter(notes, "", f).size
                    Row(
                        modifier = Modifier
                            .heightIn(min = Space.tap)
                            .panel(
                                shape = RoundedCornerShape(50),
                                fill = if (on) Ink.Access.copy(alpha = 0.18f) else Ink.Surface,
                                border = if (on) Ink.Access else Ink.Border,
                                borderWidth = if (on) 2.dp else 1.dp
                            )
                            .clickable(role = Role.Tab) { filter = f }
                            .semantics { selected = on }
                            .padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        Text(
                            text = f.display,
                            color = if (on) Ink.Primary else Ink.Secondary,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                        Label("$count")
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            VSpace(14)
            when {
                notes.isEmpty() -> EmptyState(
                    title = "Start writing.",
                    body = "After a challenge, after a no, or for no reason. This is the " +
                        "only place in the app that keeps what you actually thought."
                )

                shown.isEmpty() -> EmptyState(
                    title = "Nothing matches.",
                    body = "Try a different word, or switch the filter back to All."
                )

                else -> Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
                    shown.forEach { note ->
                        NoteCard(note, dayIndexOf(note)) { onOpen(note.id) }
                    }
                }
            }
            VSpace(20)
        }

        Column(modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, bottom = Space.gutter)) {
            PrimaryButton("Write something", onClick = onNew)
        }
    }
}

@Composable
private fun EmptyState(title: String, body: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.SurfaceHigh, shape = RoundedCornerShape(Space.radiusLarge))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Text(text = title, color = Ink.Primary, style = MaterialTheme.typography.headlineMedium)
        Text(text = body, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun NoteCard(note: Note, dayIndex: Long, onOpen: () -> Unit) {
    val accent = if (note.isFriction) Ink.Ember else Ink.Access
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(border = if (note.isLinked) accent.copy(alpha = 0.55f) else Ink.Border)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(15.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = note.title,
            color = Ink.Primary,
            style = MaterialTheme.typography.titleMedium
        )
        if (note.preview.isNotBlank()) {
            Text(
                text = note.preview,
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Pill("Day $dayIndex", Ink.Muted)
            if (note.isFriction) {
                Pill("Friction", Ink.Ember)
            } else if (note.isLinked) {
                Pill(linkLabel(note), accent)
            }
            note.tags.forEach { Pill("#$it", Ink.Muted) }
        }
        if (note.linkedTitle.isNotBlank()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                Glyph(Glyphs.ArrowRight, colour = accent, size = 14)
                Label(note.linkedTitle, color = accent)
            }
        }
    }
}

private fun linkLabel(note: Note): String = when (note.linkedKind) {
    "LESSON" -> "On a lesson"
    "REP" -> "On an attempt"
    "MISSION" -> "On a goal step"
    "AUDIT" -> "On an audit item"
    "CAMPAIGN" -> "On a goal challenge"
    else -> "On a challenge"
}

/**
 * Writing or editing one note.
 *
 * When it was opened from something that just happened, the prompt is specific to what that
 * was. After friction it asks what you expected versus what happened, because the gap between
 * those two answers is the single most useful thing anyone can write down after being refused.
 */
@Composable
fun NoteEditorScreen(
    existing: Note?,
    linkedTitle: String,
    linkedKind: String,
    onSave: (String, String) -> Unit,
    onDelete: (() -> Unit)?,
    onBack: () -> Unit
) {
    var body by remember { mutableStateOf(existing?.body ?: "") }
    var tags by remember { mutableStateOf(existing?.tags?.joinToString(", ") ?: "") }
    var preview by remember { mutableStateOf(false) }

    val prompt = Notes.promptFor(linkedKind)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .imePadding()
    ) {
        TopBar(
            title = if (existing == null) "New note" else "Edit note",
            subtitle = if (linkedTitle.isNotBlank()) linkedTitle else null,
            onBack = onBack,
            action = {
                Box(
                    modifier = Modifier
                        .heightIn(min = Space.tap)
                        .panel(
                            fill = if (preview) Ink.Access.copy(alpha = 0.18f) else Ink.Surface,
                            border = if (preview) Ink.Access else Ink.Border
                        )
                        .clickable(role = Role.Button) { preview = !preview }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Label(if (preview) "Edit" else "Preview", color = Ink.Primary, strong = true)
                }
            }
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            if (linkedTitle.isNotBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .panel(
                            fill = Ink.SurfaceHigh,
                            border = if (linkedKind == "FRICTION") Ink.Ember else Ink.Border
                        )
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Glyph(
                        Glyphs.Spark,
                        colour = if (linkedKind == "FRICTION") Ink.Ember else Ink.Access,
                        size = 18
                    )
                    Text(
                        text = prompt,
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                VSpace(12)
            }

            if (preview) {
                if (body.isBlank()) {
                    Label("Nothing to preview yet.")
                } else {
                    Markdown(text = body)
                }
            } else {
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 260.dp),
                    placeholder = {
                        Text(
                            text = "Write as much or as little as you want.\n\n" +
                                "**Bold**, *italic*, # headings and - lists all work.",
                            color = Ink.Muted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    shape = RoundedCornerShape(Space.radius),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Ink.Primary,
                        unfocusedTextColor = Ink.Primary,
                        focusedBorderColor = Ink.Access,
                        unfocusedBorderColor = Ink.Border,
                        cursorColor = Ink.Access,
                        focusedContainerColor = Ink.Surface,
                        unfocusedContainerColor = Ink.Surface
                    )
                )

                VSpace(12)
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "Tags, comma separated",
                            color = Ink.Muted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(Space.radius),
                    textStyle = MaterialTheme.typography.bodyMedium,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Ink.Primary,
                        unfocusedTextColor = Ink.Primary,
                        focusedBorderColor = Ink.Access,
                        unfocusedBorderColor = Ink.Border,
                        cursorColor = Ink.Access,
                        focusedContainerColor = Ink.Surface,
                        unfocusedContainerColor = Ink.Surface
                    )
                )
                VSpace(8)
                Label("Up to ${Notes.MAX_TAGS} tags. They show on the card and are searchable.")
            }

            VSpace(20)
        }

        Column(
            modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, bottom = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PrimaryButton(
                text = if (body.isBlank() && existing != null) "Delete this note" else "Save",
                accent = if (body.isBlank() && existing != null) Ink.Social else Ink.Access
            ) { onSave(body, tags) }
            if (onDelete != null) {
                TextAction("Delete", colour = Ink.Social) { onDelete() }
            }
        }
    }
}
