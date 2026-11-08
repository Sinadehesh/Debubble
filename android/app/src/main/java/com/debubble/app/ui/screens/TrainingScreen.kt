package com.debubble.app.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.debubble.app.data.AppState
import com.debubble.app.engine.Exercise
import com.debubble.app.engine.Measure
import com.debubble.app.engine.MoveGroup
import com.debubble.app.engine.Moves
import com.debubble.app.engine.Place
import com.debubble.app.engine.Plan
import com.debubble.app.engine.PlanSession
import com.debubble.app.engine.Plans
import com.debubble.app.engine.Target
import com.debubble.app.engine.Training
import com.debubble.app.engine.Week
import com.debubble.app.engine.WorkoutSet
import com.debubble.app.ui.components.BlockButton
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.IconBlock
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.LabelledProgress
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PremiumPill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SectionHeader
import com.debubble.app.ui.components.SheetHandle
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.Ticks
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.theme.Cut
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.fill
import com.debubble.app.ui.theme.wash

/**
 * Training: the gym tracker.
 *
 * Top to bottom in the order a decision happens: one thing to do right now, how this week is
 * going against the two numbers that matter (WHO: two strength days, 150 active minutes),
 * today's session or the plan you are on, the plans, and the record. Every number suggested
 * is what you did last time plus one step (see engine/Training.kt), so it is never a guess.
 */
