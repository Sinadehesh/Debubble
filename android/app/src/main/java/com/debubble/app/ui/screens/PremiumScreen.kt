package com.debubble.app.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.UriHandler
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import com.debubble.app.engine.Look
import com.debubble.app.engine.PlanChoice
import com.debubble.app.engine.Premium
import com.debubble.app.ui.components.CheckBox
import com.debubble.app.ui.components.Glyph
import com.debubble.app.ui.components.Glyphs
import com.debubble.app.ui.components.Label
import com.debubble.app.ui.components.MeFigure
import com.debubble.app.ui.components.Pill
import com.debubble.app.ui.components.PrimaryButton
import com.debubble.app.ui.components.RoomArt
import com.debubble.app.ui.components.SecondaryButton
import com.debubble.app.ui.components.SpriteIcon
import com.debubble.app.ui.components.TextAction
import com.debubble.app.ui.components.TopBar
import com.debubble.app.ui.components.VSpace
import com.debubble.app.ui.components.panel
import com.debubble.app.ui.components.pixelScale
import com.debubble.app.ui.components.selectablePanel
import com.debubble.app.ui.theme.Cut
import com.debubble.app.ui.theme.Ink
import com.debubble.app.ui.theme.Space
import com.debubble.app.ui.theme.centered
import com.debubble.app.ui.theme.wash
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Premium: what it adds, what it costs, and how to stop.
 *
 * It says plainly that the basics stay free, then lists what is added, then the two ways to
 * pay as Google Play prices them for this person (in their currency, with any free start),
 * then the three things Play requires before anyone subscribes: that it renews by itself, how
 * often and for how much, and how to cancel. Monthly is chosen to start with and says why;
 * yearly sits right under it at about half the price; and anyone who cannot afford either is
 * told who to ask. No countdowns, no "only today", nothing hidden: an app built on not
 * manipulating people does not start at the till.
 */
