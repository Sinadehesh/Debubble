package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Cues
import com.debubble.app.engine.Forecast
import com.debubble.app.engine.Forecasts
import com.debubble.app.ui.components.Figure
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space

/**
 * Planning a challenge before doing it.
 *
 * Two things happen here and they are deliberately on one screen. The **if-then plan** is what
 * makes the behaviour actually happen — binding a concrete cue to a concrete response roughly
 * doubles execution rates, because it removes the decision from the moment of fear. The
 * **prediction** is what makes the doing teach something: without a recorded forecast, a
 * challenge that goes fine is remembered as "it was fine", and the catastrophic expectation
 * that preceded it is quietly forgotten rather than disconfirmed.
 *
 * The cue is assembled from an anchor rather than typed into an empty box, because an empty
 * box reliably produces "when I get a chance", which is a goal intention in costume and
 * performs like one.
 */
@Composable
fun PlanScreen(
    challengeId: String,
    title: String,
    onSave: (cue: String, response: String, predicted: String, distress: Int) -> Unit,
    onBack: () -> Unit
) {
    var anchor by remember { mutableStateOf(Cues.anchors.first()) }
    var detail by remember { mutableStateOf("") }
    var response by remember { mutableStateOf("") }
    var predicted by remember { mutableStateOf("") }
    var distress by remember { mutableIntStateOf(6) }

    val cue = if (detail.isBlank()) anchor else "$anchor, $detail"
    val cueProblem = Cues.critique(cue)
    val responseProblem = Cues.critiqueResponse(response)
    val ready = cueProblem == null && responseProblem == null && predicted.isNotBlank()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .imePadding()
    ) {
        TopBar(title = "Plan it", subtitle = title.take(48), onBack = onBack)

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(fill = Ink.SurfaceHigh)
                    .padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Glyph(Glyphs.Spark, colour = Ink.Gold, size = 18)
                Text(
                    text = "Deciding now, while you are calm, is the whole trick. In the " +
                        "moment itself you will not be deciding — you will just be doing the " +
                        "thing you already decided.",
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            VSpace(24)
            SectionHeader("When", trailing = "Pick a moment that will happen anyway")
            VSpace(12)

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Cues.anchors.forEach { a ->
                    val on = a == anchor
                    Box(
                        modifier = Modifier
                            .heightIn(min = Space.tap)
                            .panel(
                                shape = RoundedCornerShape(50),
                                fill = if (on) Ink.Access.copy(alpha = 0.18f) else Ink.Surface,
                                border = if (on) Ink.Access else Ink.Border,
                                borderWidth = if (on) 2.dp else 1.dp
                            )
                            .clickable(role = Role.RadioButton) { anchor = a }
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = a,
                            color = if (on) Ink.Primary else Ink.Secondary,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (on) FontWeight.Bold else FontWeight.Medium
                            )
                        )
                    }
                }
            }

            VSpace(12)
            Field(
                value = detail,
                onChange = { detail = it },
                placeholder = "Anything more exact? (optional)"
            )

            VSpace(24)
            SectionHeader("Then I will")
            VSpace(12)
            Field(
                value = response,
                onChange = { response = it },
                placeholder = "Say what you will actually do, in a few words",
                problem = responseProblem.takeIf { response.isNotBlank() }
            )

            VSpace(20)

            // The sentence itself, assembled. Seeing it whole is what makes it a commitment
            // rather than two half-filled text fields.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(
                        shape = RoundedCornerShape(Space.radiusLarge),
                        border = if (ready) Ink.Activity else Ink.Border
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Label("Your plan", color = if (ready) Ink.Activity else Ink.Muted, strong = true)
                Text(
                    text = "When $cue, I will ${response.ifBlank { "…" }}.",
                    color = Ink.Primary,
                    style = MaterialTheme.typography.titleMedium
                )
                if (cueProblem != null) Label(cueProblem, color = Ink.Ember)
            }

            VSpace(28)
            SectionHeader("What do you think will happen?")
            VSpace(6)
            Text(
                text = "Be specific, and let it be the bad version. You are going to read " +
                    "this back afterwards, and the gap is the point.",
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
            VSpace(12)
            Field(
                value = predicted,
                onChange = { predicted = it },
                placeholder = "\"They will look at me like I am strange and I will have to leave.\"",
                lines = 3
            )

            VSpace(24)
            DistressSlider(
                label = "How hard will it feel?",
                value = distress,
                accent = Ink.Ember,
                onChange = { distress = it }
            )

            VSpace(26)
        }

        Column(
            modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, bottom = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PrimaryButton(
                text = if (ready) "Lock it in" else "Fill in the plan and the prediction",
                accent = Ink.Activity,
                enabled = ready
            ) { onSave(cue, response, predicted, distress) }
            TextAction("Not now") { onBack() }
        }
    }
}

/**
 * Closing the loop afterwards.
 *
 * The prediction is shown back first, in the user's own words, before they are asked what
 * happened. That order matters: recalling the forecast *before* reporting the outcome is what
 * produces the disconfirmation, rather than a vague sense that it went alright.
 */
