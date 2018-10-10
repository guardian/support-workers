package com.gu.zuora

import java.time.LocalDateTime

import com.gu.config.Configuration.zuoraConfigProvider
import com.gu.i18n.Currency.{AUD, EUR, GBP, USD}
import com.gu.monitoring.SafeLogger
import com.gu.monitoring.SafeLogger._
import com.gu.okhttp.RequestRunners
import com.gu.support.workers.lambdas.GetRecurringSubscription
import com.gu.support.workers.model.Monthly
import com.gu.test.tags.annotations.IntegrationTest
import com.gu.zuora.Fixtures._
import com.gu.zuora.model.SubscribeRequest
import com.gu.zuora.model.response.ZuoraErrorResponse
import org.scalatest.{AsyncFlatSpec, Matchers}

import scala.concurrent.duration._

@IntegrationTest
class ZuoraSpec extends AsyncFlatSpec with Matchers {

  def uatService: ZuoraService = new ZuoraService(zuoraConfigProvider.get(true), RequestRunners.configurableFutureRunner(30.seconds))

  "ZuoraService" should "retrieve an account" in {
    uatService.getAccount(Fixtures.accountNumber).map {
      response =>
        response.success should be(true)
        response.basicInfo.accountNumber should be(Fixtures.accountNumber)
    }
  }

  it should "retrieve account ids from an Identity id" in {
    uatService.getAccountFields("30001758").map {
      response =>
        response.nonEmpty should be(true)
    }
  }

  it should "be resistant to 'ZOQL injection'" in {
    a[NumberFormatException] should be thrownBy uatService.getAccountFields("30000701' or status = 'Active")
  }

  it should "retrieve subscriptions from an account id" in {
    uatService.getSubscriptions("A00071408").map {
      response =>
        response.nonEmpty should be(true)
        response.head.ratePlans.head.productRatePlanId should be(zuoraConfigProvider.get(true).monthlyContribution.productRatePlanId)
    }
  }

  it should "be able to find a monthly recurring subscription just after creation" in {
    val justAfterCreation = LocalDateTime.of(2017, 12, 7, 15, 47, 22)
    GetRecurringSubscription(uatService, justAfterCreation, "30001758", Monthly).map {
      _.flatMap(_.ratePlans.headOption.map(_.productName)) should be(Some("Contributor"))
    }
  }

  it should "ignore a monthly recurring subscription that was created earlier" in {
    val aWhileAfterCreation = LocalDateTime.of(2017, 12, 7, 16, 47, 22)
    GetRecurringSubscription(uatService, aWhileAfterCreation, "30001758", Monthly).map {
      _ should be(None)
    }
  }

  it should "ignore active subscriptions which do not have a recurring contributor plan" in {
    val justAfterCreation = LocalDateTime.of(2017, 12, 7, 16, 34, 47)
    GetRecurringSubscription(uatService, justAfterCreation, "18390845", Monthly).map {
      _ should be(None)
    }
  }

  it should "ignore cancelled recurring contributions" in {
    val justAfterCreation = LocalDateTime.of(2017, 12, 7, 14, 22, 36)
    GetRecurringSubscription(uatService, justAfterCreation, "30001780", Monthly).map {
      _ should be(None)
    }
  }

  it should "retrieve a default paymentMethodId from an account number" in {
    val accountNumber = "A00072689"
    val defaultPaymentMethodId = Some("2c92c0f9624bbc6c01624eac30f86724")
    uatService.getDefaultPaymentMethodId(accountNumber).map {
      response =>
        response should be(defaultPaymentMethodId)
    }
  }

  it should "retrieve a Direct Debit mandateId from a valid paymentMethodId" in {
    val defaultPaymentMethodId = "2c92c0f9624bbc6c01624eac30f86724"
    val mandateId = "65HK26E"
    uatService.getDirectDebitMandateId(defaultPaymentMethodId).map {
      response =>
        response should be(Some(mandateId))
    }
  }

  it should "return None when given an invalid paymentMethodId" in {
    val invalidPaymentMethodId = "xxxx"
    uatService.getDirectDebitMandateId(invalidPaymentMethodId).map {
      response =>
        response should be(None)
    }
  }

  it should "retrieve a Direct Debit mandateId from a valid account number" in {
    val accountNumber = "A00072689"
    val mandateId = Some("65HK26E")
    uatService.getMandateIdFromAccountNumber(accountNumber).map {
      response =>
        response should be(mandateId)
    }
  }

  it should "return None when given an invalid account number" in {
    val invalidAccountNumber = "xxxx"
    uatService.getMandateIdFromAccountNumber(invalidAccountNumber).map {
      response =>
        response should be(None)
    }
  }

  "Subscribe request" should "succeed" in doRequest(creditCardSubscriptionRequest(GBP))

  it should "work for $USD contributions" in doRequest(creditCardSubscriptionRequest(USD))

  it should "work for €Euro contributions" in doRequest(creditCardSubscriptionRequest(EUR))

  it should "work for AUD contributions" in doRequest(creditCardSubscriptionRequest(AUD))

  it should "work with Direct Debit" in doRequest(directDebitSubscriptionRequest)

  def doRequest(subscribeRequest: SubscribeRequest) = {
    //Accounts will be created in Sandbox
    val zuoraService = new ZuoraService(zuoraConfigProvider.get(), RequestRunners.configurableFutureRunner(30.seconds))
    zuoraService.subscribe(subscribeRequest).map {
      response =>
        response.head.success should be(true)
    }.recover {
      case e: ZuoraErrorResponse => SafeLogger.error(scrub"Zuora error: $e", e); fail()
    }
  }
}
