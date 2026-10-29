package com.debubble.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Care
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.CareState
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Size
import com.debubble.app.engine.Tour
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.FirstTimeSteps
import com.debubble.app.ui.components.HoldToCommit
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.SelectRow
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * One care routine, today.
 *
 * Top to bottom it is the whole protocol, in the order a person needs it: what the act is,
 * when it happens (the if-then cue), how much of it (the size, pre-selected small), one
 * change that makes it easier, and why it works. Then the hold to say it is done.
 *
 * The size question is the one that matters most on a bad day. "How much have you got?" lets
 * the step get smaller instead of getting skipped, which is the behavioural activation
 * principle that keeps people in treatment; tiny is pre-selected for the first week and after
 * any gap, and it counts in full.
 */
@Composable
fun CareScreen(
    domain: Domain,
    library: CareLibrary,
    care: CareState,
    level: Int,
    doneToday: Boolean,
    suggested: Size,
    sizeNote: String?,
    onChooseRoutine: (String) -> Unit,
    onSetAnchor: (String) -> Unit,
    onDone: (Size) -> Unit,
    onBack: () -> Unit,
    /** Nothing looked after yet: say how this screen works, in order. */
    firstTime: Boolean = false
) {
    val info = library.info(domain)
    val routine = library.chosen(domain, care)
    val accent = domain.accent
    var size by remember(routine.id) { mutableStateOf(suggested) }
    var swapping by remember { mutableStateOf(false) }
    var committed by remember { mutableStateOf(false) }
    val anchor = care.anchor.ifBlank { routine.anchors.first() }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = info.name,
            subtitle = "Right now: ${info.levels[level]}",
            accent = accent,
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Label("Your ${info.name.lowercase()} routine")
                if (doneToday) Pill("Done today", accent)
            }
            Text(
                text = routine.title,
                color = Ink.Primary,
                style = MaterialTheme.typography.displayMedium
            )
            if (firstTime) FirstTimeSteps(Tour.CARE_HOW, accent)

            // ---- when: the cue
            VSpace(6)
            SectionHeader("When")
            Text(
                text = "When $anchor, I do it.",
                color = Ink.Primary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold)
            )
            Label("Same moment every day is what turns this into a habit. Pick the one that fits.")
            routine.anchors.forEach { a ->
                SelectRow(
                    label = "When $a",
                    selected = a == anchor,
                    onToggle = { onSetAnchor(a) },
                    accent = accent,
                    single = true
                )
            }

            // ---- how much: the size
            VSpace(6)
            SectionHeader("How much have you got?")
            Size.entries.forEach { s ->
                SelectRow(
                    label = "${s.label} · ${routine.minutes(s)} min",
                    detail = routine.text(s),
                    selected = size == s,
                    onToggle = { size = s },
                    accent = accent,
                    single = true
                )
            }
            if (sizeNote != null) {
                Text(
                    text = sizeNote,
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            // ---- make it easier
            VSpace(6)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(fill = Ink.SurfaceHigh, border = accent.copy(alpha = 0.6f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Glyph(Glyphs.Spark, colour = accent, size = 16)
                    Label("Make it easier", color = accent, strong = true)
                }
                Text(text = routine.ease, color = Ink.Primary, style = MaterialTheme.typography.bodyMedium)
            }

            // ---- why it works
            VSpace(6)
            SectionHeader("Why it works")
            Text(text = routine.why, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
            Label(routine.source)

            // ---- habit strength
            VSpace(6)
            val days = Care.daysWithRoutine(care)
            val auto = Care.automaticity(days)
            SectionHeader(
                "How automatic it is",
                trailing = "$days ${if (days == 1) "day" else "days"} with this one"
            )
            ProgressTrack(fraction = auto, accent = accent, height = 8)
            Label(
                "About ${(auto * 100).toInt()}%, estimated from the typical curve in Lally " +
                    "et al. (2010): most people were near automatic by day 66, but the range " +
                    "was 18 to 254 days. Missing one day did not set anyone back."
            )

            // ---- a different routine
            VSpace(6)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = Space.tap)
                    .panel()
                    .clickable(role = Role.Button) { swapping = !swapping }
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Label(
                    if (swapping) "Keep this one" else "Choose a different routine",
                    color = Ink.Primary,
                    strong = true
                )
                Glyph(if (swapping) Glyphs.ArrowLeft else Glyphs.ArrowRight, colour = Ink.Muted, size = 16)
            }
            if (swapping) {
                Label(
                    "Choose the one you would actually do. Everything you have earned stays; " +
                        "the habit count starts again, because it is a new habit."
                )
                library.routines(domain).forEach { r ->
                    SelectRow(
                        label = r.title,
                        detail = r.tiny,
                        selected = r.id == routine.id,
                        onToggle = {
                            onChooseRoutine(r.id)
                            swapping = false
                        },
                        accent = accent,
                        single = true
                    )
                }
            }
            VSpace(10)
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HoldToCommit(
                accent = accent,
                label = if (doneToday) "Hold to log another" else "Hold when it is done",
                modifier = Modifier.fillMaxWidth()
            ) {
                if (!committed) {
                    committed = true
                    onDone(size)
                }
            }
        }
    }
}
