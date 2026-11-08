package com.debubble.app

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.debubble.app.engine.Domain
import com.debubble.app.ui.screens.Tab
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Google Play screenshots, taken from the real app rather than drawn: someone three
 * weeks in (a furnished room, a journey under way, a reminder set for this evening), eight
 * screens, the most Play shows. Not a test of behaviour: it fails only if a screen cannot be reached.
 */
@RunWith(AndroidJUnit4::class)
class StoreShots : Camera() {

    @Test
    fun takeStoreScreenshots() = withApp(OUT, threeWeeksIn()) { scenario, vm ->
        shot(OUT, "1-home")

        scenario.onActivity { vm.openCare(Domain.BODY) }
        settle()
        compose.onNode(hasContentDescription("Remind me later")).performClick()
        settle(1500)
        shot(OUT, "2-remind")

        scenario.onActivity { vm.openChallenge(vm.todaysRoad(vm.state.value)) }
        settle()
        shot(OUT, "3-letter")

        scenario.onActivity { vm.selectTab(Tab.OUTSIDE) }
        settle()
        shot(OUT, "4-outside")

        scenario.onActivity { vm.goJourney() }
        settle()
        shot(OUT, "5-journey")

        scenario.onActivity { vm.goWardrobe(fromYou = true) }
        settle()
        shot(OUT, "6-wardrobe")

        scenario.onActivity { vm.selectTab(Tab.JOURNAL) }
        settle()
        shot(OUT, "7-journal")

        scenario.onActivity { vm.openTraining() }
        settle()
        shot(OUT, "8-training")
    }

    private companion object {
        const val OUT = "/data/local/tmp/debubble-shots"
    }
}
