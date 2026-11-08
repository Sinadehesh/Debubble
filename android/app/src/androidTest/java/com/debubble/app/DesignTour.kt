package com.debubble.app

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.debubble.app.engine.Domain
import com.debubble.app.engine.Pillar
import com.debubble.app.engine.HabitKind
import com.debubble.app.engine.PremiumState
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Every screen in the app, photographed in one pass, for looking at the design as a whole:
 * the setup, home and its tour, the care tasks, the letters and postcards, the world outside,
 * the journal, the library, you. The pictures go to the "design-review" artifact.
 *
 * Screens are opened directly rather than tapped through, so each picture shows the screen
 * itself in the same lived-in state. Not a test of behaviour.
 */
@RunWith(AndroidJUnit4::class)
class DesignTour : Camera() {

    @Test
    fun everyScreen() = withApp(OUT, threeWeeksIn(openGuess = true)) { scenario, vm ->
        fun at(name: String, route: Route, wait: Long = 1800) {
            scenario.onActivity { vm.show(route) }
            settle(wait)
            shot(OUT, name)
        }

        at("01-intro", Route.Intro)
        at("02-setup-about", Route.Calibration)
        at("03-setup-habits", Route.Audit(firstRun = true))
        at("04-setup-journey", Route.GoalPicker(firstRun = true))
        at("05-setup-move-in", Route.CheckIn(firstRun = true))

        at("06-home", Route.Dashboard)
        sh("input swipe 540 1500 540 500 400")
        settle(1200)
        shot(OUT, "07-home-lower")
        scenario.onActivity { vm.replayTour() }
        at("08-tour", Route.Dashboard)
        scenario.onActivity { vm.endTour() }

        at("09-care", Route.Care(Domain.ROOM))
        at("10-care-done", Route.CareDone(Domain.ROOM, before = 2, after = 1, granted = listOf("lamp"), celebrate = "Say it out loud: done."))
        at("11-room", Route.Room)

        at("12-letter", Route.Challenge(vm.todaysRoad(vm.state.value)))
        at("13-postcard", Route.Completion(Pillar.SOCIAL, tierCleared = 7), wait = 3000)
        at("14-courage", Route.Courage(friction = 8, reason = "They said no"), wait = 3000)
        at("15-review", Route.Review(forecastId = 3L))

        at("16-outside", Route.Outside)
        at("17-journey", Route.Journey)
        at("18-journey-stop", Route.Mission)
        at("19-idea", Route.ReadPrinciple(0))

        at("20-journal", Route.Notes)
        at("21-write", Route.EditNote(id = null, linkLogId = null, linkLessonId = null))
        at("22-library", Route.Learn)
        vm.learn.course.firstOrNull()?.let { at("23-lesson", Route.ReadLesson(it.id)) }

        at("24-you", Route.Profile)
        at("25-wardrobe", Route.Wardrobe(fromYou = true))

        at("26-extra", Route.Routed(remediation = false))
        at("27-fix", Route.Routed(remediation = true))
        at("28-ending", Route.Transcendence(Pillar.ACCESS), wait = 3000)

        // Training, habits, reading and Premium: first as everyone sees them, then with
        // Premium on and a plan under way.
        at("29-training", Route.Train())
        at("30-habits", Route.HabitList())
        at("31-habit", Route.HabitPage(HabitKind.BODY))
        at("32-pack", Route.Pack("sleep"))
        at("33-paywall", Route.Paywall(back = Route.Profile))
        sh("input swipe 540 1600 540 300 500")
        settle(1200)
        shot(OUT, "33b-paywall-price")
        sh("input swipe 540 1600 540 300 500")
        settle(1200)
        shot(OUT, "33c-paywall-plans")
        sh("input swipe 540 1600 540 300 500")
        settle(1200)
        shot(OUT, "33d-paywall-discount")
        reseed { it.copy(premium = PremiumState(active = true, since = System.currentTimeMillis() - 9L * 86_400_000L), plan = "first_pushup", planDone = 4) }
        at("34-training-plan", Route.Train())
        at("35-insights", Route.HabitList())
        sh("input swipe 540 1600 540 300 500")
        settle(1200)
        shot(OUT, "35b-insights-lower")
        at("36-premium-on", Route.Paywall(back = Route.Profile))
        vm.readingRoom.pack("sleep")?.lessons?.getOrNull(1)?.let { at("37-reading-room-page", Route.ReadLesson(it.id)) }
    }

    private companion object {
        const val OUT = "/data/local/tmp/debubble-design"
    }
}
