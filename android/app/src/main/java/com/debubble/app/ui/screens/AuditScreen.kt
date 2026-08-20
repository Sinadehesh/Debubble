package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.AuditCatalogue
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.SelectRow
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space

/**
 * The Systems Audit.
 *
 * This screen asks someone to list things about themselves that are not working, which is a
 * genuinely risky thing for an app aimed at isolated people to do. Three things keep it from
 * being a self-esteem demolition:
 *
 * 1. **Every item is a behaviour with a fix attached.** Nothing here is a verdict about who
 *    someone is. Each line is a habit, and each one has three graded challenges behind it
 *    that start today.
 * 2. **The framing is diagnostic, not confessional.** You are marking target areas on a
 *    system, not admitting to faults. The copy never uses "weakness", "flaw" or "problem".
 * 3. **Selecting nothing is allowed and stated.** Someone who is not ready to name any of it
 *    can skip, and the app works fine — the audit adds one challenge a day, it does not gate
 *    anything.
 */
@Composable
fun AuditScreen(
    catalogue: AuditCatalogue,
    initial: Set<String>,
    firstRun: Boolean,
    onSave: (Set<String>) -> Unit,
    onBack: (() -> Unit)?
) {
    var marked by remember { mutableStateOf(initial) }
    var tab by remember { mutableStateOf(catalogue.categories.firstOrNull()?.id ?: "") }

    val category = catalogue.categories.firstOrNull { it.id == tab }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
    ) {
        TopBar(
            title = "Systems audit",
            subtitle = if (marked.isEmpty()) "Nothing marked yet"
            else "${marked.size} marked",
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            if (firstRun) {
                Column(
                    modifier = Modifier
                        .padding(horizontal = Space.gutter)
                        .fillMaxWidth()
                        .panel(fill = Ink.SurfaceHigh, shape = RoundedCornerShape(Space.radiusLarge))
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text(
                        text = "Mark your target areas.",
                        color = Ink.Primary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Text(
                        text = "This is a checklist of specific habits, not a judgement. " +
                            "Everything on it is something that can be changed, and " +
                            "everything you mark gets its own challenges — starting with one " +
                            "you could do today.",
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(9.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Glyph(Glyphs.Lock, colour = Ink.Muted, size = 17)
                        Text(
                            text = "Nobody sees this. It never leaves your phone, and you " +
                                "can change it any time.",
                            color = Ink.Muted,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
                VSpace(18)
            }

            // Category tabs. Each carries its own count, so someone can see at a glance
            // where they have concentrated.
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = Space.gutter),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                catalogue.categories.forEach { c ->
                    val on = c.id == tab
                    val count = catalogue.countIn(c.id, marked)
                    Row(
                        modifier = Modifier
                            .heightIn(min = Space.tap)
                            .panel(
                                shape = RoundedCornerShape(50),
                                fill = if (on) Ink.Access.copy(alpha = 0.18f) else Ink.Surface,
                                border = if (on) Ink.Access else Ink.Border,
                                borderWidth = if (on) 2.dp else 1.dp
                            )
                            .clickable(role = Role.Tab) { tab = c.id }
                            .semantics { selected = on }
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = c.name,
                            color = if (on) Ink.Primary else Ink.Secondary,
                            style = MaterialTheme.typography.labelLarge.copy(
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                        if (count > 0) Pill("$count", Ink.Access, filled = on)
                    }
                }
            }

            VSpace(18)

            if (category != null) {
                Column(modifier = Modifier.padding(horizontal = Space.gutter)) {
                    Text(
                        text = category.blurb,
                        color = Ink.Primary,
                        style = MaterialTheme.typography.titleMedium
                    )
                    VSpace(6)
                    Text(
                        text = category.note,
                        color = Ink.Muted,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    VSpace(16)

                    Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
                        catalogue.inCategory(category.id).forEach { d ->
                            SelectRow(
                                label = d.label,
                                detail = d.detail,
                                selected = d.id in marked,
                                accent = Ink.Access,
                                onToggle = {
                                    marked = if (d.id in marked) marked - d.id else marked + d.id
                                }
                            )
                        }
                    }
                }
            }

            VSpace(20)

            if (marked.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .padding(horizontal = Space.gutter)
                        .fillMaxWidth()
                        .panel(fill = Ink.SurfaceHigh)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Glyph(Glyphs.Spark, colour = Ink.Gold, size = 18)
                    Text(
                        text = "You will get one of these a day, on rotation, on top of your " +
                            "usual challenges. Each one starts with the easiest version.",
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            VSpace(24)
        }

        Column(
            modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, bottom = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PrimaryButton(
                text = when {
                    marked.isEmpty() && firstRun -> "Skip for now"
                    marked.isEmpty() -> "Save with nothing marked"
                    else -> "Save ${marked.size} target${if (marked.size == 1) "" else "s"}"
                }
            ) { onSave(marked) }
            if (!firstRun && onBack != null) TextAction("Cancel") { onBack() }
            else if (firstRun) {
                Label(
                    "You can come back and change this whenever you want.",
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }
    }
}
