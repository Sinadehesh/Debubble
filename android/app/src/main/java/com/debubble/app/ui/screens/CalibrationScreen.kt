package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Baseline
import com.debubble.app.engine.Calibration
import com.debubble.app.engine.Copy
import com.debubble.app.engine.Mobility
import com.debubble.app.engine.Pillar
import com.debubble.app.ui.components.CheckBox
import com.debubble.app.ui.components.Divider
import com.debubble.app.ui.components.Dot
import com.debubble.app.ui.components.Figure
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.SelectRow
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.selectablePanel
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * Setup. Six questions, then the result.
 *
 * The only job is working out where level 1 sits for this specific person, so every answer
 * feeds a number and nothing is asked for decoration.
 */
@Composable
fun CalibrationScreen(
    initial: Baseline,
    isRecalibration: Boolean,
    onDone: (Baseline) -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var step by remember { mutableIntStateOf(0) }

    var moves by remember { mutableStateOf(initial.modes) }
    var radius by remember { mutableIntStateOf(initial.radiusKm) }
    var routine by remember { mutableIntStateOf(initial.routinePct) }
    var novelty by remember { mutableIntStateOf(initial.noveltyRecency) }
    var resistance by remember { mutableIntStateOf(initial.socialResistance) }
    var conversations by remember { mutableIntStateOf(initial.longConversations) }
    var budget by remember { mutableIntStateOf(initial.budgetPerChallenge) }
    var capacity by remember { mutableIntStateOf(initial.capacityMinutes) }
    var canStayOut by remember { mutableStateOf(initial.canStayOut) }
    var hasPassport by remember { mutableStateOf(initial.hasPassport) }

    val baseline = Baseline(
        moves = moves,
        radiusKm = radius,
        routinePct = routine,
        noveltyRecency = novelty,
        socialResistance = resistance,
        longConversations = conversations,
        budgetPerChallenge = budget,
        capacityMinutes = capacity,
        canStayOut = canStayOut,
        hasPassport = hasPassport
    )

    val cards = 6

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
    ) {
        TopBar(
            title = if (isRecalibration) "Update your setup" else "Setting you up",
            subtitle = "Question ${(step + 1).coerceAtMost(cards + 1)} of ${cards + 1}",
            onBack = if (step > 0) ({ step-- }) else onCancel
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Space.gutter),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(cards + 1) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(4.dp)
                        .clip(RoundedCornerShape(50))
                        .background(if (i <= step) Ink.Primary else Ink.Faint)
                )
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(top = 20.dp, bottom = 12.dp)
        ) {
            when (step) {
                0 -> Question(
                    Pillar.ACCESS,
                    "How do you get around?",
                    "Pick everything that applies. This changes the wording of your " +
                        "challenges, so nothing tells you to do something you cannot do."
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
                        Mobility.all.forEach { key ->
                            SelectRow(
                                label = Mobility.label(key),
                                detail = Mobility.detail(key),
                                selected = key in moves,
                                accent = Pillar.ACCESS.accent,
                                onToggle = {
                                    moves = if (key in moves) moves - key else moves + key
                                }
                            )
                        }
                    }
                    if (moves.isEmpty()) {
                        VSpace(14)
                        Notice("Pick at least one, so we know how to describe things.")
                    } else if (Copy.rewrites(moves)) {
                        VSpace(14)
                        Notice(
                            "Got it. Your challenges will say " +
                                "\"${Copy.verb(moves).lowercase()}\" where they would " +
                                "otherwise say \"walk\"."
                        )
                    }
                }

                1 -> Question(
                    Pillar.ACCESS,
                    "How far from home have you been this month?",
                    "The furthest you got in the last thirty days. A rough guess is fine."
                ) {
                    NumberSlider(
                        value = radius, range = 0..60, step = 1,
                        unit = "km from home", accent = Pillar.ACCESS.accent,
                        display = { if (it >= 60) "60+" else "$it" },
                        onChange = { radius = it }
                    )
                }

                2 -> Question(
                    Pillar.ACTIVITY,
                    "How much of this week was the same as last week?",
                    "Same places, same order, same hours. There is no wrong answer — this " +
                        "is a starting point, not a judgement."
                ) {
                    NumberSlider(
                        value = routine, range = 0..100, step = 5,
                        unit = "% the same", accent = Pillar.ACTIVITY.accent,
                        onChange = { routine = it }
                    )
                    VSpace(26)
                    Label("When did you last try something new?", strong = true)
                    VSpace(10)
                    Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
                        listOf(
                            4 to "This week",
                            3 to "This month",
                            2 to "Some time this year",
                            1 to "I cannot remember"
                        ).forEach { (value, label) ->
                            SelectRow(
                                label = label,
                                selected = novelty == value,
                                single = true,
                                accent = Pillar.ACTIVITY.accent,
                                onToggle = { novelty = value }
                            )
                        }
                    }
                }

                3 -> Question(
                    Pillar.SOCIAL,
                    "How hard is it to talk to someone you don't know?",
                    "0 is easy. 10 is genuinely difficult. Only you ever see this."
                ) {
                    NumberSlider(
                        value = resistance, range = 0..10, step = 1,
                        unit = "out of 10", accent = Pillar.SOCIAL.accent,
                        onChange = { resistance = it }
                    )
                    VSpace(26)
                    Label("Real conversations this week, over five minutes", strong = true)
                    VSpace(10)
                    NumberSlider(
                        value = conversations, range = 0..20, step = 1,
                        unit = "conversations", accent = Pillar.SOCIAL.accent,
                        display = { if (it >= 20) "20+" else "$it" },
                        onChange = { conversations = it }
                    )
                }

                4 -> Question(
                    null,
                    "How much can you give on a normal day?",
                    "This caps what you get asked to do. Aim low — the app works better " +
                        "when the daily task is genuinely easy to fit in."
                ) {
                    NumberSlider(
                        value = capacity, range = 10..180, step = 5,
                        unit = "minutes a day", accent = Ink.Access,
                        onChange = { capacity = it }
                    )
                    VSpace(26)
                    Label("What can you spend, without it being a problem?", strong = true)
                    VSpace(10)
                    NumberSlider(
                        value = budget, range = 0..100, step = 5,
                        unit = "per challenge", accent = Ink.Access,
                        display = { if (it == 0) "Nothing" else "$it" },
                        onChange = { budget = it }
                    )
                }

                5 -> Question(
                    null,
                    "Two last things",
                    "Some later challenges involve being away from home. If either of " +
                        "these is a no, you will never be asked."
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
                        YesNoRow("I can stay away overnight", canStayOut) { canStayOut = it }
                        YesNoRow("I have a valid passport", hasPassport) { hasPassport = it }
                    }
                    VSpace(16)
                    Notice("You can change any of this later from your profile.")
                }

                else -> Result(baseline, isRecalibration)
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = Space.gutter, bottom = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PrimaryButton(
                text = when {
                    step >= cards -> if (isRecalibration) "Save changes" else "Start day 1"
                    else -> "Next"
                },
                enabled = step != 0 || moves.isNotEmpty()
            ) {
                if (step >= cards) onDone(baseline) else step++
            }
            if (step > 0) {
                TextAction("Back") { step-- }
            } else if (onCancel != null) {
                TextAction("Cancel") { onCancel() }
            }
        }
    }
}

