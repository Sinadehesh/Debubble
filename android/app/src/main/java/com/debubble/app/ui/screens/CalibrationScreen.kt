package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.debubble.app.engine.Needs
import com.debubble.app.engine.Pillar
import com.debubble.app.ui.components.Dot
import com.debubble.app.ui.components.Instrument
import com.debubble.app.ui.components.litSurface
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.tierCode
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.InstrumentFamily
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * Baseline Calibration.
 *
 * Six cards, one question each, all answerable with a thumb. No account wall, no
 * value-prop carousel, no long text input. The only job is finding where tier 1 sits for
 * this specific person — and then saying so plainly on the result card.
 */
@Composable
fun CalibrationScreen(
    initial: Baseline,
    isRecalibration: Boolean,
    onDone: (Baseline) -> Unit,
    onCancel: (() -> Unit)? = null
) {
    var step by remember { mutableIntStateOf(0) }

    var transport by remember { mutableStateOf(initial.transport) }
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
        transport = transport,
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
            .padding(horizontal = Space.gutter)
            .padding(top = 14.dp, bottom = Space.gutter)
    ) {
        // Progress is the only chrome.
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            repeat(cards + 1) { i ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(2.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i <= step) Ink.Primary else Ink.Faint)
                )
            }
        }

        VSpace(22)

        when (step) {
            0 -> QuestionCard(Pillar.ACCESS, "What moves you?", "Everything you can use without asking permission or paying a fare you cannot afford.") {
                ChipGrid(
                    options = listOf(
                        Needs.TRANSIT to "Public transit",
                        Needs.BIKE to "Bicycle",
                        Needs.CAR to "Car or moto"
                    ),
                    selected = transport,
                    accent = Pillar.ACCESS.accent,
                    onToggle = { key ->
                        transport = if (key in transport) transport - key else transport + key
                    }
                )
                VSpace(14)
                Instrument("On foot is assumed — it is always available", small = true)
            }

            1 -> QuestionCard(Pillar.ACCESS, "How far did you actually get this month?", "Furthest point from home in the last thirty days. Guess honestly; the engine only gets this wrong once.") {
                NumberSlider(
                    value = radius, range = 0..60, step = 1,
                    unit = "km from home", accent = Pillar.ACCESS.accent,
                    display = { if (it >= 60) "60+" else "$it" },
                    onChange = { radius = it }
                )
            }

            2 -> QuestionCard(Pillar.ACTIVITY, "How much of this week repeated last week?", "Same rooms, same order, same hours. Predictability is not a moral failure — it is a starting coordinate.") {
                NumberSlider(
                    value = routine, range = 0..100, step = 5,
                    unit = "% identical", accent = Pillar.ACTIVITY.accent,
                    onChange = { routine = it }
                )
                VSpace(26)
                Instrument("Last time you tried something new", small = true)
                VSpace(9)
                ChipGrid(
                    options = listOf(
                        "4" to "This week",
                        "3" to "This month",
                        "2" to "This year",
                        "1" to "Can't remember"
                    ),
                    selected = setOf(novelty.toString()),
                    accent = Pillar.ACTIVITY.accent,
                    onToggle = { novelty = it.toInt() }
                )
            }

            3 -> QuestionCard(Pillar.SOCIAL, "Talking to a stranger feels…", "Zero is effortless. Ten is physically hard. Nobody but you ever sees this number.") {
                NumberSlider(
                    value = resistance, range = 0..10, step = 1,
                    unit = "resistance", accent = Pillar.SOCIAL.accent,
                    onChange = { resistance = it }
                )
                VSpace(26)
                Instrument("Conversations over five minutes this week", small = true)
                VSpace(9)
                NumberSlider(
                    value = conversations, range = 0..20, step = 1,
                    unit = "conversations", accent = Pillar.SOCIAL.accent,
                    display = { if (it >= 20) "20+" else "$it" },
                    onChange = { conversations = it }
                )
            }

            4 -> QuestionCard(null, "What can you actually give?", "This caps what you will be served. Under-promise — the ladder works better when the daily ask is genuinely doable.") {
                NumberSlider(
                    value = capacity, range = 10..180, step = 5,
                    unit = "minutes on an ordinary day", accent = Ink.Primary,
                    onChange = { capacity = it }
                )
                VSpace(26)
                Instrument("Spend per challenge, without it being a problem", small = true)
                VSpace(9)
                NumberSlider(
                    value = budget, range = 0..100, step = 5,
                    unit = "per challenge", accent = Ink.Primary,
                    display = { if (it == 0) "Nothing" else "$it" },
                    onChange = { budget = it }
                )
            }

            5 -> QuestionCard(null, "Two hard limits", "Later tiers involve being away from home. If either of these is a no, the engine routes around it permanently.") {
                ToggleRow("I can be away overnight", canStayOut) { canStayOut = it }
                VSpace(9)
                ToggleRow("I have a valid passport", hasPassport) { hasPassport = it }
                VSpace(18)
                Instrument("Both can be changed later from your profile", small = true)
            }

            else -> ResultCard(baseline, isRecalibration)
        }

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            PrimaryButton(
                text = when {
                    step >= cards -> if (isRecalibration) "Update the ladder" else "Begin day 001"
                    step == cards - 1 -> "Compute baseline"
                    else -> "Continue"
                }
            ) {
                if (step >= cards) onDone(baseline) else step++
            }
            if (step > 0) {
                GhostButton("Back") { step-- }
            } else if (onCancel != null) {
                GhostButton("Cancel") { onCancel() }
            }
        }
    }
}