@Composable
fun TrainingScreen(
    art: RoomArt,
    state: AppState,
    today: Long,
    h24: Boolean,
    onLog: (exercise: String, amount: Int, kg: Float) -> Unit,
    onUndo: (Long) -> Unit,
    onPlace: (Place) -> Unit,
    onStartPlan: (String) -> Unit,
    onStopPlan: () -> Unit,
    onPremium: () -> Unit,
    onBack: () -> Unit
) {
    val sets = state.workouts
    val place = state.placeEnum
    val week = Training.week(sets, today)
    var skip by remember { mutableIntStateOf(0) }
    var chosen by remember { mutableStateOf<Target?>(null) }
    var picking by remember { mutableStateOf(false) }
    val snack = chosen ?: Training.snack(sets, today, place, skip)

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        TopBar(
            title = "Training",
            subtitle = "Strength days this week: ${week.strengthDays} of ${Week.STRENGTH_DAYS}",
            onBack = onBack,
            action = {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(Cut.small)
                        .background(Ink.Body.wash)
                        .border(BorderStroke(2.dp, Ink.Border), Cut.small),
                    contentAlignment = Alignment.Center
                ) {
                    SpriteIcon(art, "dumbbells", pixelScale = pixelScale(1.6f))
                }
            }
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            PlaceChips(place, onPlace = {
                chosen = null
                onPlace(it)
            })

            MoveNow(
                art = art,
                target = snack,
                doneToday = Training.on(sets, today).isNotEmpty(),
                onEasier = { Training.easier(snack, sets, today)?.let { chosen = it } },
                onHarder = { Training.harder(snack, sets, today)?.let { chosen = it } },
                onAnother = {
                    chosen = null
                    skip++
                },
                onPick = { picking = true },
                onLog = { amount, kg ->
                    onLog(snack.exercise.id, amount, kg)
                    chosen = null
                }
            )

            WeekCard(week)

            val plan = state.activePlan
            val session = plan?.let { Plans.next(it, state.planDone) }
            VSpace(6)
            if (plan != null) {
                SectionHeader(
                    if (session == null) plan.name else "${plan.name} · ${state.planDone + 1} of ${plan.length}",
                    trailing = if (state.planDay == today) "Done today" else null
                )
                if (session == null) {
                    Text(
                        text = "All ${plan.length} sessions. That is the whole plan, done. Keep any of it you liked.",
                        color = Ink.Primary,
                        style = MaterialTheme.typography.bodyLarge
                    )
                    TextAction("Choose another plan", colour = Ink.Secondary, onClick = onStopPlan)
                } else {
                    PlanSessionCard(art, session, sets, today, state.planDay == today, onLog)
                    TextAction("Stop this plan", colour = Ink.Secondary, onClick = onStopPlan)
                }
            } else {
                SectionHeader("Today's session", trailing = "about 15 min")
                Label("One exercise from each of the three groups most due. Any of it counts.")
                Training.session(sets, today, place).forEach { t ->
                    SessionRow(art, t, Training.on(sets, today).count { it.exercise == t.exercise.id }, onLog)
                }
            }

            VSpace(6)
            SectionHeader("Plans", trailing = if (state.isPremium) null else "Premium")
            Plans.all.forEach { p ->
                PlanRow(
                    art = art,
                    plan = p,
                    current = state.plan == p.id && state.isPremium,
                    premium = state.isPremium,
                    onStart = { if (state.isPremium) onStartPlan(p.id) else onPremium() }
                )
            }

            val todays = Training.on(sets, today).sortedByDescending { it.id }
            if (todays.isNotEmpty()) {
                VSpace(6)
                SectionHeader("Today", trailing = "${todays.size} ${if (todays.size == 1) "set" else "sets"}")
                todays.forEach { s -> LoggedRow(art, s, h24, onUndo) }
            }

            val bests = Training.bests(sets).take(8)
            if (bests.isNotEmpty()) {
                VSpace(6)
                SectionHeader("Your bests")
                Column(
                    modifier = Modifier.fillMaxWidth().panel().padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    bests.forEach { b ->
                        Row(
                            modifier = Modifier.fillMaxWidth().heightIn(min = 44.dp).padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SpriteIcon(art, b.exercise.icon, pixelScale = pixelScale(1.4f))
                            Text(b.exercise.name, color = Ink.Primary, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                            Text(
                                text = b.exercise.amount(b.amount).substringBefore(" each") +
                                    if (b.kg > 0f) " × ${Training.kg(b.kg)}" else "",
                                color = Ink.Primary,
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }
                }
            }

            val recent = sets.filter { it.epochDay < today }.groupBy { it.epochDay }.entries
                .sortedByDescending { it.key }.take(6)
            if (recent.isNotEmpty()) {
                VSpace(6)
                SectionHeader("Recent days")
                recent.forEach { (day, list) ->
                    Column(
                        modifier = Modifier.fillMaxWidth().panel(fill = Ink.Void, border = Ink.Faint).padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Label(daysAgo(today - day), color = Ink.Primary, strong = true)
                        Text(Training.summary(list), color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            VSpace(4)
            Label(
                "Stop if anything hurts sharply, or in a joint, and try an easier version. If you have a " +
                    "heart condition or an injury, check with a doctor first."
            )
        }
    }

    if (picking) {
        ExercisePicker(
            art = art,
            place = place,
            onPick = { ex ->
                picking = false
                chosen = Training.target(ex, sets, today).copy(sets = 1)
            },
            onDismiss = { picking = false }
        )
    }
}

private fun daysAgo(n: Long): String = when (n) {
    0L -> "Today"
    1L -> "Yesterday"
    else -> "$n days ago"
}

/** At home or at the gym: what gets suggested. */
@Composable
private fun PlaceChips(place: Place, onPlace: (Place) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Place.entries.forEach { p ->
            val on = p == place
            Box(
                modifier = Modifier
                    .heightIn(min = 44.dp)
                    .panel(
                        shape = Cut.small,
                        fill = if (on) Ink.Gold.fill else Ink.Surface,
                        borderWidth = if (on) 3.dp else 2.dp,
                        lift = 2.dp,
                        shadow = if (on) Ink.Border else Ink.Shadow
                    )
                    .clickable(role = Role.RadioButton) { onPlace(p) }
                    .semantics { selected = on }
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    p.label,
                    color = Ink.Primary,
                    style = if (on) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelMedium
                )
            }
        }
    }
}

/**
 * One set of one thing, right now. The number is editable before it is logged, because the
 * honest number is the one you did, not the one you were asked for.
 */
@Composable
private fun MoveNow(
    art: RoomArt,
    target: Target,
    doneToday: Boolean,
    onEasier: () -> Unit,
    onHarder: () -> Unit,
    onAnother: () -> Unit,
    onPick: () -> Unit,
    onLog: (amount: Int, kg: Float) -> Unit
) {
    val ex = target.exercise
    var amount by remember(target) { mutableIntStateOf(target.amount) }
    var kg by remember(target) { mutableFloatStateOf(target.kg) }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = Ink.Body.wash, borderWidth = 3.dp, shadow = Ink.Border)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(Cut.small)
                    .background(Ink.Surface)
                    .border(BorderStroke(2.dp, Ink.Border), Cut.small),
                contentAlignment = Alignment.Center
            ) {
                SpriteIcon(art, ex.icon, pixelScale = pixelScale(2.2f))
            }
            Column(modifier = Modifier.weight(1f)) {
                Label(if (doneToday) "Another move, if you want one" else "A move for right now", color = Ink.Body, strong = true)
                Text(
                    text = target.copy(amount = amount, kg = kg).label.replaceFirstChar { it.uppercase() },
                    modifier = Modifier.semantics { heading() },
                    color = Ink.Primary,
                    style = MaterialTheme.typography.titleLarge
                )
            }
        }
        Text(ex.cue, color = Ink.Primary, style = MaterialTheme.typography.bodyMedium)
        Label(target.why)

        Stepper(
            value = amount,
            unit = when (ex.measure) {
                Measure.REPS -> if (ex.eachSide) "reps each side" else "reps"
                Measure.SECONDS -> if (ex.eachSide) "seconds each side" else "seconds"
                Measure.MINUTES -> "minutes"
            },
            step = if (ex.measure == Measure.SECONDS) 5 else 1,
            onChange = { amount = it.coerceIn(1, 999) }
        )
        if (ex.weighted) {
            KgStepper(kg = kg, step = ex.kgStep, onChange = { kg = it.coerceIn(0f, 500f) })
        }

        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (ex.easier != null) ChipButton("Easier", onEasier)
            if (ex.harder != null) ChipButton("Harder", onHarder)
            ChipButton("A different one", onAnother)
            ChipButton("Pick one", onPick)
        }
        PrimaryButton("Did it") { onLog(amount, kg) }
    }
}

