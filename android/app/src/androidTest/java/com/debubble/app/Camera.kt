package com.debubble.app

import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
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
import com.debubble.app.engine.ReadingEntry
import com.debubble.app.engine.Book
import com.debubble.app.engine.WorkoutSet
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import java.io.File
import java.time.LocalDate

/**
 * What the screenshot tests share: someone three weeks in, a tidy status bar, and a camera.
 *
 * Pictures are copied to a folder under /data/local/tmp, which the workflow pulls before
 * the emulator shuts down; the app's own storage goes when the test uninstalls it.
 */
abstract class Camera {

    @get:Rule
    val compose = createEmptyComposeRule()

    protected val instrumentation = InstrumentationRegistry.getInstrumentation()
    protected val context = instrumentation.targetContext
    protected val today = LocalDate.now().toEpochDay()

    /** Seeds [state], opens the app on it, and waits for the first screen to draw. */
    protected fun withApp(out: String, state: AppState, body: (ActivityScenario<MainActivity>, DeBubbleViewModel) -> Unit) {
        tidyStatusBar(out)
        runBlocking { Repository(context).update { state } }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: DeBubbleViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[DeBubbleViewModel::class.java] }
            // A first launch on a fresh emulator is slow: wait for home to be on screen, then
            // a little longer for the room's pictures.
            waitFor("Journal")
            settle(3000)
            body(scenario, vm)
        }
    }

    /** Change the saved state while the app is open; the screens follow it. */
    protected fun reseed(transform: (AppState) -> AppState) {
        runBlocking { Repository(context).update(transform) }
    }

    // ------------------------------------------------------------------ the person

    protected fun threeWeeksIn(openGuess: Boolean = false): AppState {
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
        val forecasts = listOfNotNull(
            Forecast(
                id = 1L, challengeId = "s-4", challengeTitle = "Ask a shop assistant for a recommendation",
                predicted = "They'll think I'm wasting their time", predictedDistress = 7, createdDay = today - 2,
                actual = "She seemed glad someone asked", actualDistress = 2, resolvedDay = today - 2
            ),
            Forecast(
                id = 2L, challengeId = "a-6", challengeTitle = "Walk a street you have never walked down",
                predicted = "I'll get lost and panic", predictedDistress = 6, createdDay = today - 8,
                actual = "Found a tiny bookshop", actualDistress = 3, resolvedDay = today - 8
            ),
            if (openGuess) Forecast(
                id = 3L, challengeId = "s-7", challengeTitle = "Say good morning to a neighbour",
                predicted = "They'll look at me like I'm strange", predictedDistress = 6, createdDay = today - 1
            ) else null
        )

        // Three weeks of training: push-ups up the ladder, squats, a plank and some walks,
        // mostly in the morning.
        val workouts = mutableListOf<WorkoutSet>()
        var setId = 1_000L
        // Push-ups were last done three days ago, so they are what is due today.
        for (d in 0 until 23) {
            val day = started + d
            if (d % 4 == 0) {
                val pushup = if (d < 12) "incline_pushup" else "knee_pushup"
                val reps = if (d < 12) 6 + d / 2 else 5 + (d - 12) / 2
                repeat(2) { workouts += WorkoutSet(setId++, pushup, reps, 0f, day, 7 * 60 + 40) }
            }
            if (d % 2 == 0) workouts += WorkoutSet(setId++, "squat", 8 + d / 2, 0f, day, 7 * 60 + 45)
            if (d % 4 == 2) {
                workouts += WorkoutSet(setId++, "superman", 8 + d / 4, 0f, day, 7 * 60 + 48)
                workouts += WorkoutSet(setId++, "plank", 15 + d, 0f, day, 7 * 60 + 50)
            }
            if (d % 3 == 0) workouts += WorkoutSet(setId++, "walk", 15 + d, 0f, day, 18 * 60 + 10)
        }

        // A book on the go, three on the shelf, and most evenings a few pages.
        val books = listOf(
            Book(1L, "Frankenstein", "Mary Shelley", 280, 280, started - 40, started - 20),
            Book(2L, "The Secret Garden", "Frances Hodgson Burnett", 300, 300, started - 18, started + 6),
            Book(3L, "Pride and Prejudice", "Jane Austen", 380, 380, started + 6, started + 18),
            Book(4L, "The Wind in the Willows", "Kenneth Grahame", 240, 96, started + 18)
        )
        val readingLog = (0 until 23).filter { it % 3 != 1 }.map { d ->
            ReadingEntry(5_000L + d, if (d < 18) 3L else 4L, 8 + (d % 5) * 3, started + d, 21 * 60 + 30)
        }

        return AppState(
            workouts = workouts,
            books = books,
            readingLog = readingLog,
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
    private fun tidyStatusBar(out: String) {
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
            "mkdir -p $out"
        ).forEach(::sh)
    }

    protected fun waitFor(text: String) {
        compose.waitUntil(timeoutMillis = 60_000) {
            compose.onAllNodes(hasText(text)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun settle(millis: Long = 2000) {
        compose.waitForIdle()
        Thread.sleep(millis)
        compose.waitForIdle()
    }

    protected fun shot(out: String, name: String) {
        val bitmap: Bitmap = instrumentation.uiAutomation.takeScreenshot()
            ?: error("no screenshot for $name")
        val dir = File(context.getExternalFilesDir(null), "shots").apply { mkdirs() }
        val file = File(dir, "$name.png")
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        sh("cp ${file.absolutePath} $out/$name.png")
    }

    protected fun sh(command: String) {
        // Reading the output to the end is what waits for the command to finish.
        val pfd = instrumentation.uiAutomation.executeShellCommand(command)
        ParcelFileDescriptor.AutoCloseInputStream(pfd).use { it.readBytes() }
    }
}