@Composable
private fun ColumnScope.QuestionCard(
    pillar: Pillar?,
    question: String,
    hint: String,
    body: @Composable () -> Unit
) {
    Column(modifier = Modifier.weight(1f)) {
        Instrument(
            text = pillar?.let { "${it.display} · ${it.dimension}" } ?: "Calibration",
            color = pillar?.accent ?: Ink.Dim
        )
        VSpace(10)
        Text(
            text = question,
            color = Ink.Primary,
            style = MaterialTheme.typography.headlineLarge
        )
        VSpace(10)
        Text(
            text = hint,
            color = Ink.Dim,
            style = MaterialTheme.typography.bodyMedium
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 22.dp),
            verticalArrangement = Arrangement.Center
        ) {
            body()
        }
    }
}

@Composable
private fun ColumnScope.ResultCard(
    baseline: Baseline,
    isRecalibration: Boolean
) {
    val tiers = Calibration.entryTiers(baseline)
    Column(modifier = Modifier.weight(1f)) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.Bottom) {
            Instrument(if (isRecalibration) "Recalibrated" else "Calibration complete")
            VSpace(12)
            Text(
                text = "Your bubble,\nmeasured.",
                color = Ink.Primary,
                style = MaterialTheme.typography.displayMedium
            )
            VSpace(10)
            Text(
                text = "These are entry tiers, not scores. Nobody starts at zero, and nobody " +
                    "is served a challenge they told us they cannot reach.",
                color = Ink.Dim,
                style = MaterialTheme.typography.bodyMedium
            )
            VSpace(24)
            Pillar.order.forEach { p ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Dot(p.accent)
                    Instrument(p.display, color = Ink.Ash, modifier = Modifier.weight(1f))
                    Text(
                        text = "TIER ${tierCode(tiers.getValue(p))}",
                        color = p.accent,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
                Divider()
            }
            VSpace(20)
        }
    }
}

@Composable
private fun ChipGrid(
    options: List<Pair<String, String>>,
    selected: Set<String>,
    accent: Color,
    onToggle: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(Space.gap)) {
        options.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(Space.gap)) {
                row.forEach { (key, label) ->
                    val on = key in selected
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .litSurface(tint = accent, emphasis = if (on) 2.4f else 0.6f)
                            .toggleable(
                                value = on,
                                role = Role.Checkbox,
                                onValueChange = { onToggle(key) }
                            )
                            .sizeIn(minHeight = 48.dp)
                            .wrapContentHeight(Alignment.CenterVertically)
                            .padding(vertical = 15.dp, horizontal = 12.dp)
                    ) {
                        Instrument(label, color = if (on) Ink.Primary else Ink.Ash, small = true)
                    }
                }
                if (row.size == 1) Box(modifier = Modifier.weight(1f))
            }
        }
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
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = display(value),
                color = accent,
                style = MaterialTheme.typography.displayMedium.copy(
                    fontFamily = InstrumentFamily,
                    fontWeight = FontWeight.Bold
                )
            )
            Instrument(unit, small = true)
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

@Composable
private fun ToggleRow(label: String, on: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .litSurface(emphasis = if (on) 1.6f else 0.55f)
            .toggleable(
                value = on,
                role = Role.Switch,
                onValueChange = onChange
            )
            .sizeIn(minHeight = 48.dp)
            .wrapContentHeight(Alignment.CenterVertically)
            .padding(horizontal = 15.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Instrument(label, color = if (on) Ink.Primary else Ink.Ash, small = true)
        Instrument(if (on) "Yes" else "No", color = if (on) Ink.Activity else Ink.Dim, small = true)
    }
}

@Composable
internal fun PrimaryButton(text: String, onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        shape = RoundedCornerShape(Space.radiusLarge),
        colors = ButtonDefaults.buttonColors(
            containerColor = Ink.Primary,
            contentColor = Ink.Void
        )
    ) {
        Text(text = text.uppercase(), style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun GhostButton(text: String, colour: Color = Ink.Dim, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        shape = RoundedCornerShape(Space.radiusLarge)
    ) {
        Text(text = text.uppercase(), color = colour, style = MaterialTheme.typography.labelMedium)
    }
}
