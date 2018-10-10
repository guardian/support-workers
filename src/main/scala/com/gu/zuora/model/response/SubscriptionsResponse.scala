package com.gu.zuora.model.response

import com.gu.support.workers.encoding.Codec
import com.gu.support.workers.encoding.Helpers.deriveCodec
import com.gu.support.workers.model.BillingPeriod
import com.gu.zuora.ZuoraConfig

object SubscriptionsResponse {
  implicit val codec: Codec[SubscriptionsResponse] = deriveCodec
}

object Subscription {
  implicit val codec: Codec[Subscription] = deriveCodec
}

object RatePlan {
  implicit val codec: Codec[RatePlan] = deriveCodec
}

case class SubscriptionsResponse(subscriptions: List[Subscription])

case class Subscription(accountNumber: String, status: String, ratePlans: List[RatePlan]) {
  def isActive: Boolean = status == "Active"
}

case class RatePlan(productId: String, productName: String, productRatePlanId: String)

