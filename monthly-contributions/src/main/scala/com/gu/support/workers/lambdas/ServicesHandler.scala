package com.gu.support.workers.lambdas

import com.amazonaws.services.lambda.runtime.Context
import com.gu.services.{ServiceProvider, Services}
import com.gu.support.workers.model.ExecutionError
import com.gu.support.workers.model.states.StepFunctionUserState
import io.circe.{Decoder, Encoder}

import scala.concurrent.duration._
import scala.concurrent.{ExecutionContext, Future}

abstract class ServicesHandler[T <: StepFunctionUserState, R](servicesProvider: ServiceProvider = ServiceProvider, d: Option[Duration] = None)(
    implicit
    decoder: Decoder[T],
    encoder: Encoder[R],
    ec: ExecutionContext
) extends FutureHandler[T, R] {

  override protected def handlerFuture(input: T, error: Option[ExecutionError], requestInfo: RequestInfo, context: Context) = {
    servicesHandler(input, requestInfo, context, servicesProvider.forUser(input.user.isTestUser))
  }

  protected def servicesHandler(input: T, requestInfo: RequestInfo, context: Context, services: Services): Future[(R, RequestInfo)]

}
