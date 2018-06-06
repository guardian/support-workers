package com.gu.threadpools

import java.util.concurrent.{LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

object CustomPool extends LazyLogging {

  private val ec = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue[Runnable])
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(ec)

  def hasIncompleteTasks: Boolean = {
    ec.getCompletedTaskCount < ec.getTaskCount
  }

  def awaitCompletion: Unit = {
    while (hasIncompleteTasks) {
      logger.info(s"Total taskCount: ${ec.getTaskCount}, completed: ${ec.getCompletedTaskCount}")
      Thread.sleep(500)
    }

  }

}
