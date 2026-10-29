package com.debubble.app

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.debubble.app.data.AppState
import com.debubble.app.data.LogEntry
import com.debubble.app.data.Note
import com.debubble.app.data.Repository
import com.debubble.app.engine.Baseline
import com.debubble.app.engine.CareState
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Forecast
import com.debubble.app.engine.Goal
import com.debubble.app.engine.GoalState
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.Look
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
import com.debubble.app.engine.Progress
import com.debubble.app.engine.Reminder
import com.debubble.app.ui.screens.Tab
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.time.LocalDate

/**
 * The Google Play screenshots, taken from the real app rather than drawn.
 *
 * Seeds the state of someone three weeks in (a furnished room, a journey under way, a
 * reminder set for this evening), then opens each screen and photographs the whole display.
 * The pictures are copied to /data/local/tmp/debubble-shots, where the workflow pulls them
 * from before the emulator shuts down. Not a test of behaviour: it fails only if a screen
 * cannot be reached.
 */
@RunWith(AndroidJUnit4::class)
class StoreShots {

    @get:Rule
    val compose = createEmptyComposeRule()

    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private val context = instrumentation.targetContext
    private val today = LocalDate.now().toEpochDay()

    @Test
    fun takeStoreScreenshots() {
        tidyStatusBar()
        runBlocking { Repository(context).update { threeWeeksIn() } }

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: DeBubbleViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[DeBubbleViewModel::class.java] }
            // A first launch on a fresh emulator is slow: wait for home to be on screen, then
            // a little longer for the room's pictures.
            waitFor("Journal")
            settle(3000)

            shot("1-home")

            scenario.onActivity { vm.openCare(Domain.BODY) }
            settle()
            compose.onNode(hasContentDescription("Remind me later")).performClick()
            settle(1500)
            shot("2-remind")

            scenario.onActivity { vm.openChallenge(vm.todaysRoad(vm.state.value)) }
            settle()
            shot("3-letter")

            scenario.onActivity { vm.selectTab(Tab.OUTSIDE) }
            settle()
            shot("4-outside")

            scenario.onActivity { vm.goJourney() }
            settle()
            shot("5-journey")

            scenario.onActivity { vm.goWardrobe(fromYou = true) }
            settle()
            shot("6-wardrobe")

