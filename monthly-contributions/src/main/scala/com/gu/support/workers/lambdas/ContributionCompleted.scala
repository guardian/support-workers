package com.gu.support.workers.lambdas

import com.amazonaws.services.lambda.runtime.Context
import com.gu.support.workers.encoding.StateCodecs._
import com.gu.support.workers.model.states.{CompletedState, SendThankYouEmailState}
import com.gu.support.workers.model.{ExecutionError, Status}
import com.typesafe.scalalogging.LazyLogging

class ContributionCompleted
    extends Handler[SendThankYouEmailState, CompletedState]
    with LazyLogging {

  override protected def handler(state: SendThankYouEmailState, error: Option[ExecutionError], context: Context): CompletedState = {
    val fields = List(
      "product_description" -> state.product.describe,
      "test_user" -> state.user.isTestUser.toString,
      "payment_method" -> state.paymentMethod.`type`
    )

    logger.info(fields.map({ case (k, v) => s"$k: $v" }).mkString("SUCCESS ", " ", ""))

    CompletedState(
      requestId = state.requestId,
      user = state.user,
      product = state.product,
      status = Status.Success,
      message = None
    )
  }
}
