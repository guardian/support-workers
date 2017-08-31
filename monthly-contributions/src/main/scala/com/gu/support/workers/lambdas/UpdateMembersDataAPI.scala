package com.gu.support.workers.lambdas

import com.amazonaws.services.lambda.runtime.Context
import com.gu.monitoring.MembersDataAPIMetrics
import com.gu.services.{ServiceProvider, Services}
import com.gu.support.workers.encoding.StateCodecs._
import com.gu.support.workers.model.states.UpdateMembersDataAPIState
import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class UpdateMembersDataAPI(servicesProvider: ServiceProvider = ServiceProvider)
    extends ServicesHandler[UpdateMembersDataAPIState, Unit](servicesProvider) with LazyLogging {

  def this() = this(ServiceProvider)

  override protected def servicesHandler(state: UpdateMembersDataAPIState, context: Context, services: Services): Future[Unit] = {
    services.membersDataService
      .update(state.user.id, state.user.isTestUser)
      .flatMap(_ => putCloudWatchMetrics)
  }

  def putCloudWatchMetrics(): Future[Unit] =
    new MembersDataAPIMetrics("recurring-contribution")
      .putMembersDataAPIUpdated()
}