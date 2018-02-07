package com.gu.support.workers.lambdas

import com.gu.i18n.Currency.GBP
import com.gu.support.workers.Fixtures.{validBaid, _}
import com.gu.support.workers.encoding.StateCodecs._
import com.gu.support.workers.model.monthlyContributions.state.CreatePaymentMethodState
import com.gu.support.workers.model.{PayPalPaymentFields, StripePaymentFields}
import com.gu.zuora.encoding.CustomCodecs._
import com.typesafe.scalalogging.LazyLogging
import io.circe.Json
import io.circe.parser._
import io.circe.syntax._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class CreatePaymentMethodStateDecoderSpec extends FlatSpec with Matchers with MockitoSugar with LazyLogging {

  "Monthly Contribution Product" should "be decodable" in {
    val product: ProductType = Contribution(GBP, Monthly, 5)
    assertCodecIsValid(product.asJson)
    /*
    {
      "currency" : "GBP",
      "period" : "Monthly",
      "amount" : 5,
      "type" : "Contribution"
    }
     */
  }

  "Annual DigitalBundle Product" should "be decodable" in {
    val product: ProductType = DigitalBundle(GBP, Annual)
    assertCodecIsValid(product.asJson)
    /*
    {
      "currency" : "GBP",
      "period" : "Annual",
      "type" : "DigitalBundle"
    }
     */
  }

  "Quarterly Contribution Product" should "be decodable" in {
    val product: ProductType = Contribution(GBP, Quarterly, 150)
    assertCodecIsValid(product.asJson)
    /*
    {
      "currency" : "GBP",
      "period" : "Quarterly",
      "amount" : 150,
      "type" : "Contribution"
    }
     */
  }

  private def assertCodecIsValid(json: Json) = {
    logger.info(json.spaces2)
    val product2 = decode[ProductType](json.noSpaces)
    product2.isRight should be(true) //decoding succeeded
  }

  "CreatePaymentMethodStateDecoder" should "be able to decode a contribution with PayPal payment fields" in {
    val state = decode[CreatePaymentMethodState](createPayPalPaymentMethodDigitalBundleJson)
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

  it should "be able to decode a contribution with Stripe payment fields" in {
    val state = decode[CreatePaymentMethodState](createStripePaymentMethodContributionJson)
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

  it should "be able to decode a DigtalBundle with PayPal payment fields" in {
    val state = decode[CreatePaymentMethodState](createPayPalPaymentMethodDigitalBundleJson)
    val result = state.right.get
    result.product match {
      case digitalBundle: DigitalBundle => digitalBundle.period should be(Annual)
      case _ => fail()
    }
    result.paymentFields match {
      case paypal: PayPalPaymentFields => paypal.baid should be(validBaid)
      case _ => fail()
    }
  }

  it should "be able to decode a DigtalBundle with Direct Debit payment fields" in {
    val state = decode[CreatePaymentMethodState](createDirectDebitDigitalBundleJson)
    val result = state.right.get
    result.product match {
      case digitalBundle: DigitalBundle => digitalBundle.period should be(Annual)
      case _ => fail()
    }
    result.paymentFields match {
      case dd: DirectDebitPaymentFields => dd.accountName should be(mickeyMouse)
      case _ => fail()
    }
  }

}
