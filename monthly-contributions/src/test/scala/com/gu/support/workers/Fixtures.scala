package com.gu.support.workers

import java.io.ByteArrayInputStream

import com.gu.salesforce.Fixtures.idId
import com.gu.support.workers.Conversions.StringInputStreamConversions
import com.gu.support.workers.encoding.Wrapper
import com.gu.support.workers.encoding.Wrapper.jsonCodec
import io.circe.syntax._

//noinspection TypeAnnotation
object Fixtures {
  def wrapFixture(string: String): ByteArrayInputStream = Wrapper.wrapString(string).asJson.noSpaces.asInputStream

  val userJson =
    s"""
      "user":{
          "id": "$idId",
          "primaryEmailAddress": "test@gu.com",
          "firstName": "test",
          "lastName": "user",
          "country": "GB",
          "allowMembershipMail": false,
          "allowThirdPartyMail": false,
          "allowGURelatedMail": false,
          "isTestUser": false
        }
    """
  val requestIdJson = "\"requestId\": \"e18f6418-45f2-11e7-8bfa-8faac2182601\""
  val validBaid = "B-23637766K5365543J"
  val payPalEmail = "test@paypal.com"
  val payPalPaymentMethod =
    s"""
        {
              "PaypalBaid": "$validBaid",
              "PaypalEmail": "$payPalEmail",
              "PaypalType": "ExpressCheckout",
              "Type": "PayPal"
         }
       """

  val contributionJson =
    """
      {
        "amount": 5,
        "currency": "GBP",
        "period" : "Monthly",
        "type" : "Contribution"
      }
    """

  val digitalBundleJson =
    """
      {
        "currency": "GBP",
        "period" : "Annual",
        "type" : "DigitalBundle"
      }
    """

  val contributionProductJson =
    s"""
      "product": $contributionJson
    """

  val digitalBundleProductJson =
    s"""
      "product": $digitalBundleJson
    """

  val payPalJson =
    s"""
      {
        "baid": "$validBaid"
      }
    """

  val mickeyMouse = "Mickey Mouse"
  val directDebitJson =
    s"""
      {
        "sortcode": "111111",
        "accountNumber": "99999999",
        "accountName": "$mickeyMouse"
      }
    """

  val stripeToken = "tok_AXY4M16p60c2sg"
  val stripeJson =
    s"""
      {
        "userId": "12345",
        "stripeToken": "$stripeToken"
      }
    """

  val createPayPalPaymentMethodContributionJson =
    s"""{
          $requestIdJson,
          $userJson,
          $contributionProductJson,
          "paymentFields": $payPalJson
        }"""

  val createStripePaymentMethodContributionJson =
    s"""{
          $requestIdJson,
          $userJson,
          $contributionProductJson,
          "paymentFields": $stripeJson
        }"""

  val createPayPalPaymentMethodDigitalBundleJson =
    s"""{
          $requestIdJson,
          $userJson,
          $digitalBundleProductJson,
          "paymentFields": $payPalJson
        }"""

  val createDirectDebitDigitalBundleJson =
    s"""{
          $requestIdJson,
          $userJson,
          $digitalBundleProductJson,
          "paymentFields": $directDebitJson
        }"""

  val createSalesForceContactJson =
    s"""
          {
            $requestIdJson,
            $userJson,
            $contributionProductJson,
            "paymentMethod": $payPalPaymentMethod
          }
        """

  val thankYouEmailJson =
    s"""{
       |  $requestIdJson,
       |  $userJson,
       |  $contributionProductJson,
       |  "paymentMethod": $payPalPaymentMethod,
       |  "salesForceContact": {
       |    "Id": "123",
       |    "AccountId": "123"
       |  },
       |  "accountNumber": "123"
       |}
     """.stripMargin

  val updateMembersDataAPIJson =
    s"""{
       |  $requestIdJson,
       |  $userJson
       |}
     """.stripMargin

  val salesforceContactJson =
    """
        {
          "Id": "003g000001UnFItAAN",
          "AccountId": "001g000001gOR06AAG"
        }
      """
  val createZuoraSubscriptionJson =
    s"""
          {
            $requestIdJson,
            $userJson,
            $contributionProductJson,
            "paymentMethod": $payPalPaymentMethod,
            "salesForceContact": $salesforceContactJson
            }
        """

  val stripeCardDeclinedErrorJson =
    s"""
       {
       }
     """

  val failureJson =
    s"""{
       |  $requestIdJson,
       |  $userJson,
       |  $contributionProductJson,
       |  "error": {
       |    "Error": "com.gu.support.workers.exceptions.ErrorHandler.logAndRethrow(ErrorHandler.scala:33)",
       |    "Cause": "The card has expired"
       |  }
       |}
     """.stripMargin
}
