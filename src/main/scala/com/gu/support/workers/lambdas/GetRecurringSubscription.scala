package com.gu.support.workers.lambdas

import java.time.LocalDateTime

import cats.implicits._
import com.gu.support.workers.model.BillingPeriod
import com.gu.zuora.GetAccountForIdentity.DomainAccount
import com.gu.zuora.ZuoraConfig.RatePlanId
import com.gu.zuora.ZuoraService
import com.gu.zuora.model.response.{RatePlan, Subscription}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object GetRecurringSubscription {

  def apply(zuoraService: ZuoraService, now: LocalDateTime, identityId: String, billingPeriod: BillingPeriod): Future[Option[Subscription]] = {

    val productRatePlanId = Option(zuoraService.config).map(_.contributionConfig(billingPeriod).productRatePlanId)

    def hasContributorPlan(sub: Subscription): Boolean =
      productRatePlanId match {
        case Some(productRatePlanId) => GetRecurringSubscription.hasContributorPlan(productRatePlanId, sub.ratePlans)
        case None => false
      }

    def isRecent(domainAccount: DomainAccount): Boolean =
      GetRecurringSubscription.isRecent(now, domainAccount.createdDate)

    for {
      accountIds <- zuoraService.getAccountFields(identityId)
      recentAccountIds = accountIds.filter(isRecent).map(_.accountNumber)
      subscriptions <- recentAccountIds.map(zuoraService.getSubscriptions).combineAll
      maybeRecentCont = subscriptions.find(sub => hasContributorPlan(sub) && sub.isActive)
    } yield maybeRecentCont
  }

  def isRecent(now: LocalDateTime, accountCreation: LocalDateTime): Boolean = {
    val oldestRecentTime = now.minusMinutes(10L)
    accountCreation.isAfter(oldestRecentTime)
  }

  def hasContributorPlan(ratePlanId: RatePlanId, ratePlans: List[RatePlan]): Boolean =
    ratePlans.exists(_.productRatePlanId == ratePlanId)

}
