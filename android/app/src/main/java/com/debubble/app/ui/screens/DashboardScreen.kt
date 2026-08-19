package com.debubble.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.debubble.app.data.AppState
import com.debubble.app.engine.Goal
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.RepType
import com.debubble.app.engine.Served
import com.debubble.app.ui.components.BubbleMap
import com.debubble.app.ui.components.ChallengeCard
import com.debubble.app.ui.components.Dot
import com.debubble.app.ui.components.Instrument
import com.debubble.app.ui.components.RepTracker
import com.debubble.app.ui.components.rememberReducedMotion
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.tierCode
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.accent

/**
 * The Expanding Reality.
 *
 * Split hard down the middle: the top belongs to feeling (the Bubble Map), the bottom to
 * deciding (three offers, one per pillar, always in the same order). Nothing else competes.
 */
@Composable
fun DashboardScreen(
    state: AppState,
    dayIndex: Long,
    served: Map<Pillar, Served>,
    mission: Served?,
    reps: List<RepType>,
    pulse: Pillar?,
    onOpen: (Pillar) -> Unit,
    onOpenMission: () -> Unit,
    onLogRep: (RepType) -> Unit,
    onPrinciples: () -> Unit,
    onPickGoal: () -> Unit
) {
    val tiers = Pillar.order.associateWith { state.state(it).tier }
    val open = Pillar.order.count { !state.isDoneToday(it) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Ink.Void)
            .verticalScroll(rememberScrollState())
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.16f)
        ) {
            BubbleMap(
                tiers = tiers,
                pulse = pulse,
                // The map drifts forever, so it is the one surface that must go still when the
                // user has asked the system for no animation.
                animate = !rememberReducedMotion(),
                modifier = Modifier.fillMaxSize()
            )
            // HUD is overlaid and deliberately non-interactive: the map reports, it is not a control.
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = Space.gutter, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text(
                        text = "DAY ${tierCode(dayIndex.toInt())}",
                        color = Ink.Primary,
                        style = MaterialTheme.typography.headlineMedium
                    )
                    Instrument("Perimeter live", modifier = Modifier.padding(bottom = 3.dp), small = true)
                }
                Box(modifier = Modifier.weight(1f))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Pillar.order.forEach { p ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Dot(p.accent, size = 5)
                            Instrument("${p.code} ${tierCode(tiers.getValue(p))}", color = Ink.Ash, small = true)
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier.padding(horizontal = Space.gutter),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Instrument("Today's serve")
                Instrument(if (open == 0) "All taken" else "$open open")
            }

            Pillar.order.forEach { pillar ->
                served[pillar]?.let { s ->
                    ChallengeCard(
                        served = s,
                        done = state.isDoneToday(pillar),
                        onClick = { onOpen(pillar) }
                    )
                }
            }

            if (open == 0) {
                VSpace(2)
                Text(
                    text = "Three for three. The perimeter moved on every axis today — " +
                        "that is a rare day, not a normal one.",
                    color = Ink.Ash,
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            /* ---- the goal layer: a campaign step, then unlimited reps ---- */

            val goal = state.goalEnum
            VSpace(14)

            if (goal == null) {
                NoGoalCard(onPickGoal)
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Instrument("Campaign · ${goal.display}")
                    Instrument(
                        if (mission == null) "Complete"
                        else "Step ${tierCode(mission.tier)} / 030"
                    )
                }

                when {
                    mission == null -> CampaignComplete(goal, onPickGoal)
                    state.missionDoneToday -> ChallengeCard(
                        served = mission,
                        done = true,
                        onClick = {}
                    )
                    else -> ChallengeCard(
                        served = mission,
                        done = false,
                        onClick = onOpenMission
                    )
                }

                VSpace(14)
                RepTracker(
                    reps = reps,
                    today = state.repsToday,
                    target = mission?.repTarget ?: 0,
                    lifetime = state.repsOnGoal(goal),
                    accent = goal.homePillar.accent,
                    onLog = onLogRep
                )

                VSpace(14)
                ReadingNudge(goal, onPrinciples)
            }

            VSpace(20)
        }
    }
}

/** Shown until a campaign is chosen. The daily three work fine without one, but this is
 *  where the app stops being generic, so it asks once and then stays out of the way. */
@Composable
private fun NoGoalCard(onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Space.radiusLarge))
            .background(Ink.Strata)
            .border(1.dp, Ink.EdgeSoft, RoundedCornerShape(Space.radiusLarge))
            .clickable(role = Role.Button, onClick = onPick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Instrument("No campaign running")
        Text(
            text = "Point this at something.",
            color = Ink.Primary,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Friends, a partner, a craft, a life worth describing. Thirty steps, " +
                "daily reps and reading of its own on top of the three above.",
            color = Ink.Ash,
            style = MaterialTheme.typography.bodyMedium
        )
        VSpace(2)
        Instrument("Choose one →", color = Ink.Primary, small = true)
    }
}

@Composable
private fun CampaignComplete(goal: Goal, onPick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Space.radiusLarge))
            .background(Ink.Strata)
            .border(1.dp, goal.homePillar.accent.copy(alpha = 0.35f), RoundedCornerShape(Space.radiusLarge))
            .clickable(role = Role.Button, onClick = onPick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Instrument("Campaign cleared", color = goal.homePillar.accent)
        Text(
            text = "Thirty steps of\n${goal.display.lowercase()}.",
            color = Ink.Primary,
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "The reps do not stop — keep logging them. When you are ready, point the " +
                "next thirty steps somewhere else.",
            color = Ink.Ash,
            style = MaterialTheme.typography.bodyMedium
        )
        VSpace(2)
        Instrument("Pick the next one →", color = Ink.Primary, small = true)
    }
}

@Composable
private fun ReadingNudge(goal: Goal, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(Space.radius))
            .background(Ink.Ridge)
            .sizeIn(minHeight = 48.dp)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 15.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Instrument("The ideas underneath", color = goal.homePillar.accent, small = true)
            Text(
                text = "Why any of this works",
                color = Ink.Primary,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium)
            )
        }
        Instrument("→", color = Ink.Dim)
    }
}

/** Bottom navigation. Three destinations, no hamburger, no settings gear on the home surface. */
@Composable
fun TabBar(
    current: Tab,
    onSelect: (Tab) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(Ink.Strata)
            .padding(top = 10.dp, bottom = 14.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Tab.entries.forEach { tab ->
            Column(
                modifier = Modifier
                    .clickable(role = Role.Tab) { onSelect(tab) }
                    .semantics { selected = tab == current }
                    .sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                    .wrapContentSize(Alignment.Center)
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                // The dot may stay Faint — it is decoration, and the label carries the meaning.
                Dot(if (tab == current) Ink.Primary else Ink.Faint, size = 5)
                Instrument(
                    tab.label,
                    color = if (tab == current) Ink.Primary else Ink.Dim,
                    small = true
                )
            }
        }
    }
}

enum class Tab(val label: String) {
    TODAY("Today"),
    PROFILE("Profile")
}
