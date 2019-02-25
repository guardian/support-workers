package com.gu.support.workers.lambdas

import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.sqs.model.SendMessageResult
import com.gu.emailservices.{ContributionEmailFields, DigitalPackEmailFields, EmailService}
import com.gu.monitoring.SafeLogger
import com.gu.salesforce.Salesforce.SfContactId
import com.gu.services.{ServiceProvider, Services}
import com.gu.support.encoding.CustomCodecs._
import com.gu.support.workers._
import com.gu.support.workers.states.SendThankYouEmailState
import com.gu.threadpools.CustomPool.executionContext
import com.gu.zuora.ZuoraService
import io.circe.generic.auto._
import org.joda.time.DateTime

import scala.concurrent.Future

class SendThankYouEmail(thankYouEmailService: EmailService, servicesProvider: ServiceProvider = ServiceProvider)
    extends ServicesHandler[SendThankYouEmailState, SendMessageResult](servicesProvider) {

  def this() = this(new EmailService)

  override protected def servicesHandler(
    state: SendThankYouEmailState,
    requestInfo: RequestInfo,
    context: Context,
    services: Services
  ): FutureHandlerResult = {
    SafeLogger.info(s"Number of available processors: ${Runtime.getRuntime.availableProcessors()}")
    for {
      mandateId <- fetchDirectDebitMandateId(state, services.zuoraService)
      emailResult <- sendEmail(state, mandateId)
    } yield HandlerResult(emailResult, requestInfo)
  }

  def fetchDirectDebitMandateId(state: SendThankYouEmailState, zuoraService: ZuoraService): Future[Option[String]] = state.paymentMethod match {
    case _: DirectDebitPaymentMethod =>
      zuoraService.getMandateIdFromAccountNumber(state.accountNumber)
    case _ => Future.successful(None)
  }

  def sendEmail(state: SendThankYouEmailState, directDebitMandateId: Option[String] = None): Future[SendMessageResult] =
    thankYouEmailService.send(
      state.product match {
        case c: Contribution => ContributionEmailFields(
          email = state.user.primaryEmailAddress,
          created = DateTime.now(),
          amount = c.amount,
          currency = c.currency,
          edition = state.user.billingAddress.country.alpha2,
          name = state.user.firstName,
          billingPeriod = state.product.billingPeriod,
          sfContactId = SfContactId(state.salesForceContact.Id),
          paymentMethod = state.paymentMethod,
          directDebitMandateId = directDebitMandateId
        )
        case d: DigitalPack => DigitalPackEmailFields(
          subscriptionNumber = state.subscriptionNumber,
          billingPeriod = d.billingPeriod,
          user = state.user,
          paymentSchedule = state.paymentSchedule,
          currency = d.currency,
          paymentMethod = state.paymentMethod,
          directDebitMandateId = directDebitMandateId,
          sfContactId = SfContactId(state.salesForceContact.Id)
        )
        case p: Paper => DigitalPackEmailFields(
          subscriptionNumber = state.subscriptionNumber,
          billingPeriod = p.billingPeriod,
          user = state.user,
          paymentSchedule = state.paymentSchedule,
          currency = p.currency,
          paymentMethod = state.paymentMethod,
          directDebitMandateId = directDebitMandateId,
          sfContactId = SfContactId(state.salesForceContact.Id)
        ) //todo: future card handles the thank you email
      }
    )

}
