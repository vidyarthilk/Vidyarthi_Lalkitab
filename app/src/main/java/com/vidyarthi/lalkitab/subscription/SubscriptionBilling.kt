package com.vidyarthi.lalkitab.subscription

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.vidyarthi.lalkitab.R

/**
 * Google Play subscription for unlimited saved kundlis.
 * Product ID: [R.string.subscription_product_id]
 * Base plans (Play Console): monthly, quarterly, yearly — see strings.xml.
 */
class SubscriptionBilling(
    private val activity: Activity,
    private val onStatusChanged: () -> Unit
) : PurchasesUpdatedListener {

    private val productId = activity.getString(R.string.subscription_product_id)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val billingClient: BillingClient = BillingClient.newBuilder(activity)
        .setListener(this)
        .enablePendingPurchases()
        .build()

    private var productDetails: ProductDetails? = null
    private var onPlansLoaded: (() -> Unit)? = null

    fun start() {
        connectBilling {
            queryProduct()
            refreshEntitlement()
        }
    }

    fun setOnPlansLoaded(listener: () -> Unit) {
        onPlansLoaded = listener
        if (productDetails != null) {
            listener()
        }
    }

    fun getAvailablePlans(): List<SubscriptionPlanOption> {
        val details = productDetails ?: return emptyList()
        val offers = details.subscriptionOfferDetails.orEmpty()
        return planConfig().mapNotNull { (basePlanId, labelRes) ->
            val offer = offers.firstOrNull { it.basePlanId.equals(basePlanId, ignoreCase = true) }
                ?: return@mapNotNull null
            val price = offer.pricingPhases.pricingPhaseList.firstOrNull()?.formattedPrice.orEmpty()
            SubscriptionPlanOption(
                basePlanId = basePlanId,
                label = activity.getString(labelRes),
                priceText = price,
                offerToken = offer.offerToken
            )
        }
    }

    fun launchPurchase(basePlanId: String? = null) {
        val details = productDetails
        if (details == null) {
            onPurchaseMessage(activity.getString(R.string.subscription_billing_not_ready))
            queryProduct()
            return
        }
        val offers = details.subscriptionOfferDetails.orEmpty()
        val targetId = basePlanId?.takeIf { it.isNotBlank() }
            ?: planConfig().firstOrNull()?.first
        val offer = offers.firstOrNull { it.basePlanId.equals(targetId, ignoreCase = true) }
            ?: offers.firstOrNull()
        if (offer == null) {
            onPurchaseMessage(activity.getString(R.string.subscription_billing_not_ready))
            return
        }
        val params = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(
                listOf(
                    BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(details)
                        .setOfferToken(offer.offerToken)
                        .build()
                )
            )
            .build()
        billingClient.launchBillingFlow(activity, params)
    }

    fun refreshEntitlement() {
        if (!billingClient.isReady) return
        billingClient.queryPurchasesAsync(
            QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        ) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val active = purchases.any { isPremiumPurchase(it) }
            SubscriptionManager.setSubscribed(activity, active)
            onStatusChanged()
        }
    }

    override fun onPurchasesUpdated(result: BillingResult, purchases: MutableList<Purchase>?) {
        if (result.responseCode == BillingClient.BillingResponseCode.OK && purchases != null) {
            handlePurchases(purchases)
        } else if (result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED) {
            onPurchaseMessage(activity.getString(R.string.subscription_purchase_cancelled))
        } else if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            onPurchaseMessage(activity.getString(R.string.subscription_purchase_failed))
        }
    }

    private fun handlePurchases(purchases: List<Purchase>) {
        var premium = false
        for (purchase in purchases) {
            if (!isPremiumPurchase(purchase)) continue
            premium = true
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { ack ->
                    if (ack.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.w(TAG, "ack failed: ${ack.debugMessage}")
                    }
                }
            }
        }
        SubscriptionManager.setSubscribed(activity, premium)
        onStatusChanged()
        if (premium) {
            onPurchaseMessage(activity.getString(R.string.subscription_purchase_success))
        }
    }

    private fun isPremiumPurchase(purchase: Purchase): Boolean {
        return purchase.products.contains(productId) &&
            purchase.purchaseState == Purchase.PurchaseState.PURCHASED
    }

    private fun queryProduct() {
        if (!billingClient.isReady) return
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(
                listOf(
                    QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(productId)
                        .setProductType(BillingClient.ProductType.SUBS)
                        .build()
                )
            )
            .build()
        billingClient.queryProductDetailsAsync(params) { result, detailsList ->
            if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                productDetails = detailsList.firstOrNull()
                onPlansLoaded?.invoke()
            }
        }
    }

    private fun planConfig(): List<Pair<String, Int>> = listOf(
        activity.getString(R.string.subscription_base_plan_monthly) to R.string.subscription_plan_monthly,
        activity.getString(R.string.subscription_base_plan_quarterly) to R.string.subscription_plan_quarterly,
        activity.getString(R.string.subscription_base_plan_yearly) to R.string.subscription_plan_yearly
    )

    fun destroy() {
        onPlansLoaded = null
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    private var purchaseMessageHandler: ((String) -> Unit)? = null

    fun setPurchaseMessageHandler(handler: (String) -> Unit) {
        purchaseMessageHandler = handler
    }

    private fun onPurchaseMessage(msg: String) {
        purchaseMessageHandler?.invoke(msg)
    }

    private fun connectBilling(onReady: () -> Unit) {
        if (billingClient.isReady) {
            onReady()
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    onReady()
                }
            }

            override fun onBillingServiceDisconnected() {
                mainHandler.postDelayed({
                    if (!billingClient.isReady) {
                        connectBilling(onReady)
                    }
                }, 2_000L)
            }
        })
    }

    companion object {
        private const val TAG = "SubscriptionBilling"

        fun syncFromPlayIfPossible(context: Context) {
            val client = BillingClient.newBuilder(context)
                .enablePendingPurchases()
                .build()
            client.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        client.endConnection()
                        return
                    }
                    val productId = context.getString(R.string.subscription_product_id)
                    client.queryPurchasesAsync(
                        QueryPurchasesParams.newBuilder()
                            .setProductType(BillingClient.ProductType.SUBS)
                            .build()
                    ) { qResult, purchases ->
                        val active = qResult.responseCode == BillingClient.BillingResponseCode.OK &&
                            purchases.any {
                                it.products.contains(productId) &&
                                    it.purchaseState == Purchase.PurchaseState.PURCHASED
                            }
                        SubscriptionManager.setSubscribed(context, active)
                        client.endConnection()
                    }
                }

                override fun onBillingServiceDisconnected() {
                    client.endConnection()
                }
            })
        }
    }
}