@Composable
private fun ChipButton(text: String, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .heightIn(min = 44.dp)
            .panel(shape = Cut.small, shadow = Ink.Border)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, color = Ink.Primary, style = MaterialTheme.typography.labelLarge)
    }
}

/** − 8 reps +, in blocks. */
@Composable
private fun Stepper(value: Int, unit: String, step: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBlock(Glyphs.Minus, description = "Fewer $unit", onClick = { onChange(value - step) }, enabled = value > 1)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("$value", color = Ink.Primary, style = MaterialTheme.typography.headlineLarge)
            Label(unit)
        }
        IconBlock(Glyphs.Plus, description = "More $unit", onClick = { onChange(value + step) })
    }
}

@Composable
private fun KgStepper(kg: Float, step: Float, onChange: (Float) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconBlock(Glyphs.Minus, description = "Less weight", onClick = { onChange(kg - step) }, enabled = kg > 0f)
        Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(Training.kg(kg), color = Ink.Primary, style = MaterialTheme.typography.headlineMedium)
            Label("weight")
        }
        IconBlock(Glyphs.Plus, description = "More weight", onClick = { onChange(kg + step) })
    }
}

/** This week, Monday to Sunday, against the WHO's two numbers. */
@Composable
private fun WeekCard(week: Week) {
    Column(
        modifier = Modifier.fillMaxWidth().panel().padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("This week", color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
            Label("Mon to Sun")
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clearAndSetSemantics {
                    contentDescription = "Trained on ${week.days.count { it }} of 7 days this week"
                },
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            week.days.forEachIndexed { i, on ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .heightIn(min = 26.dp)
                        .clip(Cut.small)
                        .background(if (on) Ink.Body.fill else Ink.Well)
                        .border(BorderStroke(2.dp, if (on) Ink.Border else Ink.Faint), Cut.small),
                    contentAlignment = Alignment.Center
                ) {
                    Text(listOf("M", "T", "W", "T", "F", "S", "S")[i], color = Ink.Primary, style = MaterialTheme.typography.labelSmall)
                }
            }
        }
        LabelledProgress(
            label = "Strength days",
            value = "${week.strengthDays} of ${Week.STRENGTH_DAYS}",
            fraction = week.strengthDays / Week.STRENGTH_DAYS.toFloat(),
            accent = Ink.Body
        )
        LabelledProgress(
            label = "Minutes walking, running, cycling",
            value = "${week.activeMinutes} of ${Week.ACTIVE_MINUTES}",
            fraction = week.activeMinutes / Week.ACTIVE_MINUTES.toFloat(),
            accent = Ink.Body
        )
        Label("The WHO's numbers for adults. Some is better than none, and every bit counts.")
    }
}

