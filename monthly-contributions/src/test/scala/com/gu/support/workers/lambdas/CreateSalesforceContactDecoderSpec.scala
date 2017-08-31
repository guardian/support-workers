package com.gu.support.workers.lambdas

import com.gu.support.workers.Fixtures._
import com.gu.support.workers.encoding.StateCodecs._
import com.gu.support.workers.model.states.CreateSalesforceContactState
import com.gu.support.workers.model.{Contribution, PayPalReferenceTransaction, PaymentMethod}
import com.gu.zuora.encoding.CustomCodecs._
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser._
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{FlatSpec, Matchers}

class CreateSalesforceContactDecoderSpec extends FlatSpec with Matchers with MockitoSugar with LazyLogging {

  "CreateSalesforceContactDecoder" should "be able to decode a CreateSalesforceContactState" in {
    val state = decode[CreateSalesforceContactState](createSalesForceContactJson)
    val result = state.right.get
    result.product match {
      case contribution: Contribution => contribution.amount should be(5)
      case _ => fail()
    }
    result.paymentMethod match {
      case payPal: PayPalReferenceTransaction => succeed
      case _ => fail()
    }
  }

  it should "fail when given duff json" in {
    val duffJson = """
                {
                  "aintIt": "Funky"
                }
                """
    val result = decode[CreateSalesforceContactState](duffJson)
    result.isLeft should be(true)
  }

  "Decoder" should "be able to decode PaymentMethod" in {
    val result = decode[PaymentMethod](payPalPaymentMethod)
    result.isRight should be(true)
  }
}