@Composable
fun PremiumScreen(
    art: RoomArt,
    look: Look,
    active: Boolean,
    /** When Premium started, in milliseconds, or zero. */
    since: Long,
    /** The ways to pay, monthly first, from [com.debubble.app.engine.Offers.choices]. */
    plans: List<PlanChoice>,
    /** False when Google Play billing is not available on this device. */
    available: Boolean?,
    notice: String?,
    onSubscribe: (Premium.Plan) -> Unit,
    onRestore: () -> Unit,
    onClose: () -> Unit,
    /** Ask Google Play again, when it has not answered. */
    onRetry: () -> Unit = onRestore
) {
    val uri = LocalUriHandler.current
    val asked = plans.any { it.ready }
    // Google Play usually answers in a second. If it has not after a while (no Play Store,
    // signed out, or the subscription not set up yet), say so rather than wait forever.
    var waited by remember { mutableStateOf(false) }
    LaunchedEffect(asked, available) {
        waited = false
        if (!asked && available != false) {
            delay(8_000)
            waited = true
        }
    }
    var chosen by rememberSaveable { mutableStateOf(Premium.Plan.MONTHLY) }
    val current = plans.firstOrNull { it.plan == chosen } ?: plans.firstOrNull()

    Column(modifier = Modifier.fillMaxSize().navigationBarsPadding()) {
        TopBar(title = "Premium", subtitle = "DeBubble", onBack = onClose)
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.gutter)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(Space.gap)
        ) {
            Box(
                modifier = Modifier.fillMaxWidth().panel(fill = Ink.Gold.wash).padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                MeFigure(art, look, pixelScale = pixelScale(4f))
            }
            Pill(if (active) "Premium is on" else "Premium", Ink.Gold, filled = true)
            Text(
                text = if (active) "Thank you." else "More of everything you already do.",
                modifier = Modifier.semantics { heading() },
                color = Ink.Primary,
                style = MaterialTheme.typography.displaySmall
            )
            Text(
                text = if (active) {
                    "Premium is how DeBubble is paid for, with no ads and nothing about you sold. " +
                        "Everything below is yours."
                } else {
                    "Everything that helps you look after yourself stays free: today's four, your " +
                        "room, letters, journeys, the journal, the thirty-day library, training and " +
                        "every detail of your habits. Premium adds depth on top."
                },
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyLarge
            )

            Premium.Feature.entries.forEach { f ->
                Row(
                    modifier = Modifier.fillMaxWidth().panel().padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(Cut.small)
                            .background(Ink.Gold.wash)
                            .border(BorderStroke(2.dp, Ink.Border), Cut.small),
                        contentAlignment = Alignment.Center
                    ) {
                        SpriteIcon(art, f.icon, pixelScale = pixelScale(1.8f))
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(f.title, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
                        Text(f.detail, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            VSpace(4)
            if (active) {
                Column(
                    modifier = Modifier.fillMaxWidth().panel(fill = Ink.Gold.wash, borderWidth = 3.dp).padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Premium is on", color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
                    if (since > 0L) {
                        val date = Instant.ofEpochMilli(since).atZone(ZoneId.systemDefault()).toLocalDate()
                            .format(DateTimeFormatter.ofPattern("d MMMM yyyy"))
                        Label("Since $date.")
                    }
                    Label("To change or cancel it, use Google Play. Nothing you have done here goes away if you do.")
                    SecondaryButton("Manage in Google Play") { uri.tryOpen(Premium.MANAGE_URL) }
                }
            } else {
                Text(
                    text = if (plans.size > 1) "Two ways to pay" else "What it costs",
                    modifier = Modifier.semantics { heading() },
                    color = Ink.Primary,
                    style = MaterialTheme.typography.titleLarge
                )
                if (plans.size > 1) Label("Both get you everything above.")
                Column(
                    modifier = Modifier.fillMaxWidth().selectableGroup(),
                    verticalArrangement = Arrangement.spacedBy(Space.gap)
                ) {
                    plans.forEach { p ->
                        PlanCard(choice = p, selected = p.plan == current?.plan) { chosen = p.plan }
                    }
                }
                if (available == false) {
                    Text(
                        text = "Google Play billing is not available on this device, so Premium " +
                            "cannot be bought here. Everything else works as normal.",
                        modifier = Modifier.fillMaxWidth(),
                        color = Ink.Secondary,
                        style = MaterialTheme.typography.bodyMedium.centered
                    )
                } else {
                    PrimaryButton(
                        text = if (current?.trial == true) "Start the free trial" else "Start Premium",
                        enabled = current?.ready == true,
                        onClick = { current?.let { onSubscribe(it.plan) } }
                    )
                    if (!asked && !waited) {
                        Text(
                            text = "Asking Google Play for the price...",
                            modifier = Modifier.fillMaxWidth(),
                            color = Ink.Muted,
                            style = MaterialTheme.typography.labelMedium.centered
                        )
                    }
                    if (!asked && waited) {
                        Text(
                            text = "Google Play is not answering. Check you are signed in to " +
                                "Google Play and online, then try again.",
                            modifier = Modifier.fillMaxWidth(),
                            color = Ink.Secondary,
                            style = MaterialTheme.typography.bodyMedium.centered
                        )
                        SecondaryButton("Try again") {
                            waited = false
                            onRetry()
                        }
                    }
                }
                if (notice != null) {
                    Text(
                        notice,
                        modifier = Modifier.fillMaxWidth(),
                        color = Ink.Primary,
                        style = MaterialTheme.typography.bodyMedium.centered
                    )
                }
                Text(
                    text = "Renews automatically every ${(current?.plan ?: Premium.Plan.MONTHLY).every} " +
                        "at the price shown" +
                        (if (current?.trial == true) ", after the free trial," else "") +
                        " until you cancel. Cancel any time in Google Play, under Payments and " +
                        "subscriptions, at least a day before it renews. Payment is taken by Google Play.",
                    color = Ink.Muted,
                    style = MaterialTheme.typography.bodySmall
                )
                DiscountNote(art)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(modifier = Modifier.weight(1f)) { TextAction("Terms", colour = Ink.Secondary) { uri.tryOpen(Premium.TERMS_URL) } }
                Box(modifier = Modifier.weight(1f)) { TextAction("Privacy", colour = Ink.Secondary) { uri.tryOpen(Premium.PRIVACY_URL) } }
                Box(modifier = Modifier.weight(1.4f)) { TextAction("Restore purchase", colour = Ink.Secondary, onClick = onRestore) }
            }
        }
    }
}

/** One way to pay: a card with a dot that is visibly chosen, its price, and one sentence. */
@Composable
private fun PlanCard(choice: PlanChoice, selected: Boolean, onSelect: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .selectablePanel(selected, Ink.Gold)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CheckBox(selected = selected, accent = Ink.Gold, round = true)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(choice.plan.title, color = Ink.Primary, style = MaterialTheme.typography.titleMedium)
            Text(choice.line, color = Ink.Primary, style = MaterialTheme.typography.titleLarge)
            if (choice.note != null) {
                Text(choice.note, color = Ink.Secondary, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

/**
 * For anyone the price is too much for: write to the person who makes DeBubble. The address
 * is plain, selectable text, and the button opens a mail app if there is one; if there is
 * not, it copies the address instead of failing quietly.
 */
@Composable
private fun DiscountNote(art: RoomArt) {
    val context = LocalContext.current
    val clipboard = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }
    Column(
        modifier = Modifier.fillMaxWidth().panel(border = Ink.Faint).padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            SpriteIcon(art, "icon_heart", pixelScale = pixelScale(1.4f))
            Text(
                "If you cannot afford it",
                modifier = Modifier.semantics { heading() },
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
        }
        SelectionContainer {
            Text(
                text = buildAnnotatedString {
                    append("Email ")
                    withStyle(SpanStyle(color = Ink.Primary, fontWeight = FontWeight.ExtraBold)) {
                        append(Premium.CONTACT_EMAIL)
                    }
                    append(" and ask for a discount.")
                },
                color = Ink.Secondary,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        SecondaryButton(if (copied) "Address copied" else "Email for a discount") {
            val mail = Intent(
                Intent.ACTION_SENDTO,
                Uri.parse("mailto:${Premium.CONTACT_EMAIL}?subject=${Uri.encode(Premium.DISCOUNT_SUBJECT)}")
            ).putExtra(Intent.EXTRA_SUBJECT, Premium.DISCOUNT_SUBJECT)
            try {
                context.startActivity(mail)
            } catch (e: ActivityNotFoundException) {
                clipboard.setText(AnnotatedString(Premium.CONTACT_EMAIL))
                copied = true
            }
        }
    }
}

/** Open a link, and do nothing rather than crash on a phone with nothing to open it. */
private fun UriHandler.tryOpen(url: String) {
    try {
        openUri(url)
    } catch (e: IllegalArgumentException) {
        // No browser. The address is on the website and in the store listing.
    } catch (e: ActivityNotFoundException) {
        // Older Compose versions let this through unwrapped.
    }
}

/** The card on the You tab: one line about Premium, or a thank-you once it is on. */
@Composable
fun PremiumCard(art: RoomArt, active: Boolean, price: String, onOpen: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = Space.tap)
            .panel(fill = Ink.Gold.wash, borderWidth = if (active) 2.dp else 3.dp, shadow = if (active) Ink.Shadow else Ink.Border)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SpriteIcon(art, "icon_spark", pixelScale = pixelScale(2f))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                if (active) "Premium is on" else "DeBubble Premium",
                color = Ink.Primary,
                style = MaterialTheme.typography.titleMedium
            )
            Label(
                if (active) "Thank you. Plans, the reading room and insights are yours."
                else "Training plans, the reading room and insights. $price."
            )
        }
        Glyph(Glyphs.ArrowRight, colour = Ink.Primary, size = 16)
    }
}
