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
import scala.concurrent.Future

class SendThankYouEmail(thankYouEmailService: EmailService)
    extends FutureHandler[SendThankYouEmailState, Unit]
    with LazyLogging {
  def this() = this(new EmailService(Configuration.emailServicesConfig.thankYou))

  override protected def handlerFuture(state: SendThankYouEmailState, error: Option[ExecutionError], context: Context): Future[Unit] = {
    sendEmail(state)
  }

  def sendEmail(state: SendThankYouEmailState): Future[Unit] = {
    thankYouEmailService.send(EmailFields(
      email = state.user.primaryEmailAddress,
      created = DateTime.now(),
      amount = 0, //TODO? It's not actually used by the email, maybe remove it?
      currency = state.product.currency.iso,
      edition = state.user.country.alpha2,
      name = state.user.firstName,
      product = state.product.toString
    )).map(_ => Unit)
  }
}