@Composable
private fun ColumnScope.Question(
    pillar: Pillar?,
    question: String,
    hint: String,
    body: @Composable () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        if (pillar != null) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Dot(pillar.accent)
                Label(pillar.display, color = pillar.accent, strong = true)
            }
            VSpace(10)
        }
        Text(
            text = question,
            color = Ink.Primary,
            style = MaterialTheme.typography.headlineLarge
        )
        VSpace(10)
        Text(
            text = hint,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        VSpace(22)
        body()
    }
}

@Composable
private fun ColumnScope.Result(baseline: Baseline, isRecalibration: Boolean) {
    val tiers = Calibration.entryTiers(baseline)
    Column(modifier = Modifier.fillMaxWidth()) {
        Label(if (isRecalibration) "Updated" else "All set")
        VSpace(10)
        Text(
            text = "Here is where you start.",
            color = Ink.Primary,
            style = MaterialTheme.typography.displayMedium
        )
        VSpace(12)
        Text(
            text = "These are starting levels, not scores. Nobody begins at zero, and you " +
                "will never be given something you told us you cannot do.",
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
        VSpace(24)
        Pillar.order.forEach { p ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Dot(p.accent, size = 10)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = p.display,
                        color = Ink.Primary,
                        style = MaterialTheme.typography.labelLarge
                    )
                    Label(p.dimension)
                }
                Figure("Level ${tiers.getValue(p)}", color = p.accent)
            }
            Divider()
        }
    }
}

/** A plain informational strip. Never red, never an error — it is guidance. */
@Composable
private fun Notice(text: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.SurfaceHigh)
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = text,
            color = Ink.Secondary,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun NumberSlider(
    value: Int,
    range: IntRange,
    step: Int,
    unit: String,
    accent: Color,
    onChange: (Int) -> Unit,
    display: (Int) -> String = { it.toString() }
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Figure(display(value), color = accent, large = true)
            Label(unit)
        }
        val steps = ((range.last - range.first) / step) - 1
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = steps.coerceAtLeast(0),
            colors = SliderDefaults.colors(
                thumbColor = accent,
                activeTrackColor = accent,
                inactiveTrackColor = Ink.Faint,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            )
        )
    }
}

/** A yes/no with both states visible, rather than a switch whose off state is invisible. */
@Composable
private fun YesNoRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .selectablePanel(on, Ink.Activity)
            .toggleable(value = on, role = Role.Switch, onValueChange = onChange)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckBox(selected = on, accent = Ink.Activity)
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = Ink.Primary,
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Normal
            )
        )
        Text(
            text = if (on) "Yes" else "No",
            color = if (on) Ink.Activity else Ink.Muted,
            style = MaterialTheme.typography.labelLarge
        )
    }
}
