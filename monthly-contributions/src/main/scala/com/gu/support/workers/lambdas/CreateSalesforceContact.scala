package com.gu.support.workers.lambdas

import com.amazonaws.services.lambda.runtime.Context
import com.gu.monitoring.products.RecurringContributionsMetrics
import com.gu.salesforce.Salesforce.{SalesforceContactResponse, UpsertData}
import com.gu.services.Services
import com.gu.support.workers.encoding.StateCodecs._
import com.gu.support.workers.exceptions.SalesforceException
import com.gu.support.workers.model.monthlyContributions.state.{CreateSalesforceContactState, CreateZuoraSubscriptionState}
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class CreateSalesforceContact extends ServicesHandler[CreateSalesforceContactState, CreateZuoraSubscriptionState] with LazyLogging {

  override protected def servicesHandler(state: CreateSalesforceContactState, context: Context, services: Services): Future[CreateZuoraSubscriptionState] = {
    logger.debug(s"CreateSalesforceContact state: $state")
    services.salesforceService.upsert(UpsertData.create(
      state.user.id,
      state.user.primaryEmailAddress,
      state.user.firstName,
      state.user.lastName,
      state.user.state,
      state.user.country.name,
      state.user.allowMembershipMail,
      state.user.allowThirdPartyMail,
      state.user.allowGURelatedMail
    )).map(response =>
      if (response.Success) {
        putSalesForceContactCreated(state.paymentMethod.`type`)
        getCreateZuoraSubscriptionState(state, response)
      } else {
        val errorMessage = response.ErrorString.getOrElse("No error message returned")
        logger.warn(s"Error creating Salesforce contact:\n$errorMessage")
        throw new SalesforceException(errorMessage)
      })
  }

  private def getCreateZuoraSubscriptionState(state: CreateSalesforceContactState, response: SalesforceContactResponse) =
    CreateZuoraSubscriptionState(
      state.requestId,
      state.user,
      state.contribution,
      state.paymentMethod,
      response.ContactRecord,
      state.acquisitionData
    )

  def putSalesForceContactCreated(paymentMethod: String): Future[Unit] = {
    val paymentMethodLabel = {
      if(paymentMethod.contains("CreditCardReferenceTransaction"))
        "stripe"
      else
        "paypal"
    }
    new RecurringContributionsMetrics(paymentMethodLabel, "monthly")
      .putSalesforceContactCreated().recover({ case _ => () })
  }

}
