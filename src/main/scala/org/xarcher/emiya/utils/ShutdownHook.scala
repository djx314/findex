package org.xarcher.emiya.utils

import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.Failure
import scala.concurrent.ExecutionContext.Implicits.global

class ShutdownHook() {

  protected var hooks: ListBuffer[() => Future[Unit]] = new ListBuffer[() => Future[Unit]]

  def addHook(f: () => Future[Unit]): Unit = {
    hooks += f
  }

  def exec(): Future[Unit] = {
    hooks.foldLeft(Future.successful(())) { (front, each) =>
      front.flatMap { (_: Unit) =>
        //println("closing")
        each()
      }.recover {
        case e: Exception =>
          e.printStackTrace
          ()
      }
    }
    Future.sequence(hooks.map(_.apply())).map((_: ListBuffer[Unit]) => ())
  }

}