package com.debubble.app.ui.components

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.Offset
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Look
import com.debubble.app.engine.Reminder
import com.debubble.app.engine.ReminderChoice
import com.debubble.app.engine.Reminders
import com.debubble.app.engine.Scene
import com.debubble.app.engine.World
import com.debubble.app.notify.Reminding
import com.debubble.app.ui.theme.Cut
import com.debubble.app.ui.theme.fill
import com.debubble.app.ui.theme.wash
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import java.time.LocalTime
import kotlin.math.roundToInt

/* ==================================================================== paper

   Letters and postcards: the same paper as every card, but written on — an envelope flap,
   a seal, a postmark — so a mission still reads as something that arrived.               */

/** A sheet of paper, standing off the wall like a card. */
fun Modifier.paper(
    shape: Shape = Cut.card,
    fill: Color = Ink.Paper
): Modifier = this.panel(shape = shape, fill = fill)

/** What a letter says. Heavy and round: it is a sentence, so it is never set in pixels. */
@Composable
fun PaperTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        modifier = modifier.semantics { heading() },
        color = Ink.PaperText,
        style = MaterialTheme.typography.headlineSmall
    )
}

@Composable
fun PaperText(text: String, modifier: Modifier = Modifier, muted: Boolean = false) {
    Text(
        text = text,
        modifier = modifier,
        color = if (muted) Ink.PaperMuted else Ink.PaperText,
        style = MaterialTheme.typography.bodyLarge
    )
}

@Composable
fun PaperLabel(text: String, modifier: Modifier = Modifier, color: Color = Ink.PaperMuted) {
    Text(text = text, modifier = modifier, color = color, style = MaterialTheme.typography.labelLarge)
}

/**
 * A letter: an envelope flap across the top with a wax seal in the mission's colour, then
 * whatever is inside. The same letter you are holding in the room.
 */
@Composable
fun LetterCard(
    kicker: String,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = modifier.fillMaxWidth().paper()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(40.dp)
                .drawBehind {
                    // The flap, stepped in whole pixels like the letter you hold in the room.
                    val px = 3.dp.toPx().roundToInt().toFloat()
                    val rows = (size.height / px).toInt()
                    for (r in 0 until rows) {
                        val inset = (size.width / 2f) * (r / rows.toFloat())
                        drawRect(Ink.PaperShade, topLeft = Offset(inset, r * px), size = Size(size.width - inset * 2, px))
                        drawRect(Ink.PaperEdge, topLeft = Offset(inset, r * px), size = Size(px, px))
                        drawRect(Ink.PaperEdge, topLeft = Offset(size.width - inset - px, r * px), size = Size(px, px))
                    }
                },
            contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .offset(y = 12.dp)
                    .size(26.dp)
                    .clip(Cut.small)
                    .background(accent.fill)
                    .border(BorderStroke(2.dp, Ink.PaperText), Cut.small),
                contentAlignment = Alignment.Center
            ) {
                Glyph(Glyphs.Spark, colour = Ink.PaperText, size = 14)
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp)
                .padding(top = 22.dp, bottom = 18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            PaperLabel(kicker)
            content()
        }
    }
}

/* ==================================================================== done, or when

   The bottom of every task: a hold for "I did it", and beside it, a bell for "then when?".
   "Not now" is the moment a plan is most useful, so it is the one place the app offers to
   make one, instead of leaving a task to quietly drop out of the day.                     */

@Composable
fun DoneBar(
    art: RoomArt,
    accent: Color,
    /** "Today 19:00" once one is set, else null. */
    reminder: String?,
    onRemind: () -> Unit,
    holdLabel: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier
                .width(92.dp)
                .heightIn(min = 64.dp)
                .panel(
                    fill = if (reminder != null) Ink.Gold.wash else Ink.Surface,
                    border = Ink.Border,
                    borderWidth = if (reminder != null) 3.dp else 2.dp,
                    shadow = Ink.Border
                )
                .clickable(role = Role.Button, onClick = onRemind)
                .semantics {
                    contentDescription = reminder?.let { "Reminder at $it. Change it." } ?: "Remind me later"
                }
                .padding(horizontal = 6.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp, Alignment.CenterVertically)
        ) {
            SpriteIcon(art, World.BELL, pixelScale = pixelScale(2f))
            Text(
                text = reminder ?: "Remind me",
                color = if (reminder != null) Ink.Gold else Ink.Primary,
                style = MaterialTheme.typography.labelLarge,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
        HoldBar(accent = accent, label = holdLabel, modifier = Modifier.weight(1f), onCommit = onDone)
    }
}

