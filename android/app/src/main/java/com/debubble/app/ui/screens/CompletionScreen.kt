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
import com.debubble.app.ui.components.ExpansionBurst
import com.debubble.app.ui.components.Instrument
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.tierCode
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

private const val JOURNAL_MAX = 90

/**
 * Completion and micro-journal.
 *
 * The reward is a geometric expansion, not confetti, and progress is stated as a physical
 * measurement rather than points. The journal is capped at one sentence by construction —
 * the field cannot hold a paragraph — and skipping it is a plain, unpunished option.
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
        Pillar.ACCESS -> "Access radius extended"
        Pillar.ACTIVITY -> "One novel act logged"
        Pillar.SOCIAL -> "One direct contact made"
    }
    val closing = when (pillar) {
        Pillar.ACCESS -> "The perimeter moved. Tomorrow it moves again."
        Pillar.ACTIVITY -> "One repetition broken. Your routine is now negotiable."
        Pillar.SOCIAL -> "Contact made. That is the whole mechanism, repeated a hundred times."
    }

    Box(modifier = Modifier.fillMaxSize().background(Ink.Void)) {
        ExpansionBurst(accent = pillar.accent, modifier = Modifier.fillMaxSize())

        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .padding(Space.gutter)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Instrument("Tier ${tierCode(tierCleared)} cleared")
                VSpace(12)
                Text(
                    text = "Perimeter\nextended.",
                    color = Ink.Primary,
                    style = MaterialTheme.typography.displayMedium,
                    textAlign = TextAlign.Center
                )
                VSpace(14)
                Instrument(delta, color = pillar.accent)
                VSpace(14)
                Text(
                    text = closing,
                    color = Ink.Ash,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }

            Divider()
            VSpace(18)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Instrument("Micro-journal")
                Instrument("${note.length}/$JOURNAL_MAX")
            }
            VSpace(10)

            OutlinedTextField(
                value = note,
                onValueChange = { if (it.length <= JOURNAL_MAX) note = it.replace("\n", "") },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "What actually happened?",
                        color = Ink.Faint,
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                singleLine = true,
                shape = RoundedCornerShape(Space.radius),
                textStyle = MaterialTheme.typography.bodyLarge,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = pillar.accent,
                    unfocusedBorderColor = Ink.Edge,
                    focusedContainerColor = Ink.Ridge,
                    unfocusedContainerColor = Ink.Ridge,
                    focusedTextColor = Ink.Primary,
                    unfocusedTextColor = Ink.Primary,
                    cursorColor = pillar.accent
                )
            )

            VSpace(12)
            PrimaryButton(if (note.isBlank()) "Done" else "Log it") { onSave(note) }
            GhostButton("Skip") { onSkip() }
        }
    }
}
