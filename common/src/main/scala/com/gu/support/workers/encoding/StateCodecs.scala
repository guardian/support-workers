package com.gu.support.workers.encoding

import com.gu.salesforce.Salesforce._
import com.gu.support.workers.encoding.Helpers.deriveCodec
import com.gu.support.workers.model.Status
import com.gu.support.workers.model.states._
import com.gu.zuora.encoding.CustomCodecs._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.syntax._

object StateCodecs {

  implicit val encodeStatus: Encoder[Status] = Encoder.encodeString.contramap[Status](_.asString)

  implicit val decodeStatus: Decoder[Status] =
    Decoder.decodeString.emap { identifier => Status.fromString(identifier).toRight(s"Unrecognised status '$identifier'") }

  implicit val createPaymentMethodStateEncoder: Encoder[CreatePaymentMethodState] = deriveEncoder
  //We need a custom decoder for CreatePaymentMethodState because we use it to decode old executions of the state machine
  //in StepFunctionsService.decodeInput so we will need to support the old schema for quite a while
  implicit val createPaymentMethodStateDecoder: Decoder[CreatePaymentMethodState] = deriveDecoder[CreatePaymentMethodState].prepare {
    top =>
      val contribution = top.downField("contribution").as[Json]
      contribution.fold(
        _ => top, //This input doesn't contain the contribution key so it is the new schema
        contributionJson => top.withFocus(convertContributionToProduct(_, contributionJson)) //This input does contain the contribution key, migrate it
      )
  }

  private def convertContributionToProduct(top: Json, contributionJson: Json) =
    top.asObject.map(topObject =>
      topObject
        .add("product", contributionJson)
        .remove("contribution")
        .asJson
    ).getOrElse(top)

  implicit val createSalesforceContactStateCodec: Codec[CreateSalesforceContactState] = deriveCodec
  implicit val createZuoraSubscriptionStateCodec: Codec[CreateZuoraSubscriptionState] = deriveCodec
  implicit val sendThankYouEmailStateCodec: Codec[SendThankYouEmailState] = deriveCodec
  implicit val failureHandlerStateCodec: Codec[FailureHandlerState] = deriveCodec
  implicit val completedStateCodec: Codec[CompletedState] = deriveCodec[CompletedState]
  implicit val sendAcquisitionEventStateDecoder: Decoder[SendAcquisitionEventState] = deriveDecoder
}