/** Everything a task needs to offer "Remind me", gathered by whoever knows the state. */
class RemindHost(
    /** What it is, as the sheet's subtitle: "Wash up". */
    val what: String,
    val current: Reminder?,
    val choices: List<ReminderChoice>,
    val today: Long,
    val h24: Boolean,
    val dailyOption: String?,
    val dailyFirst: Boolean,
    val onSet: (day: Long, minute: Int, daily: Boolean) -> Unit,
    val onRemove: () -> Unit
)

/** [DoneBar] with its [RemindSheet]: the bottom of every task. */
@Composable
fun TaskFooter(
    art: RoomArt,
    accent: Color,
    host: RemindHost,
    holdLabel: String,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    var open by remember { mutableStateOf(false) }
    DoneBar(
        art = art,
        accent = accent,
        reminder = host.current?.let { Reminders.short(it, host.today, host.h24) },
        onRemind = { open = true },
        holdLabel = holdLabel,
        onDone = onDone,
        modifier = modifier
    )
    if (open) {
        RemindSheet(
            art = art,
            what = host.what,
            choices = host.choices,
            current = host.current,
            today = host.today,
            h24 = host.h24,
            dailyOption = host.dailyOption,
            dailyFirst = host.dailyFirst,
            onSet = { day, minute, daily ->
                open = false
                host.onSet(day, minute, daily)
            },
            onRemove = {
                open = false
                host.onRemove()
            },
            onDismiss = { open = false }
        )
    }
}

/**
 * Pick when. Four quick times, "every day" where it makes sense, or any time at all.
 *
 * One tap sets it and closes: every extra step between "not now" and "then at seven" is a
 * step at which the plan does not get made. Asks for the notification permission the first
 * time one is set, which is when the reason for it is obvious, rather than at launch.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemindSheet(
    art: RoomArt,
    what: String,
    choices: List<ReminderChoice>,
    current: Reminder?,
    today: Long,
    h24: Boolean,
    /** The label for the repeating option, or null where only a one-off makes sense. */
    dailyOption: String?,
    dailyFirst: Boolean,
    onSet: (day: Long, minute: Int, daily: Boolean) -> Unit,
    onRemove: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val sheet = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var daily by remember { mutableStateOf(dailyOption != null && (current?.daily ?: dailyFirst)) }
    var picking by remember { mutableStateOf(false) }
    val ask = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun set(day: Long, minute: Int) {
        if (Reminding.needsPermission(context)) ask.launch(Manifest.permission.POST_NOTIFICATIONS)
        onSet(day, minute, daily)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheet,
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpriteIcon(art, World.BELL, pixelScale = pixelScale(3f))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "When will you do it?",
                        modifier = Modifier.semantics { heading() },
                        color = Ink.Primary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Label(what)
                }
            }

            if (current != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .panel(fill = Ink.Gold.wash, lift = 0.dp)
                        .padding(start = 14.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Label(
                        "Set for ${Reminders.label(current, today, h24)}",
                        modifier = Modifier.weight(1f),
                        color = Ink.Gold,
                        strong = true
                    )
                    Box(modifier = Modifier.width(110.dp)) {
                        BlockButton("Remove", fill = Ink.Surface, small = true, onClick = onRemove)
                    }
                }
            }

            if (dailyOption != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = Space.tap)
                        .panel()
                        .clickable(role = Role.Switch) { daily = !daily }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(dailyOption, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                        Label(if (daily) "At the same time, until you turn it off" else "Just this once")
                    }
                    PixelSwitch(checked = daily)
                }
            }

            val shown = if (daily) choices.distinctBy { it.minute }.sortedBy { it.minute } else choices
            shown.forEach { c ->
                OptionRow(
                    label = if (daily) "Every day · ${Reminders.clock(c.minute, h24)}" else c.label,
                    onClick = { set(if (daily) today else c.day, c.minute) }
                )
            }
            OptionRow(label = "Pick a time…", onClick = { picking = true })

            Label(
                if (!Reminding.canNotify(context) && !Reminding.needsPermission(context)) {
                    "Notifications are off for DeBubble in your phone's settings, so this will " +
                        "show in the app and on your widget, but not as a notification."
                } else {
                    "You get a notification then. Tap it to open this; \"In 30 min\" puts it off once."
                }
            )
        }
    }

    if (picking) {
        val now = LocalTime.now()
        TimePickDialog(
            h24 = h24,
            initialMinute = ((now.hour + 1) % 24) * 60,
            onPick = { minute ->
                picking = false
                val nowMinute = now.hour * 60 + now.minute
                set(if (daily || minute > nowMinute) today else today + 1, minute)
            },
            onDismiss = { picking = false }
        )
    }
}

