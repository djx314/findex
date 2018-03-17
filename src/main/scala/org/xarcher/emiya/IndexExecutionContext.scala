package org.xarcher.xPhoto

import java.util.concurrent.Executor

import org.xarcher.emiya.utils._

import scala.concurrent.ExecutionContext

class IndexExecutionContext(shutdownHook: ShutdownHook) {

  val indexEc = ExecutionContext.fromExecutor(null: Executor)

}