package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Progress
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.ParticleBurst
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

private const val JOURNAL_MAX = 90

/**
 * Done.
 *
 * A particle burst, what actually changed, the XP earned, and one optional line about it.
 * The note field cannot physically hold a paragraph, and skipping it costs nothing.
 */
@Composable
fun CompletionScreen(
    pillar: Pillar,
    tierCleared: Int,
    onSave: (String) -> Unit,
    onSkip: () -> Unit
) {
    var note by remember { mutableStateOf("") }

    val delta = when (pillar) {
        Pillar.ACCESS -> "You went somewhere new"
        Pillar.ACTIVITY -> "You did something new"
        Pillar.SOCIAL -> "You spoke to someone"
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink.Void)) {
        ParticleBurst(accent = pillar.accent, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(horizontal = Space.gutter)
                .padding(top = 40.dp, bottom = Space.gutter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Glyph(Glyphs.Check, colour = pillar.accent, size = 56, weight = 2.6f)
                VSpace(20)
                Text(
                    text = "Level $tierCleared done.",
                    color = Ink.Primary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.displayMedium
                )
                VSpace(10)
                Text(
                    text = delta,
                    color = pillar.accent,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.titleMedium
                )
                VSpace(18)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Pill("+${Progress.XP_CHALLENGE} XP", Ink.Gold, filled = true)
                    Pill(pillar.display, pillar.accent)
                }
                VSpace(28)
                Text(
                    text = "Your bubble is bigger than it was this morning. That is the " +
                        "whole thing — it only ever happens one of these at a time.",
                    color = Ink.Secondary,
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(shape = RoundedCornerShape(Space.radiusLarge))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Label("Add a note (optional)", color = Ink.Primary, strong = true)
                    Label("${note.length}/$JOURNAL_MAX")
                }
                OutlinedTextField(
                    value = note,
                    onValueChange = { if (it.length <= JOURNAL_MAX) note = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = {
                        Text(
                            text = "One line about how it went",
                            color = Ink.Muted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    textStyle = MaterialTheme.typography.bodyLarge,
                    singleLine = true,
                    shape = RoundedCornerShape(Space.radius),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Ink.Primary,
                        unfocusedTextColor = Ink.Primary,
                        focusedBorderColor = pillar.accent,
                        unfocusedBorderColor = Ink.Border,
                        cursorColor = pillar.accent,
                        focusedContainerColor = Ink.SurfaceHigh,
                        unfocusedContainerColor = Ink.SurfaceHigh
                    )
                )
            }

            VSpace(12)
            PrimaryButton(
                text = if (note.isBlank()) "Done" else "Save it",
                accent = pillar.accent
            ) { onSave(note) }
            TextAction("Skip") { onSkip() }
        }
    }
}