/** One option in a sheet: a full-width row you tap once. */
@Composable
fun OptionRow(label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .panel(fill = Ink.SurfaceHigh)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Ink.Primary, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        Glyph(Glyphs.ArrowRight, colour = Ink.Primary, size = 16)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickDialog(h24: Boolean, initialMinute: Int, onPick: (Int) -> Unit, onDismiss: () -> Unit) {
    val state = rememberTimePickerState(
        initialHour = initialMinute / 60,
        initialMinute = initialMinute % 60,
        is24Hour = h24
    )
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            Box(modifier = Modifier.width(110.dp)) {
                BlockButton("Set", small = true, onClick = { onPick(state.hour * 60 + state.minute) })
            }
        },
        dismissButton = {
            Box(modifier = Modifier.width(110.dp)) {
                BlockButton("Cancel", fill = Ink.Surface, small = true, onClick = onDismiss)
            }
        },
        title = { Text("Pick a time", color = Ink.Primary, style = MaterialTheme.typography.headlineMedium) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = Ink.Well,
                    selectorColor = Ink.Primary,
                    containerColor = Ink.Surface,
                    timeSelectorSelectedContainerColor = Ink.Gold.fill,
                    timeSelectorUnselectedContainerColor = Ink.Well,
                    timeSelectorSelectedContentColor = Ink.Primary,
                    timeSelectorUnselectedContentColor = Ink.Primary,
                    periodSelectorSelectedContainerColor = Ink.Gold.fill,
                    periodSelectorBorderColor = Ink.Border
                )
            )
        },
        shape = Cut.large,
        containerColor = Ink.Surface
    )
}

/* ==================================================================== the little helpers */

/**
 * A first-visit tip, said by your heart buddy. Just-in-time rather than all up front: an
 * explanation lands when it is next to the thing it explains and you are about to use it.
 */
@Composable
fun PageTip(
    art: RoomArt,
    title: String,
    body: String,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .panel(fill = Ink.Heart.wash)
            .padding(start = 14.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SpriteIcon(art, Scene.buddy(0), pixelScale = pixelScale(2.4f), modifier = Modifier.padding(top = 2.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
            Text(body, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Box(modifier = Modifier.width(120.dp)) {
                    BlockButton("Got it", fill = Ink.Surface, small = true, onClick = onDismiss)
                }
            }
        }
    }
}

/** Your head, in your look and your hat: the "You" tab, and anywhere you are named. */
@Composable
fun MeHead(art: RoomArt, look: Look, pixelScale: Int, modifier: Modifier = Modifier) {
    val r = art.rect(Scene.meFrame(0)) ?: return
    val rows = HEAD_ROWS.coerceAtMost(r[3])
    val hat = art.catalogue.hat(look)?.let { art.rect(it.id) }
    val sheet = art.sheet(look)
    val atlas = art.atlas
    val density = LocalDensity.current
    val w = with(density) { (r[2] * pixelScale).toDp() }
    val h = with(density) { (rows * pixelScale).toDp() }
    Canvas(modifier = modifier.size(w, h)) {
        drawImage(
            image = sheet,
            srcOffset = IntOffset(r[0], r[1]),
            srcSize = IntSize(r[2], rows),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(r[2] * pixelScale, rows * pixelScale),
            filterQuality = FilterQuality.None
        )
        if (hat != null) {
            val hh = hat[3].coerceAtMost(rows)
            drawImage(
                image = atlas,
                srcOffset = IntOffset(hat[0], hat[1]),
                srcSize = IntSize(hat[2], hh),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(hat[2] * pixelScale, hh * pixelScale),
                filterQuality = FilterQuality.None
            )
        }
    }
}

/** Hair and face: the top of the 23-row frame, down to the chin. */
private const val HEAD_ROWS = 16

/** All of you, in your look and your hat: the You tab and the wardrobe. */
@Composable
fun MeFigure(art: RoomArt, look: Look, pixelScale: Int, modifier: Modifier = Modifier, frame: Int = 0) {
    val r = art.rect(Scene.meFrame(frame)) ?: return
    val hat = art.catalogue.hat(look)?.let { art.rect(it.id) }
    val sheet = art.sheet(look)
    val atlas = art.atlas
    val density = LocalDensity.current
    val w = with(density) { (r[2] * pixelScale).toDp() }
    val h = with(density) { (r[3] * pixelScale).toDp() }
    Canvas(modifier = modifier.size(w, h)) {
        drawImage(
            image = sheet,
            srcOffset = IntOffset(r[0], r[1]),
            srcSize = IntSize(r[2], r[3]),
            dstOffset = IntOffset.Zero,
            dstSize = IntSize(r[2] * pixelScale, r[3] * pixelScale),
            filterQuality = FilterQuality.None
        )
        if (hat != null) {
            drawImage(
                image = atlas,
                srcOffset = IntOffset(hat[0], hat[1]),
                srcSize = IntSize(hat[2], hat[3]),
                dstOffset = IntOffset.Zero,
                dstSize = IntSize(hat[2] * pixelScale, hat[3] * pixelScale),
                filterQuality = FilterQuality.None
            )
        }
    }
}

/** A section heading with its pixel sign beside it. */
@Composable
fun SignHeader(
    art: RoomArt,
    icon: String,
    title: String,
    modifier: Modifier = Modifier,
    trailing: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteIcon(art, icon, pixelScale = pixelScale(2f))
        Text(
            text = title,
            modifier = Modifier.weight(1f).semantics { heading() },
            color = Ink.Primary,
            style = MaterialTheme.typography.titleLarge
        )
        if (trailing != null) Label(trailing)
    }
}

