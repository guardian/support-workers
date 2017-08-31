package com.gu.support.workers

import com.gu.support.workers.encoding.StateCodecs._
import com.gu.support.workers.encoding.StateMigration.migrate
import com.gu.support.workers.model.states.CreatePaymentMethodState
import com.typesafe.scalalogging.LazyLogging
import io.circe.parser.decode
import org.scalatest.{FlatSpec, Matchers}

class StateMigrationSpec extends FlatSpec with Matchers with LazyLogging {
  val oldJson =
    """
      {
        "requestId": "e18f6418-45f2-11e7-8bfa-8faac2182601",
        "user": {
          "id": "30000264",
          "primaryEmailAddress": "test@gu.com",
          "firstName": "test",
          "lastName": "user",
          "country": "GB",
          "allowMembershipMail": false,
          "allowThirdPartyMail": false,
          "allowGURelatedMail": false,
          "isTestUser": false
        },
        "contribution": {
          "amount": 5,
          "currency": "GBP"
        },
        "paymentFields": {
          "userId": "12345",
          "stripeToken": "tok_AXY4M16p60c2sg"
        }
      }
    """

  "StateMigrations" should "migrate old state to new" in {

    val state = decode[CreatePaymentMethodState](migrate(oldJson))
    state.right shouldBe true

  }

  it should "leave new state unchanged" in {
    migrate(oldJson) should be(oldJson)
  }

}
