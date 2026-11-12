package com.debubble.app

import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasText
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
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Footage for the advert: the real app, filmed with screenrecord while it is used.
 *
 * Not a test of behaviour, and not run with the others: only the "Ad footage" workflow passes
 * adReel=true, with animations on so the room breathes. Each clip is its own file under
 * /data/local/tmp/debubble-ad, plus taps.txt, which says where and when each tap or hold was
 * so the edit can draw it (android/tools/ad/make_ad.py).
 */
@RunWith(AndroidJUnit4::class)
class AdReel : Camera() {

    private val taps = StringBuilder()
    private var clipName = ""
    private var clipStart = 0L

    @Test
    fun recordAdFootage() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("adReel") == "true")
        withApp(OUT, threeWeeksIn()) { scenario, vm ->
            val items = Repository(context).loadItems()
            val full = threeWeeksIn().home

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
                    settle(1700)
                }
            }
            reseed { it.copy(pillars = cleared(24), habitatSeen = Stage.DOOR.name, home = full) }
            settle(1500)

            // Home, then a tiny thing done, and the room getting cleaner for it.
            clip("2-home") {
                settle(2200)
                sh("input swipe 540 1500 540 1000 900")
                settle(1500)
            }
            sh("input swipe 540 900 540 1500 300")
            settle(800)

            clip("3-care") {
                scenario.onActivity { vm.openCare(Domain.BODY) }
                settle(1500)
                hold(hasText("I did it"), 3300)
                settle(3200)
                scenario.onActivity { vm.recordFeeling(Domain.BODY, Feel.BETTER) }
                settle(2500)
            }

            clip("4-letter") {
                scenario.onActivity { vm.openChallenge(vm.todaysRoad(vm.state.value)) }
                settle(2500)
                sh("input swipe 540 1500 540 1050 900")
                settle(1200)
            }

            clip("5-outside") {
                scenario.onActivity { vm.selectTab(Tab.OUTSIDE) }
                settle(2200)
                sh("input swipe 540 1500 540 900 1000")
                settle(1200)
            }

            clip("6-training") {
                scenario.onActivity { vm.openTraining() }
                settle(2500)
                sh("input swipe 540 1500 540 1000 900")
                settle(1200)
            }

            clip("7-habit") {
                scenario.onActivity { vm.openHabit(HabitKind.BODY) }
                settle(1800)
                sh("input swipe 540 1500 540 700 1100")
                settle(1500)
            }

            clip("8-journal") {
                scenario.onActivity { vm.selectTab(Tab.JOURNAL) }
                settle(2500)
            }

            clip("9-wardrobe") {
                scenario.onActivity { vm.goWardrobe(fromYou = true) }
                settle(2500)
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
            "screenrecord --bit-rate 16000000 --time-limit 30 $OUT/$name.mp4"
        )
        clipStart = SystemClock.uptimeMillis()
        Thread.sleep(1000)
        body()
        sh("pkill -INT screenrecord")
        ParcelFileDescriptor.AutoCloseInputStream(rec).use { it.readBytes() }
        Thread.sleep(800)
    }

    /** Press and hold, as a finger would, so a hold-to-commit button fills and fires. */
    private fun hold(matcher: SemanticsMatcher, millis: Int) {
        val b = compose.onNode(matcher).fetchSemanticsNode().boundsInWindow
        val x = b.center.x.toInt()
        val y = b.center.y.toInt()
        taps.append("$clipName ${SystemClock.uptimeMillis() - clipStart} $x $y $millis\n")
        sh("input swipe $x $y $x $y $millis")
    }

    private companion object {
        const val OUT = "/data/local/tmp/debubble-ad"
    }
}
