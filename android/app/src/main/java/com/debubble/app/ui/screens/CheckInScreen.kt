package com.debubble.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.CareLibrary
import com.debubble.app.engine.Domain
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.Look
import com.debubble.app.engine.Stage
import com.debubble.app.ui.components.Dot
import com.debubble.app.ui.components.HouseScene
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.LookPicker
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SelectRow
import com.debubble.app.ui.components.SetupSteps
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * The honest check-in: how is your body, your room and your heart, really?
 *
 * The first time, this is moving in. The room is drawn above the questions and changes as
 * each one is answered, so the first thing a new user learns about the room is the only
 * thing that matters about it — it shows the truth, and it is theirs.
 *
 * Afterwards it is reachable any time from the dashboard. The room slips on its own when a
 * domain is not looked after, but only the user knows their actual floor; this is how they
 * tell it. Self-monitoring works because the record is accurate, so the questions are worded
 * to make the honest answer the easy one — no answer is bad, and each one is phrased in the
 * user's own voice rather than as a rating.
 */
@Composable
fun CheckInScreen(
    firstRun: Boolean,
    art: RoomArt,
    library: CareLibrary,
    stage: Stage,
    home: HomeState,
    initial: Map<Domain, Int>,
    onLook: (Look) -> Unit,
    onSave: (Map<Domain, Int>) -> Unit,
    onBack: (() -> Unit)?
) {
    var levels by remember { mutableStateOf(initial) }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = if (firstRun) "Move in" else "Honest check-in",
            subtitle = if (firstRun) "Last step" else "Only you see this",
            onBack = onBack
        )
        if (firstRun) SetupSteps(art, 4)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            Text(
                text = if (firstRun) "This room is a mirror of your real one."
                else "How is it, really?",
                color = Ink.Primary,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = if (firstRun) {
                    "It starts empty, and everything in it you will earn by looking after " +
                        "yourself. When your real body, room or heart need care, it shows " +
                        "here, and doing the real thing is what cleans it. Be honest — " +
                        "nobody sees this but you."
                } else {
                    "The room guesses from how long it has been. You know better. Whatever " +
                        "you pick, the next step is the same: one small thing."
                },
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )

            HouseScene(
                art = art,
                stage = stage,
                home = home,
                dirt = levels,
                description = "Your room, as you describe it"
            )

            // Moving in is when you make the character yours, before anything else happens
            // to it. Afterwards it lives in the room screen.
            if (firstRun) {
                VSpace(4)
                Text("This is you", color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                Label("Pick what looks like you. You can change it any time from your room.")
                LookPicker(art = art, look = home.look, onChange = onLook)
            }

            Domain.order.forEach { d ->
                val info = library.info(d)
                VSpace(8)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Dot(d.accent, size = 10)
                    Text(
                        text = info.name,
                        color = d.accent,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Label(info.levels[levels[d] ?: 0])
                }
                info.checkin.forEachIndexed { level, answer ->
                    SelectRow(
                        label = answer,
                        selected = levels[d] == level,
                        onToggle = { levels = levels + (d to level) },
                        accent = d.accent,
                        single = true
                    )
                }
            }
            VSpace(8)
        }
        Column(modifier = Modifier.fillMaxWidth().padding(Space.gutter)) {
            PrimaryButton(
                text = if (firstRun) "Move in" else "Save",
                accent = Ink.Room,
                onClick = { onSave(levels) }
            )
        }
    }
}
