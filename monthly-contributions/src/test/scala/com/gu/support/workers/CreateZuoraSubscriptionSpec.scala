package com.gu.support.workers

import java.io.ByteArrayOutputStream

import com.gu.config.Configuration
import com.gu.okhttp.RequestRunners
import com.gu.support.workers.Conversions.{FromOutputStream, StringInputStreamConversions}
import com.gu.support.workers.Fixtures._
import com.gu.support.workers.lambdas.CreateZuoraSubscription
import com.gu.support.workers.model.SendThankYouEmailState
import com.gu.zuora.ZuoraService

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import io.circe.generic.auto._

class CreateZuoraSubscriptionSpec extends LambdaSpec {

  "CreateZuoraSubscription lambda" should "create a Zuora subscription" in {
    val createZuora = new CreateZuoraSubscription(new ZuoraService(Configuration.zuoraConfig, RequestRunners.configurableFutureRunner(20.seconds)))

    val outStream = new ByteArrayOutputStream()

    createZuora.handleRequest(createZuoraSubscriptionJson.asInputStream(), outStream, context)

    val sendThankYouEmail = outStream.toClass[SendThankYouEmailState]
    sendThankYouEmail.accountNumber.length should be > 0
  }
}