            scenario.onActivity { vm.selectTab(Tab.JOURNAL) }
            settle()
            shot("7-journal")
        }
    }

    // ------------------------------------------------------------------ the person

    private fun threeWeeksIn(): AppState {
        val repo = Repository(context)
        val items = repo.loadItems()
        val owned = listOf(
            "bed", "lamp", "plant", "rug", "basket", "photo", "shelf", "water", "fruit",
            "cat", "books", "record", "desk", "beanbag", "guitar", "clock", "pennant"
        ).filter { items.item(it) != null }
        val started = today - 23

        val care = mapOf(
            Domain.BODY to CareState(dirt = 1, setDay = today, reps = 17, lastDoneDay = today - 1, anchor = "I put the kettle on"),
            Domain.ROOM to CareState(dirt = 1, setDay = today, reps = 12, lastDoneDay = today - 1, anchor = "I get home"),
            Domain.HEART to CareState(dirt = 0, setDay = today, reps = 11, lastDoneDay = today, anchor = "I sit down after dinner")
        )

        var id = 1L
        val log = mutableListOf<LogEntry>()
        val trips = listOf(
            Pillar.ACCESS to "Walk a street you have never walked down",
            Pillar.SOCIAL to "Ask a shop assistant for a recommendation",
            Pillar.ACTIVITY to "Try one thing on a menu you have never had",
            Pillar.ACCESS to "Sit in a park you have only walked past",
            Pillar.SOCIAL to "Say good morning to a neighbour"
        )
        for (d in 0 until 23) {
            val day = started + d
            Domain.entries.forEach { dom ->
                if ((d + dom.ordinal) % 4 != 0) {
                    log += LogEntry(id = id++, pillar = dom.name, tier = 1, title = "Looked after ${dom.name.lowercase()}", note = "Tiny", epochDay = day, minutes = 2, kind = "CARE")
                }
            }
            if (d % 2 == 0) {
                val (p, title) = trips[(d / 2) % trips.size]
                log += LogEntry(id = id++, pillar = p.name, tier = 1 + d / 3, title = title, epochDay = day, minutes = 15)
            }
        }
        log += LogEntry(id = id++, pillar = Domain.HEART.name, tier = 1, title = "Looked after heart", note = "Tiny", epochDay = today, minutes = 2, kind = "CARE")
        log += LogEntry(id = id++, pillar = Pillar.SOCIAL.name, tier = 6, title = "Ask someone how their week went", note = "They did not answer", friction = true, epochDay = today - 3)

        val notes = listOf(
            Note(
                id = 2L, epochDay = today - 2,
                body = "Asked the barista what she'd order. She drew me a little map to a bakery I'd never heard of. Nobody stared.",
                linkedKind = "PILLAR", linkedTitle = "Ask a shop assistant for a recommendation", linkedLogId = 3L
            ),
            Note(
                id = 1L, epochDay = today - 6,
                body = "Went the long way home through the park. Saw a heron. Felt like a different week.",
                linkedKind = "PILLAR", linkedTitle = "Sit in a park you have only walked past", linkedLogId = 2L
            )
        )
        val forecasts = listOf(
            Forecast(
                id = 1L, challengeId = "s-4", challengeTitle = "Ask a shop assistant for a recommendation",
                predicted = "They'll think I'm wasting their time", predictedDistress = 7, createdDay = today - 2,
                actual = "She seemed glad someone asked", actualDistress = 2, resolvedDay = today - 2
            ),
            Forecast(
                id = 2L, challengeId = "a-6", challengeTitle = "Walk a street you have never walked down",
                predicted = "I'll get lost and panic", predictedDistress = 6, createdDay = today - 8,
                actual = "Found a tiny bookshop", actualDistress = 3, resolvedDay = today - 8
            )
        )

        return AppState(
            introSeen = true,
            tourSeen = true,
            tipsSeen = setOf("outside", "journal", "library", "you", "journey"),
            onboarded = true,
            baseline = Baseline(),
            pillars = mapOf(
                Pillar.ACCESS.name to PillarState(tier = 10, cleared = 9, minutesInvested = 140),
                Pillar.ACTIVITY.name to PillarState(tier = 9, cleared = 8, minutesInvested = 95),
                Pillar.SOCIAL.name to PillarState(tier = 8, cleared = 7, minutesInvested = 80)
            ),
            friction = 7,
            lastActiveDay = today - 1,
            startedDay = started,
            servedDay = today,
            activeGoals = setOf(Goal.FRIENDS.name),
            goalStates = mapOf(Goal.FRIENDS.name to GoalState(step = 9, completed = 8, repsLogged = 14, read = setOf(0, 1))),
            xp = 3 * Progress.XP_PER_LEVEL + 70,
            auditDone = true,
            notes = notes,
            forecasts = forecasts,
            reminders = listOf(
                Reminder(link = "care:ROOM", title = "Room", text = "Tidy one surface", minute = 19 * 60, day = today)
            ),
            soundOn = false,
            widgetOffered = true,
            habitatSeen = "DOOR",
            care = care.mapKeys { it.key.name },
            home = HomeState(
                owned = owned,
                placed = owned.associateWith { items.item(it)!!.spot },
                look = Look(skin = 1, hair = 2, top = 1, hat = 2)
            ),
            movedIn = true,
            homeSeed = 7L,
            log = log.sortedByDescending { it.epochDay }
        )
    }

    // ------------------------------------------------------------------ the camera

    /** Demo mode: a fixed clock, full bars and battery, no notification icons, gesture bar. */
    private fun tidyStatusBar() {
        // SystemUI ignores demo commands until it has seen the setting and entered demo mode,
        // so the first two get a moment each.
        sh("settings put global sysui_demo_allowed 1")
        Thread.sleep(1500)
        sh("am broadcast -a com.android.systemui.demo -e command enter")
        Thread.sleep(1000)
        listOf(
            "am broadcast -a com.android.systemui.demo -e command clock -e hhmm 0930",
            "am broadcast -a com.android.systemui.demo -e command battery -e level 100 -e plugged false",
            "am broadcast -a com.android.systemui.demo -e command network -e wifi show -e level 4 -e fully true",
            "am broadcast -a com.android.systemui.demo -e command network -e mobile hide",
            "am broadcast -a com.android.systemui.demo -e command notifications -e visible false",
            "cmd overlay enable com.android.internal.systemui.navbar.gestural",
            "mkdir -p $OUT"
        ).forEach(::sh)
    }

    private fun waitFor(text: String) {
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun settle(millis: Long = 2000) {
        compose.waitForIdle()
        Thread.sleep(millis)
        compose.waitForIdle()
    }

    private fun shot(name: String) {
        val bitmap: Bitmap = instrumentation.uiAutomation.takeScreenshot()
            ?: error("no screenshot for $name")
        val dir = File(context.getExternalFilesDir(null), "shots").apply { mkdirs() }
        val file = File(dir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        sh("cp ${file.absolutePath} $OUT/$name.png")
    }

    private fun sh(command: String) {
        // Reading the output to the end is what waits for the command to finish.
        val pfd = instrumentation.uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }

    private companion object {
        const val OUT = "/data/local/tmp/debubble-shots"
    }
}
