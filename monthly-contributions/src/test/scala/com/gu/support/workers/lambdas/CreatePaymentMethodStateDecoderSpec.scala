package com.gu.support.workers.lambdas

import java.util.UUID

import com.gu.i18n.Currency.GBP
import com.gu.i18n.{Country, Currency}
import com.gu.support.workers.Fixtures.{validBaid, _}
import com.gu.support.workers.encoding.StateCodecs._
import com.gu.support.workers.model._
import com.gu.support.workers.model.states.CreatePaymentMethodState
import com.gu.zuora.encoding.CustomCodecs._
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class CreatePaymentMethodStateDecoderSpec extends FlatSpec with Matchers with MockitoSugar with LazyLogging {

  "Encode" should "work" in {
    val state = CreatePaymentMethodState(UUID.randomUUID(),
      User("123", "test@gu.com", "test", "user", Country.UK, None, allowMembershipMail = true,
        allowThirdPartyMail = false, allowGURelatedMail = true, isTestUser = true),
      Contribution(Currency.GBP, Monthly, 5),
      PayPalPaymentFields(validBaid))
    logger.info(s"${state.asJson}")
  }

  "Product" should "be decodable" in {
    val product: ProductType = Contribution(GBP, Monthly, 5)
    val json = product.asJson
    logger.info(json.spaces2)
    /*
    {
      "currency" : "GBP",
      "period" : "Monthly",
      "amount" : 5,
      "type" : "Contribution"
    }
     */
    val product2 = decode[ProductType](json.noSpaces)
    product2.isRight should be(true) //decoding succeeded
  }

  "CreatePaymentMethodStateDecoder" should "be able to decode a CreatePaymentMethodStateDecoder with PayPal payment fields" in {
    val state = decode[CreatePaymentMethodState](createPayPalPaymentMethodJson)
    val result = state.right.get
    result.product match {
      case contribution: Contribution => contribution.amount should be(5)
      case _ => fail()
    }
    result.paymentFields match {
      case paypal: PayPalPaymentFields => paypal.baid should be(validBaid)
      case _ => fail()
    }

  }

  it should "be able to decode a CreatePaymentMethodStateDecoder with Stripe payment fields" in {
    val state = decode[CreatePaymentMethodState](createStripePaymentMethodJson)
    val result = state.right.get
    result.product match {
      case contribution: Contribution => contribution.amount should be(5)
      case _ => fail()
    }
    result.paymentFields match {
      case stripe: StripePaymentFields => stripe.stripeToken should be(stripeToken)
      case _ => fail()
    }
  }

}
