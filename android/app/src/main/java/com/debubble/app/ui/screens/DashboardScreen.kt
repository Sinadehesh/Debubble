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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.debubble.app.data.AppState
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.Served
import com.debubble.app.ui.components.BubbleMap
import com.debubble.app.ui.components.ChallengeCard
import com.debubble.app.ui.components.Dot
import com.debubble.app.ui.components.Instrument
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
    pulse: Pillar?,
    onOpen: (Pillar) -> Unit
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
                VSpace(6)
                Text(
                    text = "Three for three. The perimeter moved on every axis today — " +
                        "that is a rare day, not a normal one.",
                    color = Ink.Ash,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            VSpace(16)
        }
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
                    .clickable { onSelect(tab) }
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Dot(if (tab == current) Ink.Primary else Ink.Faint, size = 5)
                Instrument(
                    tab.label,
                    color = if (tab == current) Ink.Primary else Ink.Faint,
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