@Composable
fun ReviewScreen(
    forecast: Forecast,
    onResolve: (actual: String, distress: Int) -> Unit,
    onDismiss: () -> Unit,
    onBack: () -> Unit
) {
    var actual by remember { mutableStateOf("") }
    var distress by remember { mutableIntStateOf(4) }

    val violation = forecast.predictedDistress - distress

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .imePadding()
    ) {
        TopBar(
            title = "How did it go?",
            subtitle = forecast.challengeTitle.take(48),
            onBack = onBack
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(
                        shape = RoundedCornerShape(Space.radiusLarge),
                        fill = Ink.SurfaceHigh,
                        border = Ink.Ember.copy(alpha = 0.6f)
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Label("Before, you wrote", color = Ink.Ember, strong = true)
                Text(
                    text = "\"${forecast.predicted}\"",
                    color = Ink.Primary,
                    style = MaterialTheme.typography.bodyLarge
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Pill("${forecast.predictedDistress}/10", Ink.Ember)
                    Label(Forecasts.label(forecast.predictedDistress))
                }
            }

            VSpace(26)
            SectionHeader("What actually happened?")
            VSpace(12)
            Field(
                value = actual,
                onChange = { actual = it },
                placeholder = "Plainly. Including if it went badly.",
                lines = 3
            )

            VSpace(24)
            DistressSlider(
                label = "How hard was it really?",
                value = distress,
                accent = Ink.Access,
                onChange = { distress = it }
            )

            VSpace(20)

            // Live, so the gap is visible while the number is still being chosen.
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .panel(
                        shape = RoundedCornerShape(Space.radiusLarge),
                        border = when {
                            violation >= Forecasts.MEANINGFUL_VIOLATION -> Ink.Activity
                            violation <= -Forecasts.MEANINGFUL_VIOLATION -> Ink.Ember
                            else -> Ink.Border
                        }
                    )
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Figure(
                        if (violation > 0) "−$violation" else "${-violation}",
                        color = when {
                            violation >= Forecasts.MEANINGFUL_VIOLATION -> Ink.Activity
                            violation <= -Forecasts.MEANINGFUL_VIOLATION -> Ink.Ember
                            else -> Ink.Muted
                        }
                    )
                    Label(
                        when {
                            violation >= Forecasts.MEANINGFUL_VIOLATION -> "points easier than you expected"
                            violation <= -Forecasts.MEANINGFUL_VIOLATION -> "points harder than you expected"
                            else -> "about what you expected"
                        },
                        color = Ink.Primary
                    )
                }
                Text(
                    text = when {
                        violation >= Forecasts.MEANINGFUL_VIOLATION ->
                            "That gap is the whole thing. Your forecast was wrong in the " +
                                "direction it is usually wrong, and now you have it written down."
                        violation <= -Forecasts.MEANINGFUL_VIOLATION ->
                            "Harder than you thought. That usually means the step was too " +
                                "big — not that you misjudged yourself."
                        distress >= Forecasts.REALLY_FEARED ->
                            "Difficult, and you called it accurately. Worth doing this one " +
                                "again rather than moving on — nothing got disconfirmed."
                        else ->
                            "Close to accurate, and not especially hard. Move on to a bigger one."
                    },
                    color = Ink.Secondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            VSpace(26)
        }

        Column(
            modifier = Modifier.padding(start = Space.gutter, end = Space.gutter, bottom = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            PrimaryButton(
                text = "Save it",
                enabled = actual.isNotBlank()
            ) { onResolve(actual, distress) }
            // Never nag twice. Someone who does not want to answer gets to not answer.
            TextAction("I would rather not log this one") { onDismiss() }
        }
    }
}

@Composable
private fun Field(
    value: String,
    onChange: (String) -> Unit,
    placeholder: String,
    problem: String? = null,
    lines: Int = 1
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        OutlinedTextField(
            value = value,
            onValueChange = onChange,
            modifier = Modifier
                .fillMaxWidth()
                .then(if (lines > 1) Modifier.heightIn(min = (lines * 44).dp) else Modifier),
            placeholder = {
                Text(
                    text = placeholder,
                    color = Ink.Muted,
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            singleLine = lines == 1,
            shape = RoundedCornerShape(Space.radius),
            textStyle = MaterialTheme.typography.bodyLarge,
            isError = problem != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Ink.Primary,
                unfocusedTextColor = Ink.Primary,
                focusedBorderColor = Ink.Access,
                unfocusedBorderColor = Ink.Border,
                errorBorderColor = Ink.Ember,
                cursorColor = Ink.Access,
                focusedContainerColor = Ink.Surface,
                unfocusedContainerColor = Ink.Surface,
                errorContainerColor = Ink.Surface
            )
        )
        if (problem != null) Label(problem, color = Ink.Ember)
    }
}

@Composable
private fun DistressSlider(
    label: String,
    value: Int,
    accent: Color,
    onChange: (Int) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Label(label, color = Ink.Primary, strong = true)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            Figure("$value / ${Forecasts.SCALE}", color = accent, large = true)
            Label(Forecasts.label(value))
        }
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.toInt()) },
            valueRange = 0f..Forecasts.SCALE.toFloat(),
            steps = Forecasts.SCALE - 1,
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
