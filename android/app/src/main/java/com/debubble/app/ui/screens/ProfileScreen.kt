package com.debubble.app.ui.screens

import android.Manifest
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.data.AppState
import com.debubble.app.engine.BudgetTier
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Progress
import com.debubble.app.engine.World
import com.debubble.app.notify.Reminding
import com.debubble.app.ui.components.CheckBox
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.MeFigure
import com.debubble.app.ui.components.PageTip
import com.debubble.app.ui.components.ProgressTrack
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.SelectRow
import com.debubble.app.ui.components.SignHeader
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.StatTile
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.roomGround

/**
 * You: the character you made, what your level has earned, and the settings.
 *
 * Everything about progress outside — the roads, the jar, the guesses, the history — moved
 * to Outside, where it is drawn as the world. What stays here is about you as a person using
 * the app: how you look, how often you have shown up, and how the app treats you.
 *
 * "Days you showed up" replaces a streak. It only goes up; one missed day did not set
 * anyone's habits back in Lally et al. (2010), and a number that can be lost teaches people
 * to stop after they lose it.
 */
@Composable
fun ProfileScreen(
    art: RoomArt,
    state: AppState,
    dayIndex: Long,
    showTip: Boolean,
    onTipSeen: () -> Unit,
    onOpenWardrobe: () -> Unit,
    onRecalibrate: () -> Unit,
    onChangeGoal: () -> Unit,
    onOpenAudit: () -> Unit,
    onSetBudget: (BudgetTier) -> Unit,
    onToggleSound: () -> Unit,
    onToggleAmbient: () -> Unit,
    /** Whether a widget is already on a home screen, which changes the wording only. */
    widgetPlaced: Boolean,
    onAddWidget: () -> Unit,
    /** The first-day tour of the home screen, again. */
    onReplayTour: () -> Unit
) {
    val nextHat = World.nextHat(art.catalogue, state.level)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(roomGround())
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Space.gutter)
            .padding(top = 18.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(Space.gap)
    ) {
        if (showTip) {
            PageTip(
                art = art,
                title = "This is you",
                body = "Levels come from everything you do, and new levels bring hats. The " +
                    "tour, your reminders and your settings are here too.",
                onDismiss = onTipSeen
            )
        }

        // You, large, in your own room's light.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .panel(fill = Ink.Room.copy(alpha = 0.12f), border = Ink.Room.copy(alpha = 0.55f))
                .clickable(role = Role.Button, onClick = onOpenWardrobe)
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MeFigure(art, state.home.look, pixelScale = pixelScale(4f))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                Text(
                    text = "Level ${state.level}",
                    modifier = Modifier.semantics { heading() },
                    color = Ink.Primary,
                    style = MaterialTheme.typography.headlineMedium
                )
                ProgressTrack(state.levelProgress, Ink.Gold, height = 8)
                Label("${Progress.xpIntoLevel(state.xp)} / ${Progress.XP_PER_LEVEL} to level ${state.level + 1}")
                if (nextHat != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        SpriteIcon(art, nextHat.id, pixelScale = pixelScale(1.2f))
                        Label("${nextHat.label} at level ${nextHat.level}")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Label("Wardrobe", color = Ink.Gold, strong = true)
                    Glyph(Glyphs.ArrowRight, colour = Ink.Gold, size = 15)
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
            StatTile("Day", "$dayIndex", Modifier.weight(1f))
            StatTile("Showed up", "${state.daysShowedUp}", Modifier.weight(1f), accent = Ink.Activity)
            StatTile("Sparks", "${state.friction}", Modifier.weight(1f), accent = Ink.Ember)
        }

        // The tour promises it is here, so it is near the top rather than under settings.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Space.tap)
                .panel()
                .clickable(role = Role.Button, onClick = onReplayTour)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            SpriteIcon(art, World.BOOK, pixelScale = pixelScale(1.8f))
            Column(modifier = Modifier.weight(1f)) {
                Text("How it works", color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                Label("Take the tour of your home screen again")
            }
            Glyph(Glyphs.ArrowRight, colour = Ink.Muted, size = 18)
        }

        VSpace(14)
        SignHeader(art, World.BELL, "Reminders")
        Notifications()
        if (state.reminders.isNotEmpty()) {
            Label(
                "${state.reminders.size} set. Change one from its task: the bell beside \"I did it\"."
            )
        }

        VSpace(14)
        SignHeader(art, World.TAB_HOME, "Home screen")
        Column(
            modifier = Modifier.fillMaxWidth().panel().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (widgetPlaced) {
                    "Your room and today's four are on your home screen, with the times you " +
                        "picked, so they are in front of you when the moment arrives."
                } else {
                    "Your room and today's four, on your home screen. A plan only works if " +
                        "you see it when the moment turns up, and an app has to be opened first."
                },
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
            SecondaryButton(if (widgetPlaced) "Add another" else "Add the widget", onClick = onAddWidget)
        }

        VSpace(14)
        SignHeader(art, World.TAB_LIBRARY, "Your audit", trailing = "${state.debuffs.size} marked")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = Space.tap)
                .panel()
                .clickable(role = Role.Button, onClick = onOpenAudit)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (state.debuffs.isEmpty()) "Run your systems audit"
                    else "${state.debuffs.size} habit${if (state.debuffs.size == 1) "" else "s"} to work on",
                    color = Ink.Primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Label(
                    if (state.debuffs.isEmpty()) "Mark habits that keep you inside, and get extras and reading for each."
                    else "Each one brings its own extras and reading, in turn."
                )
            }
            Glyph(Glyphs.ArrowRight, colour = Ink.Muted, size = 18)
        }

        VSpace(14)
        SignHeader(art, World.road(Pillar.ACTIVITY), "Budget", trailing = state.budget.display)
        Label("A hard limit. Nothing above it is ever suggested.")
        BudgetTier.all.forEach { t ->
            SelectRow(
                label = t.display,
                detail = t.detail,
                selected = state.budget == t,
                single = true,
                accent = Ink.Activity,
                onToggle = { onSetBudget(t) }
            )
        }

        VSpace(14)
        SignHeader(art, World.SPARK, "Sound")
        SettingRow(
            label = "Sound effects",
            detail = "Short sounds when you finish or log something",
            on = state.soundOn,
            onToggle = onToggleSound
        )
        SettingRow(
            label = "Background hum",
            detail = "A low drone while a letter is open. Never plays over music.",
            on = state.ambientOn,
            onToggle = onToggleAmbient
        )

        VSpace(14)
        SecondaryButton(if (state.goalEnum == null) "Pick a journey" else "Change your journey") { onChangeGoal() }
        SecondaryButton("Redo your setup") { onRecalibrate() }

        VSpace(10)
        Text(
            text = "Everything here stays on this phone. There is no account and nothing " +
                "is sent anywhere.",
            color = Ink.Muted,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

/**
 * Whether reminders can actually reach you, and the one button that fixes it if not: the
 * system dialog the first time, the app's notification settings after someone has said no.
 */
@Composable
private fun Notifications() {
    val context = LocalContext.current
    // Bumped after the dialog closes, so the row reads the answer.
    var asked by remember { mutableIntStateOf(0) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { asked++ }
    val on = remember(asked) { Reminding.canNotify(context) }
    val needs = remember(asked) { Reminding.needsPermission(context) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(border = if (on) Ink.Activity else Ink.Gold)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = if (on) "Notifications are on" else "Notifications are off",
            color = if (on) Ink.Activity else Ink.Gold,
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = if (on) "A reminder arrives at the time you picked, unless it's already done."
            else "Reminders still show in the app and on your widget, but not as notifications.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        if (!on) {
            SecondaryButton("Turn them on", colour = Ink.Gold) {
                if (needs) {
                    ask.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    runCatching {
                        context.startActivity(
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingRow(
    label: String,
    detail: String,
    on: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .panel(border = if (on) Ink.Activity else Ink.Border)
            .toggleable(value = on, role = Role.Switch, onValueChange = { onToggle() })
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckBox(selected = on, accent = Ink.Activity)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, color = Ink.Primary, style = MaterialTheme.typography.bodyLarge)
            Text(text = detail, color = Ink.Muted, style = MaterialTheme.typography.bodyMedium)
        }
        Box {
            Text(
                text = if (on) "On" else "Off",
                color = if (on) Ink.Activity else Ink.Muted,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
}
