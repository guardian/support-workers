package com.gu.support.workers

import java.io.{ByteArrayInputStream, ByteArrayOutputStream, InputStream}

import com.gu.support.workers.Fixtures.{createPayPalPaymentMethodDigitalBundleJson, wrapFixture}
import com.gu.support.workers.lambdas._
import com.gu.test.tags.annotations.IntegrationTest

import scala.io.Source

@IntegrationTest
class EndToEndSpec extends LambdaSpec {
  "The monthly contribution lambdas" should "chain successfully" in {
    logger.info(createPayPalPaymentMethodDigitalBundleJson)
    val output = wrapFixture(createPayPalPaymentMethodDigitalBundleJson)
      .chain(new CreatePaymentMethod())
      .chain(new CreateSalesforceContact())
      .chain(new CreateZuoraSubscription())
      .parallel(new ContributionCompleted, new SendThankYouEmail(), new UpdateMembersDataAPI())
      .last()

    assertUnit(output)
  }

  implicit class InputStreamChaining(val stream: InputStream) {

    def parallel(handlers: Handler[_, _]*): InputStream = {
      val listStartMarker = Array[Byte]('[')
      val listEndMarker = Array[Byte](',')
      val listSeparator = Array[Byte](']')

      val output = new ByteArrayOutputStream()

      output.write(listStartMarker)

      handlers.zipWithIndex.foreach {
        case (handler, index) =>
          if (index != 0) output.write(listSeparator)
          handler.handleRequest(stream, output, context)
          stream.reset()
      }

      output.write(listEndMarker)

      new ByteArrayInputStream(output.toByteArray)
    }

    def chain(handler: Handler[_, _]): InputStream = {
      new ByteArrayInputStream(last(handler).toByteArray)
    }

    def last(handler: Handler[_, _]): ByteArrayOutputStream = {
      val output = new ByteArrayOutputStream()
      handler.handleRequest(stream, output, context)
      output
    }

    def last(): ByteArrayOutputStream = {
      val output = new ByteArrayOutputStream()
      output.write(Source.fromInputStream(stream).mkString.getBytes)
      output
    }
  }

}
