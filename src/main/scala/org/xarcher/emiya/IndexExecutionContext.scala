package org.xarcher.xPhoto

import java.util.concurrent.{ Executors, TimeUnit }

import org.xarcher.emiya.utils._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

class IndexExecutionContext(shutdownHook: ShutdownHook) {

  val indexEc = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(78))
  shutdownHook.addHook(new Thread() {
    override def run(): Unit = {
      indexEc.shutdown() //.awaitTermination(20, TimeUnit.SECONDS)
    }
  })

}