/** The courage jar, with how many sparks are in it and how many until it looks fuller. */
@Composable
fun CourageJar(art: RoomArt, friction: Int, modifier: Modifier = Modifier) {
    val next = World.toNextSpark(friction)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .panel(fill = Ink.Ember.wash)
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteIcon(art, World.jar(friction), pixelScale = pixelScale(4f))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                SpriteIcon(art, World.SPARK, pixelScale = pixelScale(1.6f))
                Text(
                    text = if (friction == 1) "1 spark of courage" else "$friction sparks of courage",
                    color = Ink.Ember,
                    style = MaterialTheme.typography.titleLarge
                )
            }
            Text(
                text = "One for every time something did not go to plan and you said so: a no, " +
                    "a silence, turning back. Each is proof you went to the edge.",
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
            if (next != null) Label(if (next == 1) "1 more and the jar fills up a bit." else "$next more and the jar fills up a bit.")
        }
    }
}

/**
 * Where you are in setting up: four named steps, the one you are on lit, said by your heart
 * buddy. Setup is a run of questions before anything fun happens, and people finish a
 * sequence they can see the end of.
 */
@Composable
fun SetupSteps(art: RoomArt, step: Int, modifier: Modifier = Modifier) {
    val names = listOf("About you", "Habits", "Your journey", "Move in")
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.gutter, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteIcon(art, Scene.buddy(0), pixelScale = pixelScale(2f))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Label(
                "Setting up · ${step.coerceIn(1, names.size)} of ${names.size} · ${names[(step - 1).coerceIn(0, names.lastIndex)]}",
                color = Ink.Heart,
                strong = true
            )
            Ticks(total = names.size, done = step.coerceIn(0, names.size), accent = Ink.Heart, height = 10)
        }
    }
}

/** The grip at the top of a sheet: a short plum bar, in pixels. */
@Composable
fun SheetHandle() {
    Box(
        modifier = Modifier
            .padding(top = 12.dp, bottom = 8.dp)
            .size(width = 44.dp, height = 6.dp)
            .background(Ink.Border)
    )
}

/**
 * The top of a tab: its sign, its name in the pixel face, a line about it, and anything
 * that belongs in the corner. Home, Outside, Journal, Library and You all open the same way.
 */
@Composable
fun PageHeader(
    art: RoomArt,
    icon: String,
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null,
    sign: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Space.gutter)
            .padding(top = 14.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .panel(fill = Ink.Surface, lift = 2.dp),
            contentAlignment = Alignment.Center
        ) {
            if (sign != null) sign() else SpriteIcon(art, icon, pixelScale = pixelScale(2.2f))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                modifier = Modifier.semantics { heading() },
                color = Ink.Primary,
                style = MaterialTheme.typography.headlineMedium
            )
            if (subtitle != null) Label(subtitle)
        }
        trailing?.invoke()
    }
}
