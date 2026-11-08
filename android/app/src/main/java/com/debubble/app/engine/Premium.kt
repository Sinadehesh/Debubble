package com.debubble.app.engine

import kotlinx.serialization.Serializable

/**
 * DeBubble Premium: one monthly subscription that adds depth, never takes away the basics.
 *
 * The rule is that everything that helps someone look after themselves stays free: today's
 * four, the room, the letters and journeys, the journal, the thirty-day library, the training
 * tracker and its suggestions, your books, and every detail of your habits. Premium adds
 * things built on top: training plans, the reading room, and insights across your habits.
 * A paywall in front of the help would be a paywall in front of the people who need it most.
 *
 * Pure: no billing library, no Android. The Play side lives in billing/Billing.kt.
 */
object Premium {

    /** The subscription's product id and base plan, as created in Play Console. */
    const val PRODUCT_ID = "debubble_premium"
    const val BASE_PLAN = "monthly"

    /** Shown when Play cannot be asked, and as the price the store listing quotes. */
    const val FALLBACK_PRICE = "$4.99 a month"

    /** Where a subscriber manages or cancels it. */
    const val MANAGE_URL =
        "https://play.google.com/store/account/subscriptions?sku=$PRODUCT_ID&package=com.debubble.app"

    const val TERMS_URL = "https://www.sinadehesh.com/debubble/terms/"
    const val PRIVACY_URL = "https://www.sinadehesh.com/debubble/privacy/"

    /** What Premium adds, in the order the paywall lists it. */
    enum class Feature(val title: String, val detail: String, val icon: String) {
        PLANS(
            "Training plans",
            "Your first push-up, strong at home, a gym starter and a walking plan. Each session a little harder than the last.",
            "dumbbells"
        ),
        READING(
            "The reading room",
            "Short reads on sleep, moving for your mood, getting unstuck and a quieter mind. Five minutes each, with the research.",
            "books"
        ),
        INSIGHTS(
            "Insights",
            "What your habits show together: your best day, what you do together, what helps your mood.",
            "icon_spark"
        ),
        SUPPORT(
            "No ads, ever",
            "Premium is how DeBubble is paid for. Nothing you write or do ever leaves your phone.",
            "icon_heart"
        )
    }
}

/** What the app remembers about the subscription between launches. */
@Serializable
data class PremiumState(
    val active: Boolean = false,
    /** When the purchase was made, in milliseconds, as Play reported it. */
    val since: Long = 0L,
    /** When Play last answered, in milliseconds. Zero means never. */
    val checkedAt: Long = 0L,
    /** The purchase token, so a re-check can tell it is the same purchase. */
    val token: String = ""
)

/** What asking Play for purchases came back with. */
sealed interface PlayAnswer {
    /** An active, paid subscription. */
    data class Owned(val token: String, val purchaseTime: Long) : PlayAnswer
    /** Play answered, and there is no active subscription. */
    data object NotOwned : PlayAnswer
    /** Play could not be asked: offline, no Play Store, or an error. */
    data object Unavailable : PlayAnswer
}

object Entitlement {

    /**
     * The new cached state after asking Play.
     *
     * Only a real answer changes anything. Unavailable keeps what was known, so someone who
     * paid is not locked out on a train with no signal, and someone who cancelled loses it
     * the next time Play can be asked.
     */
    fun resolve(cached: PremiumState, answer: PlayAnswer, now: Long): PremiumState = when (answer) {
        is PlayAnswer.Owned -> PremiumState(
            active = true,
            since = if (cached.token == answer.token && cached.since > 0L) cached.since else answer.purchaseTime,
            checkedAt = now,
            token = answer.token
        )
        PlayAnswer.NotOwned -> PremiumState(active = false, since = 0L, checkedAt = now, token = "")
        PlayAnswer.Unavailable -> cached
    }
}

/** One pricing phase of an offer, copied out of Play's ProductDetails. */
data class PricePhase(
    val formattedPrice: String,
    val priceMicros: Long,
    /** ISO 8601: P1M, P1Y, P1W, P7D. */
    val billingPeriod: String,
    /** How many times this phase repeats. Zero for the recurring phase. */
    val cycles: Int,
    /** True for the phase that renews forever. */
    val recurring: Boolean
)

object Offers {

    /** "a month", "a year", "a week", "7 days". */
    fun period(iso: String, forLength: Boolean = false): String {
        val m = Regex("P(\\d+)([DWMY])").matchEntire(iso.trim().uppercase()) ?: return iso
        val n = m.groupValues[1].toIntOrNull() ?: return iso
        val unit = when (m.groupValues[2]) {
            "D" -> "day"
            "W" -> "week"
            "M" -> "month"
            else -> "year"
        }
        return when {
            n == 1 && !forLength -> "a $unit"
            n == 1 -> "1 $unit"
            else -> "$n ${unit}s"
        }
    }

    /**
     * The line under the button: what it costs, when, and any free start. Play requires the
     * price, the billing period and any trial to be plain before someone subscribes.
     */
    fun describe(phases: List<PricePhase>): String {
        val recurring = phases.lastOrNull { it.recurring } ?: phases.lastOrNull() ?: return Premium.FALLBACK_PRICE
        val then = "${recurring.formattedPrice} ${period(recurring.billingPeriod)}"
        val intro = phases.firstOrNull { !it.recurring } ?: return then
        val length = totalLength(intro)
        return if (intro.priceMicros == 0L) "$length free, then $then"
        else "${intro.formattedPrice} ${period(intro.billingPeriod)} for $length, then $then"
    }

    /** How long an introductory phase lasts in all: one week, 3 months. */
    private fun totalLength(p: PricePhase): String {
        val cycles = p.cycles.coerceAtLeast(1)
        if (cycles == 1) return period(p.billingPeriod, forLength = true)
        val m = Regex("P(\\d+)([DWMY])").matchEntire(p.billingPeriod.trim().uppercase())
            ?: return "$cycles × ${period(p.billingPeriod, forLength = true)}"
        val n = (m.groupValues[1].toIntOrNull() ?: 1) * cycles
        return period("P$n${m.groupValues[2]}", forLength = true)
    }

    /** True when the offer starts free. */
    fun hasFreeTrial(phases: List<PricePhase>): Boolean = phases.any { !it.recurring && it.priceMicros == 0L }
}
