package com.debubble.app

import android.graphics.Rect
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.debubble.app.data.Repository
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Feel
import com.debubble.app.engine.HabitKind
import com.debubble.app.engine.HomeState
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.PillarState
import com.debubble.app.engine.Stage
import com.debubble.app.ui.screens.Tab
import kotlinx.coroutines.runBlocking
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Footage for the advert: the real app, filmed with screenrecord while it is used.
 *
 * Not a test of behaviour, and not run with the others: only the "Ad footage" workflow passes
 * adReel=true, with animations on. It deliberately has no Compose test rule. The rule runs the
 * app's clock for the test, which is right for screenshots and wrong for film: the room's
 * breathing and a held button filling only happen in real time. So screens are found through
 * accessibility, as a person's screen reader would find them, and waits are real waits.
 *
 * Each clip is its own file under /data/local/tmp/debubble-ad, plus taps.txt, which says
 * where and when each hold was, so the edit can draw the finger (android/tools/ad/make_ad.py).
 */
@RunWith(AndroidJUnit4::class)
class AdReel : Seeds() {

    private val taps = StringBuilder()
    private var clipName = ""
    private var clipStart = 0L

    @Test
    fun recordAdFootage() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("adReel") == "true")
        tidyStatusBar(OUT)
        val start = threeWeeksIn()
        runBlocking { Repository(context).update { start } }
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            lateinit var vm: DeBubbleViewModel
            scenario.onActivity { vm = ViewModelProvider(it)[DeBubbleViewModel::class.java] }
            waitForText("Journal")
            Thread.sleep(4000)

            val items = Repository(context).loadItems()
            val full = start.home

            // The room at every stage, furnished as it would be by then.
            clip("1-stages") {
                Stage.all.forEachIndexed { i, stage ->
                    val owned = full.owned.take(2 + i * 3)
                    reseed { s ->
                        s.copy(
                            pillars = cleared(stage.threshold),
                            habitatSeen = stage.name,
                            home = HomeState(
                                owned = owned,
                                placed = owned.associateWith { items.item(it)!!.spot },
                                look = full.look
                            )
                        )
                    }
                    Thread.sleep(2000)
                }
            }
            reseed { it.copy(pillars = start.pillars, habitatSeen = start.habitatSeen, home = full) }
            Thread.sleep(2000)

            clip("2-home") {
                Thread.sleep(3000)
                sh("input swipe 540 1500 540 1000 900")
                Thread.sleep(1800)
            }
            sh("input swipe 540 900 540 1500 300")
            Thread.sleep(1200)

            // A tiny thing done: hold "I did it", the room cleaned, then home with it sparkling.
            clip("3-care") {
                scenario.onActivity { vm.openCare(Domain.BODY) }
                Thread.sleep(2200)
                hold("I did it", 3400)
                Thread.sleep(4500)
                scenario.onActivity { vm.recordFeeling(Domain.BODY, Feel.BETTER) }
                Thread.sleep(3500)
            }

            clip("4-letter") {
                scenario.onActivity { vm.openChallenge(vm.todaysRoad(vm.state.value)) }
                Thread.sleep(3000)
                sh("input swipe 540 1500 540 1050 900")
                Thread.sleep(1800)
            }

            clip("5-outside") {
                scenario.onActivity { vm.selectTab(Tab.OUTSIDE) }
                Thread.sleep(3000)
                sh("input swipe 540 1500 540 900 1000")
                Thread.sleep(1800)
            }

            clip("6-training") {
                scenario.onActivity { vm.openTraining() }
                Thread.sleep(3000)
                sh("input swipe 540 1500 540 1000 900")
                Thread.sleep(1800)
            }

            clip("7-habit") {
                scenario.onActivity { vm.openHabit(HabitKind.BODY) }
                Thread.sleep(2500)
                sh("input swipe 540 1500 540 700 1100")
                Thread.sleep(2000)
            }

            clip("8-journal") {
                scenario.onActivity { vm.selectTab(Tab.JOURNAL) }
                Thread.sleep(3500)
            }

            clip("9-wardrobe") {
                scenario.onActivity { vm.goWardrobe(fromYou = true) }
                Thread.sleep(3500)
            }

            File(context.getExternalFilesDir(null), "taps.txt").let { f ->
                f.writeText(taps.toString())
                sh("cp ${f.absolutePath} $OUT/taps.txt")
            }
        }
    }

    /** Every ladder cleared evenly, to [total] rungs between them. */
    private fun cleared(total: Int): Map<String, PillarState> {
        val each = total / 3
        return Pillar.order.mapIndexed { i, p ->
            val n = each + if (i < total % 3) 1 else 0
            p.name to PillarState(tier = n + 1, cleared = n, minutesInvested = n * 12)
        }.toMap()
    }

    /**
     * Film [body]. screenrecord stops on SIGINT and closes the file properly; the time limit
     * is only a backstop. The first second or so is the recorder starting, and is cut.
     */
    private fun clip(name: String, body: () -> Unit) {
        clipName = name
        val rec = instrumentation.uiAutomation.executeShellCommand(
            "screenrecord --bit-rate 20000000 --time-limit 40 $OUT/$name.mp4"
        )
        clipStart = SystemClock.uptimeMillis()
        Thread.sleep(1000)
        body()
        sh("pkill -INT screenrecord")
        ParcelFileDescriptor.AutoCloseInputStream(rec).use { it.readBytes() }
        Thread.sleep(800)
    }

    /** Where [text] is on screen, found the way a screen reader finds it. */
    private fun centreOf(text: String): Pair<Int, Int>? {
        val root = instrumentation.uiAutomation.rootInActiveWindow ?: return null
        val node = root.findAccessibilityNodeInfosByText(text).firstOrNull() ?: return null
        val r = Rect()
        node.getBoundsInScreen(r)
        return r.centerX() to r.centerY()
    }

    private fun waitForText(text: String, timeout: Long = 60_000) {
        val until = SystemClock.uptimeMillis() + timeout
        while (SystemClock.uptimeMillis() < until) {
            if (centreOf(text) != null) return
            Thread.sleep(250)
        }
        error("\"$text\" never appeared")
    }

    /** Press and hold, as a finger would, so a hold-to-commit button fills and fires. */
    private fun hold(text: String, millis: Int) {
        waitForText(text, 10_000)
        val (x, y) = centreOf(text) ?: error("no \"$text\" to hold")
        taps.append("$clipName ${SystemClock.uptimeMillis() - clipStart} $x $y $millis\n")
        sh("input swipe $x $y $x $y $millis")
    }

    private companion object {
        const val OUT = "/data/local/tmp/debubble-ad"
    }
}
