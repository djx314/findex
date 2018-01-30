package org.xarcher.xPhoto

import java.util.concurrent.Executors
import org.xarcher.emiya.utils._

import scala.concurrent.{ ExecutionContext, Future }
import scala.util.Try

class IndexExecutionContext(shutdownHook: ShutdownHook) {

  val indexEc = ExecutionContext.fromExecutorService(Executors.newFixedThreadPool(36))
  shutdownHook.addHook(() => Future.successful(Try {
    indexEc.shutdown()
  }))

}