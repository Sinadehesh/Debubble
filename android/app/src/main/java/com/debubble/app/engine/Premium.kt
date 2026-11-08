package com.debubble.app.engine

import kotlinx.serialization.Serializable
import kotlin.math.roundToInt

/**
 * DeBubble Premium: one subscription that adds depth, never takes away the basics, paid
 * monthly or yearly.
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

    /** The subscription's product id, as created in Play Console. Its base plans are [Plan]. */
    const val PRODUCT_ID = "debubble_premium"

    /** Shown when Play cannot be asked, and as the price the store listing quotes. */
    const val FALLBACK_PRICE = "$4.99 a month"

    /** Where someone who cannot afford Premium can write and ask for a discount. */
    const val CONTACT_EMAIL = "sinadehesh@gmail.com"
    const val DISCOUNT_SUBJECT = "DeBubble Premium discount"

    /**
     * The two ways to pay, as the subscription's two base plans in Play Console. Both get
     * exactly the same things.
     *
     * Yearly costs about half of twelve monthly payments, so Premium is in reach for more
     * people. Monthly is the one the paywall starts on, and it says why: paying monthly is the
     * better help to the person who makes DeBubble. Nobody is nudged further than that.
     */
    enum class Plan(
        /** The base plan id in Play Console. */
        val basePlan: String,
        val title: String,
        /** "Renews automatically every month". */
        val every: String,
        private val usPrice: String,
        private val usMicros: Long,
        private val period: String
    ) {
        MONTHLY("monthly", "Monthly", "month", "$4.99", 4_990_000L, "P1M"),
        YEARLY("yearly", "Yearly", "year", "$29.99", 29_990_000L, "P1Y");

        /** The US price, for the paywall to show until Google Play says what it is here. */
        val fallback: List<PricePhase>
            get() = listOf(PricePhase(usPrice, usMicros, period, cycles = 0, recurring = true))

        companion object {
            fun of(basePlanId: String): Plan? = entries.firstOrNull { it.basePlan == basePlanId }
        }
    }

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

/** One way to pay, as the paywall shows it. */
data class PlanChoice(
    val plan: Premium.Plan,
    /** Everything, with any free start: "1 week free, then $4.99 a month". */
    val line: String,
    /** Only what it renews at: "$4.99 a month". */
    val price: String,
    /** The sentence under the price: why monthly, or what yearly saves. */
    val note: String?,
    val trial: Boolean,
    /** True once Google Play has offered it, so it can be bought. */
    val ready: Boolean
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
        val then = price(phases)
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

    /** The price it renews at, without any free start: "$4.99 a month", "$29.99 a year". */
    fun price(phases: List<PricePhase>): String {
        val recurring = renewing(phases) ?: return Premium.FALLBACK_PRICE
        return "${recurring.formattedPrice} ${period(recurring.billingPeriod)}"
    }

    /**
     * How much less a year of the yearly plan costs than a year of the monthly one, as a
     * whole percentage. Null when either is unknown or yearly is not cheaper. Both come from
     * the same Play answer, so they are in the same currency.
     */
    fun yearlySaving(yearly: List<PricePhase>, monthly: List<PricePhase>): Int? {
        val y = renewing(yearly)?.let(::perYear) ?: return null
        val m = renewing(monthly)?.let(::perYear) ?: return null
        if (m <= 0.0) return null
        return ((1.0 - y / m) * 100.0).roundToInt().takeIf { it in 1..99 }
    }

    /** The yearly plan next to the monthly one, in words: "About half the price of paying monthly". */
    fun yearlyNote(yearly: List<PricePhase>, monthly: List<PricePhase>): String? {
        val saving = yearlySaving(yearly, monthly) ?: return null
        return if (saving in 45..55) "About half the price of paying monthly"
        else "$saving% less than paying monthly"
    }

    /** What the monthly card says under its price, because the paywall starts on monthly. */
    const val MONTHLY_NOTE = "The biggest help to the person who makes DeBubble."

    /**
     * The paywall's choices, monthly first. [offered] is what Google Play offers this person,
     * by plan. Empty means Play has not answered yet: then both plans show at the US price and
     * neither can be bought. Once it has, only the plans it offers are shown.
     */
    fun choices(offered: Map<Premium.Plan, List<PricePhase>>): List<PlanChoice> {
        val asked = offered.isNotEmpty()
        fun phases(p: Premium.Plan): List<PricePhase> = if (asked) offered[p].orEmpty() else p.fallback
        val yearly = yearlyNote(phases(Premium.Plan.YEARLY), phases(Premium.Plan.MONTHLY))
        return Premium.Plan.entries.mapNotNull { p ->
            if (asked && phases(p).isEmpty()) return@mapNotNull null
            val ph = phases(p)
            PlanChoice(
                plan = p,
                line = describe(ph),
                price = price(ph),
                note = when (p) {
                    Premium.Plan.MONTHLY -> MONTHLY_NOTE
                    Premium.Plan.YEARLY -> yearly?.let { "$it." }
                },
                trial = hasFreeTrial(ph),
                ready = asked
            )
        }
    }

    /** For the You tab: "$4.99 a month or $29.99 a year". */
    fun summary(choices: List<PlanChoice>): String =
        choices.joinToString(" or ") { it.price }.ifEmpty { Premium.FALLBACK_PRICE }

    private fun renewing(phases: List<PricePhase>): PricePhase? = phases.lastOrNull { it.recurring } ?: phases.lastOrNull()

    /** What a phase that renews forever costs over a year, in micros. Months and years only. */
    private fun perYear(p: PricePhase): Double? {
        val m = Regex("P(\\d+)([MY])").matchEntire(p.billingPeriod.trim().uppercase()) ?: return null
        val n = m.groupValues[1].toIntOrNull()?.takeIf { it > 0 } ?: return null
        val months = if (m.groupValues[2] == "Y") n * 12 else n
        return p.priceMicros * 12.0 / months
    }
}
