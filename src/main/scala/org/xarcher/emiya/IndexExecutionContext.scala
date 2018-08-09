package org.xarcher.xPhoto

import java.util.concurrent.ForkJoinPool

import org.xarcher.emiya.utils._

import scala.concurrent.ExecutionContext

class IndexExecutionContext(shutdownHook: ShutdownHook) {

  val fineIndexExec: ExecutionContext = ExecutionContext.fromExecutor(new ForkJoinPool(2333))

}