/** One exercise in today's session: how much, how many sets, and a button per set. */
@Composable
private fun SessionRow(art: RoomArt, t: Target, loggedToday: Int, onLog: (String, Int, Float) -> Unit) {
    val ex = t.exercise
    val done = loggedToday >= t.sets
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = if (done) Ink.Void else Ink.Surface, border = if (done) Ink.Faint else Ink.Border)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteIcon(art, ex.icon, pixelScale = pixelScale(1.8f))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ex.name, color = if (done) Ink.Secondary else Ink.Primary, style = MaterialTheme.typography.titleMedium)
            Label("${t.sets} × ${ex.amount(t.amount)}" + if (t.kg > 0f) " · ${Training.kg(t.kg)}" else "")
            Ticks(total = t.sets, done = loggedToday.coerceAtMost(t.sets), accent = Ink.Body, modifier = Modifier.width(120.dp), height = 10)
        }
        BlockButton(
            text = if (done) "Done" else "Set done",
            modifier = Modifier.width(110.dp),
            fill = if (done) Ink.Surface else Ink.Primary,
            enabled = !done,
            small = true
        ) { onLog(ex.id, t.amount, t.kg) }
    }
}

@Composable
private fun PlanSessionCard(
    art: RoomArt,
    session: PlanSession,
    sets: List<WorkoutSet>,
    today: Long,
    finishedToday: Boolean,
    onLog: (String, Int, Float) -> Unit
) {
    Text(session.title, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
    if (finishedToday) {
        Label("Today's session is done. The next one is ready whenever you are, tomorrow or later.")
    }
    session.items.forEach { item ->
        val ex = Moves.byId(item.exercise) ?: return@forEach
        val kg = if (ex.weighted) Training.target(ex, sets, today).kg else 0f
        val t = Target(ex, item.amount, kg, item.sets, "")
        SessionRow(art, t, sets.count { it.epochDay == today && it.exercise == ex.id }, onLog)
    }
}

@Composable
private fun PlanRow(art: RoomArt, plan: Plan, current: Boolean, premium: Boolean, onStart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .panel(fill = if (current) Ink.Gold.wash else Ink.Surface, borderWidth = if (current) 3.dp else 2.dp)
            .clickable(role = Role.Button, enabled = !current, onClick = onStart)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(Cut.small)
                .background(Ink.Body.wash)
                .border(BorderStroke(2.dp, Ink.Border), Cut.small),
            contentAlignment = Alignment.Center
        ) {
            SpriteIcon(art, plan.icon, pixelScale = pixelScale(1.8f))
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(plan.name, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
            Text(plan.blurb, color = Ink.Secondary, style = MaterialTheme.typography.bodySmall)
            Label("${plan.length} sessions · ${plan.perWeek} a week · ${plan.place.label.lowercase()}")
        }
        when {
            current -> Pill("On it", Ink.Gold, filled = true)
            !premium -> PremiumPill()
            else -> Glyph(Glyphs.ArrowRight, colour = Ink.Primary, size = 16)
        }
    }
}

@Composable
private fun LoggedRow(art: RoomArt, s: WorkoutSet, h24: Boolean, onUndo: (Long) -> Unit) {
    val ex = Moves.byId(s.exercise) ?: return
    Row(
        modifier = Modifier.fillMaxWidth().panel(fill = Ink.Void, border = Ink.Faint).padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteIcon(art, ex.icon, pixelScale = pixelScale(1.4f))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                ex.name + " · " + ex.amount(s.amount) + if (s.kg > 0f) " × ${Training.kg(s.kg)}" else "",
                color = Ink.Primary,
                style = MaterialTheme.typography.titleSmall
            )
            Label(com.debubble.app.engine.Habits.clock(s.minute, h24))
        }
        IconBlock(Glyphs.Undo, description = "Undo this set", onClick = { onUndo(s.id) }, size = 40)
    }
}

/** Every exercise a place can do, by group. One tap puts it in the "right now" card. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ExercisePicker(art: RoomArt, place: Place, onPick: (Exercise) -> Unit, onDismiss: () -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
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
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Pick a move", color = Ink.Primary, style = MaterialTheme.typography.headlineMedium)
            Label(if (place == Place.HOME) "Everything here needs nothing but you." else "Everything, kit included.")
            MoveGroup.entries.forEach { g ->
                val moves = Moves.at(place).filter { it.group == g }
                if (moves.isEmpty()) return@forEach
                VSpace(6)
                Text(g.label, color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
                moves.forEach { ex ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = Space.tap)
                            .panel(fill = Ink.SurfaceHigh)
                            .clickable(role = Role.Button) { onPick(ex) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SpriteIcon(art, ex.icon, pixelScale = pixelScale(1.4f))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(ex.name, color = Ink.Primary, style = MaterialTheme.typography.titleSmall)
                            Label(ex.gear.label)
                        }
                        Glyph(Glyphs.ArrowRight, colour = Ink.Primary, size = 14)
                    }
                }
            }
        }
    }
}
