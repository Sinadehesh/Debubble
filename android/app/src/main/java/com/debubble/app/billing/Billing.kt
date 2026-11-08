package com.debubble.app.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.debubble.app.engine.Offers
import com.debubble.app.engine.PlayAnswer
import com.debubble.app.engine.Premium
import com.debubble.app.engine.PricePhase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The subscription, through Google Play Billing.
 *
 * Play is the source of truth: on every launch the app asks it what is owned, and only its
 * answer changes whether Premium is on (see [com.debubble.app.engine.Entitlement]). Purchases
 * are acknowledged as soon as they are seen, because Play refunds any subscription that is
 * not acknowledged within three days.
 *
 * Nothing is sent anywhere by the app itself. The purchase happens in Google Play's own sheet,
 * and all the app keeps is that it is active and the purchase token Play gave it.
 */
class Billing(
    context: Context,
    /** Called with every real answer, and with Unavailable when Play cannot be reached. */
    private val onAnswer: (PlayAnswer) -> Unit
) : PurchasesUpdatedListener {

    /**
     * One way to pay, as Play offers it here: the plan, the token that buys it, and its price
     * phases. What the paywall says about it comes from [Offers.choices].
     */
    data class Offer(
        val plan: Premium.Plan,
        val product: ProductDetails,
        val token: String,
        val phases: List<PricePhase>
    )

    private val client: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .enableAutoServiceReconnection()
        .build()

    /** The plans Play offers this person, by plan. Empty until it has answered. */
    private val _offers = MutableStateFlow<Map<Premium.Plan, Offer>>(emptyMap())
    val offers: StateFlow<Map<Premium.Plan, Offer>> = _offers

    /** Null until Play has answered once; false when billing is not available here. */
    private val _available = MutableStateFlow<Boolean?>(null)
    val available: StateFlow<Boolean?> = _available

    /** A line for the paywall after something did not go to plan: pending, failed. */
    private val _notice = MutableStateFlow<String?>(null)
    val notice: StateFlow<String?> = _notice

    private var connecting = false

    /** Connect if needed, then ask what is owned and what is on offer. */
    fun start() {
        if (client.isReady) {
            refresh()
            if (_offers.value.size < Premium.Plan.entries.size) loadOffers()
            return
        }
        if (connecting) return
        connecting = true
        client.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                connecting = false
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    _available.value = true
                    refresh()
                    loadOffers()
                } else {
                    _available.value = false
                    onAnswer(PlayAnswer.Unavailable)
                }
            }

            override fun onBillingServiceDisconnected() {
                // Reconnection is automatic (enableAutoServiceReconnection); the next call
                // re-establishes it.
                connecting = false
            }
        })
    }

    /** Ask Play what is owned now. */
    fun refresh() {
        if (!client.isReady) return start()
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()
        client.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                handle(purchases, answered = true)
            } else {
                onAnswer(PlayAnswer.Unavailable)
            }
        }
    }

    /**
     * Back in the app: ask again, if already connected. A promo code redeemed in the Play
     * Store, or a subscription bought on another phone, shows up here without a restart.
     */
    fun recheck() {
        if (client.isReady) refresh()
    }

    /** Ask Play what each plan costs here, in the local currency, with any free start. */
    fun loadOffers() {
        if (!client.isReady) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(Premium.PRODUCT_ID)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        client.queryProductDetailsAsync(params) { result, details ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryProductDetailsAsync
            val product = details.productDetailsList.firstOrNull() ?: return@queryProductDetailsAsync
            val all = product.subscriptionOfferDetails.orEmpty()
            // A plan only appears once its base plan is active in Play Console, so a yearly
            // plan that is not set up yet is simply not offered.
            _offers.value = Premium.Plan.entries.mapNotNull { plan ->
                val withPhases = all.filter { it.basePlanId == plan.basePlan }.map { o ->
                    o to o.pricingPhases.pricingPhaseList.map { p ->
                        PricePhase(
                            formattedPrice = p.formattedPrice,
                            priceMicros = p.priceAmountMicros,
                            billingPeriod = p.billingPeriod,
                            cycles = p.billingCycleCount,
                            recurring = p.recurrenceMode == ProductDetails.RecurrenceMode.INFINITE_RECURRING
                        )
                    }
                }
                // Play only returns offers this person is eligible for, so a free trial here
                // is one they can actually have. Otherwise the plain base plan.
                val chosen = withPhases.firstOrNull { (o, phases) -> o.offerId != null && Offers.hasFreeTrial(phases) }
                    ?: withPhases.firstOrNull { (o, _) -> o.offerId == null }
                    ?: withPhases.firstOrNull()
                    ?: return@mapNotNull null
                val (o, phases) = chosen
                plan to Offer(plan, product, o.offerToken, phases)
            }.toMap()
        }
    }

    /** Open Google Play's own subscribe sheet for a plan. False when Play has not offered it yet. */
    fun buy(activity: Activity, plan: Premium.Plan): Boolean {
        val o = _offers.value[plan] ?: return false
        _notice.value = null
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(o.product)
                        .setOfferToken(o.token)
                        .build()
                )
            )
            .build()
        return client.launchBillingFlow(activity, params).responseCode == BillingClient.BillingResponseCode.OK
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> handle(purchases.orEmpty(), answered = false)
            BillingClient.BillingResponseCode.USER_CANCELED -> Unit
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> refresh()
            else -> _notice.value = "Google Play could not finish that. Nothing was charged."
        }
    }

    /**
     * Turn purchases into an answer. A query that comes back empty is a real "not owned";
     * an update that comes back empty is only a sheet that closed. Either plan is Premium:
     * they are base plans of the same subscription.
     */
    private fun handle(purchases: List<Purchase>, answered: Boolean) {
        val mine = purchases.filter { Premium.PRODUCT_ID in it.products }
        mine.filter { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isAcknowledged }
            .forEach { acknowledge(it) }
        val paid = mine.firstOrNull { it.purchaseState == Purchase.PurchaseState.PURCHASED && !it.isSuspended }
        when {
            paid != null -> {
                _notice.value = null
                onAnswer(PlayAnswer.Owned(paid.purchaseToken, paid.purchaseTime))
            }
            mine.any { it.purchaseState == Purchase.PurchaseState.PENDING } ->
                _notice.value = "Your payment is still going through. Premium starts the moment it does."
            answered -> onAnswer(PlayAnswer.NotOwned)
        }
    }

    private fun acknowledge(p: Purchase) {
        val params = AcknowledgePurchaseParams.newBuilder().setPurchaseToken(p.purchaseToken).build()
        client.acknowledgePurchase(params) { _ -> }
    }

    fun end() {
        client.endConnection()
    }
}
