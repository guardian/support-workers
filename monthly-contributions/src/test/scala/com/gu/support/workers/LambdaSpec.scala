package com.gu.support.workers

import java.io.ByteArrayOutputStream

import com.amazonaws.services.lambda.runtime.Context
import com.gu.support.workers.encoding.Conversions.FromOutputStream
import com.gu.support.workers.encoding.Encoding
import org.mockito.Mockito.when
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{Assertion, AsyncFlatSpec, FlatSpec, Matchers}

abstract class LambdaSpec extends FlatSpec with Matchers with MockContext {
  def assertUnit(output: ByteArrayOutputStream): Assertion =
    Encoding.in[Unit](output.toInputStream).isSuccess should be(true)
}
abstract class AsyncLambdaSpec extends AsyncFlatSpec with Matchers {
  implicit override def executionContext = com.gu.threadpools.CustomPool.executionContext
}

trait MockContext extends MockitoSugar {
  val context = mock[Context]
  when(context.getRemainingTimeInMillis).thenReturn(60000)
}
