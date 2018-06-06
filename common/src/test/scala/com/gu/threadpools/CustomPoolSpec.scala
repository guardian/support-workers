package com.gu.threadpools

import com.gu.threadpools.CustomPool.executionContext
import com.typesafe.scalalogging.LazyLogging
import org.scalatest.{FlatSpec, Matchers}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

class CustomPoolSpec extends FlatSpec with Matchers with LazyLogging {

  "CustomPool" should "know if it has incomplete tasks" in {
    val future1 = newFuture(1).map(_ => newFuture(2))

    Await.result(future1, Duration.Inf)
    CustomPool.hasIncompleteTasks shouldBe (true)
    CustomPool.awaitCompletion
    CustomPool.hasIncompleteTasks shouldBe (false)
  }

  def newFuture(n: Int): Future[Unit] = Future {
    logger.info(s"Starting future $n")
    Thread.sleep(n * 1000)
    logger.info(s"completed future $n")
  }

}
