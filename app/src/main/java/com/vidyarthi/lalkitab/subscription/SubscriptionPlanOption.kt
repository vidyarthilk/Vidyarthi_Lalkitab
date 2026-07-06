package com.vidyarthi.lalkitab.subscription

/** One Play Console base plan offer for [SubscriptionBilling]. */
data class SubscriptionPlanOption(
    val basePlanId: String,
    val label: String,
    val priceText: String,
    val offerToken: String
)
