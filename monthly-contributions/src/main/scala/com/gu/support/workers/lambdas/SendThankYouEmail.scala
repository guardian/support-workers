package com.gu.support.workers.lambdas

import com.amazonaws.services.lambda.runtime.Context
import com.gu.config.Configuration
import com.gu.emailservices.{EmailFields, EmailService}
import com.gu.support.workers.encoding.StateCodecs._
import com.gu.support.workers.model.ExecutionError
import com.gu.support.workers.model.states.SendThankYouEmailState
import com.gu.zuora.encoding.CustomCodecs._
import com.typesafe.scalalogging.LazyLogging
import org.joda.time.DateTime

import scala.concurrent.ExecutionContext.Implicits.global

class SendThankYouEmail(thankYouEmailService: EmailService)
    extends FutureHandler[SendThankYouEmailState, Unit] with LazyLogging {

  def this() = this(new EmailService(Configuration.emailServicesConfig.thankYou))

  override protected def handlerFuture(
    state: SendThankYouEmailState,
    error: Option[ExecutionError],
    requestInfo: RequestInfo,
    context: Context
  ): FutureHandlerResult =
    sendEmail(state, requestInfo)

  def sendEmail(state: SendThankYouEmailState, requestInfo: RequestInfo): FutureHandlerResult = {
    thankYouEmailService.send(EmailFields(
      email = state.user.primaryEmailAddress,
      created = DateTime.now(),
      amount = 0, //TODO? It's not actually used by the email, maybe remove it?
      currency = state.product.currency.iso,
      edition = state.user.country.alpha2,
      name = state.user.firstName,
      product = "monthly-contribution"
    )).map(_ => HandlerResult(Unit, requestInfo))
  }
}
