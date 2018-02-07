package com.gu.support.workers.lambdas

import cats.implicits._
import com.amazonaws.services.lambda.runtime.Context
import com.gu.config.Configuration
import com.gu.emailservices.{EmailFields, EmailService}
import com.gu.helpers.FutureExtensions._
import com.gu.stripe.Stripe.StripeError
import com.gu.support.workers.encoding.ErrorJson
import com.gu.support.workers.encoding.StateCodecs._
import com.gu.support.workers.model.states.{CompletedState, FailureHandlerState}
import com.gu.support.workers.model.{ExecutionError, RequestInfo, Status}
import com.gu.zuora.model.response.{ZuoraError, ZuoraErrorResponse}
import com.typesafe.scalalogging.LazyLogging
import io.circe.Decoder
import io.circe.parser.decode
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global

class FailureHandler(emailService: EmailService)
    extends FutureHandler[FailureHandlerState, CompletedState]
    with LazyLogging {
  def this() = this(new EmailService(Configuration.emailServicesConfig.failed))

  override protected def handlerFuture(
    state: FailureHandlerState,
    error: Option[ExecutionError],
    requestInfo: RequestInfo,
    context: Context
  ): FutureHandlerResult = {
    logger.info(
      s"FAILED product: ${state.product.describe} test_user: ${state.user.isTestUser}"
    )
    sendEmail(state).whenFinished(handleError(state, error, requestInfo))
  }

  private def sendEmail(state: FailureHandlerState) = emailService.send(EmailFields(
    email = state.user.primaryEmailAddress,
    created = DateTime.now(),
    amount = 0, //TODO: Not used by email & digital pack doesn't have it
    currency = state.product.currency.iso,
    edition = state.user.country.alpha2,
    name = state.user.firstName,
    product = "monthly-contribution"
  ))

  private def handleError(state: FailureHandlerState, error: Option[ExecutionError], requestInfo: RequestInfo) =
    error.flatMap(extractUnderlyingError) match {
      case Some(ZuoraErrorResponse(_, List(ze @ ZuoraError("TRANSACTION_FAILED", _)))) => returnState(
        state,
        requestInfo.appendMessage(s"Zuora reported a payment failure: $ze")
      )
      case Some(se @ StripeError("card_error", _, _, _, _)) => returnState(
        state,
        requestInfo.appendMessage(s"Stripe reported a payment failure: ${se.getMessage}")
      )
      case _ => returnState(state, requestInfo.copy(failed = true),
        "There was an error processing your payment. Please\u00a0try\u00a0again\u00a0later.")
    }

  private def returnState(
    state: FailureHandlerState,
    requestInfo: RequestInfo,
    message: String = "There was an error processing your payment. Please\u00a0try\u00a0again."
  ) =
    HandlerResult(
      CompletedState(
        requestId = state.requestId,
        user = state.user,
        product = state.product,
        status = Status.Failure,
        message = Some(message)
      ), requestInfo
    )

  private def extractUnderlyingError(executionError: ExecutionError): Option[Throwable] = for {
    errorJson <- decode[ErrorJson](executionError.Cause).toOption
    result <- tryToDecode[ZuoraErrorResponse](errorJson) orElse tryToDecode[StripeError](errorJson)
  } yield result

  private def tryToDecode[T](errorJson: ErrorJson)(implicit decoder: Decoder[T]): Option[T] = decode[T](errorJson.errorMessage).toOption
}
