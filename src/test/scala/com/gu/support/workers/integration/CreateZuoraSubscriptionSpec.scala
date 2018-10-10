package com.gu.support.workers.integration

import java.io.ByteArrayOutputStream

import com.gu.config.Configuration.zuoraConfigProvider
import com.gu.okhttp.RequestRunners.configurableFutureRunner
import com.gu.support.workers.Fixtures._
import com.gu.support.workers.LambdaSpec
import com.gu.support.workers.encoding.Conversions.FromOutputStream
import com.gu.support.workers.encoding.Encoding
import com.gu.support.workers.encoding.StateCodecs._
import com.gu.support.workers.errors.MockServicesCreator
import com.gu.support.workers.lambdas.CreateZuoraSubscription
import com.gu.support.workers.model.states.SendThankYouEmailState
import com.gu.support.workers.model.{Annual, BillingPeriod, Monthly}
import com.gu.test.tags.annotations.IntegrationTest
import com.gu.zuora.ZuoraService
import com.gu.zuora.model.SubscribeRequest
import org.mockito.Matchers.any
import org.mockito.Mockito.when
import org.mockito.invocation.InvocationOnMock

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

@IntegrationTest
class CreateZuoraSubscriptionSpec extends LambdaSpec with MockServicesCreator {

  "CreateZuoraSubscription lambda" should "create a monthly Zuora subscription" in {
    val createZuora = new CreateZuoraSubscription(mockServiceProvider)

    val outStream = new ByteArrayOutputStream()

    createZuora.handleRequest(wrapFixture(createContributionZuoraSubscriptionJson(billingPeriod = Monthly)), outStream, context)

    val sendThankYouEmail = Encoding.in[SendThankYouEmailState](outStream.toInputStream).get
    sendThankYouEmail._1.accountNumber.length should be > 0
  }

  "CreateZuoraSubscription lambda" should "create an annual Zuora subscription" in {
    val createZuora = new CreateZuoraSubscription(mockServiceProvider)

    val outStream = new ByteArrayOutputStream()

    createZuora.handleRequest(wrapFixture(createContributionZuoraSubscriptionJson(billingPeriod = Annual)), outStream, context)

    val sendThankYouEmail = Encoding.in[SendThankYouEmailState](outStream.toInputStream).get
    sendThankYouEmail._1.accountNumber.length should be > 0
  }

  "CreateZuoraSubscription lambda" should "create a Digital Pack subscription" in {
    val createZuora = new CreateZuoraSubscription(mockServiceProvider)

    val outStream = new ByteArrayOutputStream()

    createZuora.handleRequest(wrapFixture(createDigiPackZuoraSubscriptionJson), outStream, context)

    val sendThankYouEmail = Encoding.in[SendThankYouEmailState](outStream.toInputStream).get
    sendThankYouEmail._1.accountNumber.length should be > 0
  }

  val realService = new ZuoraService(zuoraConfigProvider.get(false), configurableFutureRunner(60.seconds))

  val mockZuoraService = {
    val mockZuora = mock[ZuoraService]
    // Need to return None from the Zuora service `getRecurringSubscription`
    // method or the subscribe step gets skipped
    // if these methods weren't coupled into one class then we could pass them separately and avoid reflection
    when(mockZuora.getAccountFields(any[String]))
      .thenReturn(Future.successful(Nil))
    when(mockZuora.getSubscriptions(any[String]))
      .thenReturn(Future.successful(Nil))
    when(mockZuora.subscribe(any[SubscribeRequest]))
      .thenAnswer((invocation: InvocationOnMock) => realService.subscribe(invocation.getArguments.head.asInstanceOf[SubscribeRequest]))
    mockZuora
  }

  val mockServiceProvider = mockServices(
    services => services.zuoraService,
    mockZuoraService
  )
}
