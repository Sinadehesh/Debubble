package com.debubble.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Look
import com.debubble.app.engine.World
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.LookPicker
import com.debubble.app.ui.components.MeFigure
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SignHeader
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space

/**
 * The wardrobe: how you look, and the hats your levels have earned.
 *
 * The old avatar studio dressed a different character in a different style; this one dresses
 * the you that lives in your room, so what you earn shows up where you see yourself every
 * day. Locked hats are shown with the level they arrive at, because a reward you can see
 * coming is one you work towards (and a locked one you cannot see is not a reward at all).
 */
@Composable
fun WardrobeScreen(
    art: RoomArt,
    look: Look,
    level: Int,
    onLook: (Look) -> Unit,
    onBack: () -> Unit
) {
    val hats = art.catalogue.hats
    Column(modifier = Modifier.fillMaxSize().background(Ink.Void)) {
        TopBar(title = "Wardrobe", subtitle = "Level $level", onBack = onBack)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(fill = Ink.Room.copy(alpha = 0.14f), border = Ink.Room.copy(alpha = 0.5f))
                    .padding(vertical = 18.dp),
                contentAlignment = Alignment.Center
            ) {
                MeFigure(art, look, pixelScale = pixelScale(6f))
            }

            SignHeader(art, hats.firstOrNull()?.id ?: World.SPARK, "Hats", trailing = "${World.hatsAt(art.catalogue, level).size} of ${hats.size}")
            // Rows of three: none, then each hat in the order it arrives.
            val tiles = listOf<Pair<Int, String?>>(0 to null) + hats.mapIndexed { i, h -> (i + 1) to h.id }
            tiles.chunked(3).forEach { row ->
                Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
                    row.forEach { (index, id) ->
                        val def = hats.getOrNull(index - 1)
                        val open = def == null || def.level <= level
                        val on = look.hat == index
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(Space.radius))
                                .background(if (on) Ink.SurfaceHigh else Ink.Surface)
                                .border(
                                    BorderStroke(if (on) 2.dp else 1.dp, if (on) Ink.Gold else Ink.Border),
                                    RoundedCornerShape(Space.radius)
                                )
                                .clickable(role = Role.RadioButton, enabled = open) { onLook(look.copy(hat = index)) }
                                .semantics {
                                    selected = on
                                    contentDescription = when {
                                        def == null -> "No hat"
                                        open -> def.label
                                        else -> "${def.label}, arrives at level ${def.level}"
                                    }
                                }
                                .padding(8.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterVertically)
                        ) {
                            if (id != null) {
                                SpriteIcon(art, id, pixelScale = pixelScale(2.4f), modifier = Modifier.alpha(if (open) 1f else 0.35f))
                            } else {
                                Text("None", color = Ink.Secondary, style = MaterialTheme.typography.labelLarge)
                            }
                            Label(
                                when {
                                    def == null -> ""
                                    open -> def.label
                                    else -> "Level ${def.level}"
                                },
                                color = if (open) Ink.Primary else Ink.Muted
                            )
                        }
                    }
                    repeat(3 - row.size) { Box(modifier = Modifier.weight(1f)) }
                }
            }
            Label("Levels come from everything you do: looking after yourself, missions, and every spark of courage.")

            SignHeader(art, World.TAB_HOME, "How you look")
            LookPicker(art = art, look = look, onChange = onLook, preview = false)
        }
    }
}
