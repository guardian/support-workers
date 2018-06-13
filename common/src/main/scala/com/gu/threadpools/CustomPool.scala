package com.gu.threadpools

import java.util.concurrent.{LinkedBlockingQueue, ThreadPoolExecutor, TimeUnit}

import com.typesafe.scalalogging.LazyLogging

import scala.concurrent.ExecutionContext

object CustomPool extends LazyLogging {

  private val tec = new ThreadPoolExecutor(4, 4, 0L, TimeUnit.MILLISECONDS, new LinkedBlockingQueue[Runnable])
  private val ec = new LoggingExecutionContext(4)
  implicit val executionContext = ExecutionContext.fromExecutor(ec)

  def hasIncompleteTasks: Boolean = {
    ec.getCompletedTaskCount < ec.getTaskCount
  }

  def awaitCompletion: Unit = {
    ec.awaitTermination(30, TimeUnit.SECONDS)
    //    while (hasIncompleteTasks) {
    //      logger.info(s"Total taskCount: ${ec.getTaskCount}, completed: ${ec.getCompletedTaskCount}")
    //      Thread.sleep(500)
    //    }

  }
}

class LoggingExecutionContext(nThreads: Int) extends ThreadPoolExecutor(nThreads, nThreads, 0L, TimeUnit.MILLISECONDS,
  new LinkedBlockingQueue[Runnable]) with LazyLogging {
  override def execute(command: Runnable) = {
    logger.info("Adding a new task to the thread pool")
    super.execute(command)
  }

  override def beforeExecute(t: Thread, r: Runnable) = {
    //logger.info("Task starting")
    super.beforeExecute(t, r)
  }

  override def afterExecute(r: Runnable, t: Throwable) = {
    logger.info("Task completed")
    super.afterExecute(r, t)
  }

}
