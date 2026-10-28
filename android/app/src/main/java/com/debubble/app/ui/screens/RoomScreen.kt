package com.debubble.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Care
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Family
import com.debubble.app.engine.Home
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.Scene
import com.debubble.app.engine.Stage
import com.debubble.app.ui.components.HouseScene
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.LabelledProgress
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space

/**
 * Your room, full screen, to arrange.
 *
 * Drag anything — including yourself — and it stays where you put it. Floor things stay on
 * the floor and wall things on the wall. Tap something to select it; put it away and it
 * waits in the tray below, still yours.
 *
 * The collection at the bottom says what is still to come and exactly how it comes, but not
 * what it is: the timing is shown so it reads as a goal, and the item stays a surprise.
 */
@Composable
fun RoomScreen(
    art: RoomArt,
    stage: Stage,
    home: HomeState,
    dirt: Map<Domain, Int>,
    rungs: Int,
    careReps: Map<Domain, Int>,
    onMove: (id: String, x: Int, b: Int) -> Unit,
    onPutAway: (String) -> Unit,
    onPutBack: (String) -> Unit,
    onSeen: () -> Unit,
    onBack: () -> Unit
) {
    var selected by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) { onSeen() }
    val cat = art.catalogue
    val walls = Home.wallsStand(stage)
    val stored = home.owned.filter { it !in home.placed }

    Column(modifier = Modifier.fillMaxSize()) {
        TopBar(
            title = "Your room",
            subtitle = if (home.owned.isEmpty()) "Nothing yet. It all gets earned."
            else "Drag anything to move it",
            onBack = onBack
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            HouseScene(
                art = art,
                stage = stage,
                home = home,
                dirt = dirt,
                editable = true,
                selected = selected,
                onSelect = { selected = it },
                onMove = onMove,
                description = "Your room. Drag things to move them."
            )

            val pick = selected
            val item = pick?.let { cat.item(it) }
            when {
                pick == Scene.ME -> Label("That is you. Put yourself wherever you like.")
                item != null -> Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .panel(fill = Ink.SurfaceHigh)
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(modifier = Modifier.size(52.dp), contentAlignment = Alignment.Center) {
                        SpriteIcon(art, item.id, pixelScale = if (item.w > 24) 2 else 3)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(item.label, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                        Label(sourceOf(item.family))
                    }
                    Box(modifier = Modifier.weight(0.9f)) {
                        SecondaryButton("Put away") {
                            onPutAway(item.id)
                            selected = null
                        }
                    }
                }
                else -> Label(
                    if (walls) "Tap something to select it, or drag it somewhere new."
                    else "No walls left out here, so the wall things are packed away for now."
                )
            }

            if (stored.isNotEmpty()) {
                VSpace(6)
                SectionHeader("Put away", trailing = "${stored.size}")
                stored.mapNotNull { cat.item(it) }.forEach { stash ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Space.tap)
                            .panel()
                            .clickable(role = Role.Button) { onPutBack(stash.id) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(modifier = Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                            SpriteIcon(art, stash.id, pixelScale = if (stash.w > 24) 1 else 2)
                        }
                        Text(
                            text = stash.label,
                            modifier = Modifier.weight(1f),
                            color = Ink.Primary,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Label("Put back", color = Ink.Gold, strong = true)
                    }
                }
            }

            VSpace(8)
            SectionHeader("Collection", trailing = "${home.owned.size} of ${cat.items.size}")
            Family.entries.forEach { f ->
                val have = Home.ownedIn(home, cat, f)
                val total = cat.family(f).size
                val count = when (f) {
                    Family.WORLD -> rungs
                    Family.BODY -> careReps[Domain.BODY] ?: 0
                    Family.ROOM -> careReps[Domain.ROOM] ?: 0
                    Family.HEART -> careReps[Domain.HEART] ?: 0
                }
                val next = Care.nextRewardIn(count)
                LabelledProgress(
                    label = familyName(f),
                    value = "$have of $total",
                    fraction = have.toFloat() / total.coerceAtLeast(1),
                    accent = familyColour(f)
                )
                Label(
                    sourceOf(f.name) + when (next) {
                        null -> " All earned."
                        1 -> " The next one is one ${unitOf(f)} away."
                        else -> " The next one is $next ${unitOf(f)}s away."
                    }
                )
                VSpace(4)
            }
            VSpace(12)
        }
    }
}

private fun familyName(f: Family): String = when (f) {
    Family.BODY -> "Body"
    Family.ROOM -> "Room"
    Family.HEART -> "Heart"
    Family.WORLD -> "World"
}

private fun familyColour(f: Family) = when (f) {
    Family.BODY -> Ink.Body
    Family.ROOM -> Ink.Room
    Family.HEART -> Ink.Heart
    Family.WORLD -> Ink.Access
}

private fun unitOf(f: Family): String = if (f == Family.WORLD) "mission" else "day"

private fun sourceOf(family: String): String = when (family) {
    "BODY" -> "Earned by looking after your body."
    "ROOM" -> "Earned by looking after your room."
    "HEART" -> "Earned by looking after your heart."
    else -> "Earned on missions outside the bubble."
}
